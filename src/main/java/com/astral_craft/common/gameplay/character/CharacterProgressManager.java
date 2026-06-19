package com.astral_craft.common.gameplay.character;

import com.astral_craft.common.network.OpenCharacterSettingsPayload;
import com.astral_craft.common.registry.AstralAttachments;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Set;
import java.util.stream.Collectors;

public class CharacterProgressManager {

    public static CharacterProgress progress(ServerPlayer player) {
        CharacterProgress progress = player.getData(AstralAttachments.CHARACTER_PROGRESS);
        progress.syncUnlockedDefaults(CharacterManager.INSTANCE.values());
        player.setData(AstralAttachments.CHARACTER_PROGRESS, progress);
        return progress;
    }

    public static void open(ServerPlayer player) {
        CharacterProgress progress = progress(player);
        CharacterDefinition selected = CharacterManager.INSTANCE.get(progress.selectedCharacter());
        if (!progress.isCharacterUnlocked(selected.id()) && !selected.unlockedByDefault()) {
            selected = CharacterManager.INSTANCE.values().stream()
                    .filter(definition -> progress.isCharacterUnlocked(definition.id()) || definition.unlockedByDefault())
                    .findFirst().orElse(CharacterManager.INSTANCE.defaultCharacter());
            progress.setSelectedCharacter(selected.id());
        }

        if (!selected.skins().isEmpty() && selected.skins().stream().noneMatch(skin -> skin.id().equals(progress.selectedSkin()))) {
            CharacterDefinition finalSelected = selected;
            CharacterSkinDefinition skin = selected.skins().stream()
                    .filter(value -> value.unlockedByDefault() || progress.isSkinUnlocked(finalSelected.id(), value.id()))
                    .findFirst().orElse(selected.skins().getFirst());
            progress.unlockSkin(selected.id(), skin.id());
            progress.setSelectedSkin(skin.id());
        }

        save(player, progress);
        PacketDistributor.sendToPlayer(player, new OpenCharacterSettingsPayload(
                CharacterManager.INSTANCE.encodeList(),
                progress.selectedCharacter().toString(),
                progress.selectedSkin(),
                progress.level(),
                progress.experience(),
                progress.friendship(),
                encodeIdentifiers(progress.unlockedCharacters()),
                encodeSkinKeys(progress),
                encodeProgressEntries(progress)));
    }

    public static void selectCharacter(ServerPlayer player, String rawId) {
        Identifier id = safeParse(rawId, CharacterManager.INSTANCE.defaultCharacter().id());
        if (!CharacterManager.INSTANCE.contains(id)) return;
        CharacterDefinition definition = CharacterManager.INSTANCE.get(id);
        CharacterProgress progress = progress(player);
        if (!definition.unlockedByDefault() && !progress.isCharacterUnlocked(id)) return;
        progress.setSelectedCharacter(id);
        if (!definition.skins().isEmpty()) {
            CharacterSkinDefinition skin = definition.skins().stream()
                    .filter(value -> value.unlockedByDefault() || progress.isSkinUnlocked(definition.id(), value.id()))
                    .findFirst().orElse(definition.skins().getFirst());
            progress.unlockSkin(definition.id(), skin.id());
            progress.setSelectedSkin(skin.id());
        }

        save(player, progress);
    }

    public static void selectSkin(ServerPlayer player, String rawCharacterId, String skinId) {
        CharacterProgress progress = progress(player);
        Identifier characterId = safeParse(rawCharacterId, progress.selectedCharacter());
        CharacterDefinition definition = CharacterManager.INSTANCE.get(characterId);
        if (!definition.id().equals(progress.selectedCharacter())) return;
        if (!definition.unlockedByDefault() && !progress.isCharacterUnlocked(definition.id())) return;
        CharacterSkinDefinition skin = definition.skinOrDefault(skinId);
        if (skin.unlockedByDefault() || progress.isSkinUnlocked(definition.id(), skin.id())) {
            progress.setSelectedSkin(skin.id());
            save(player, progress);
        }
    }

    public static void unlockCharacter(ServerPlayer player, Identifier characterId) {
        if (!CharacterManager.INSTANCE.contains(characterId)) return;
        CharacterProgress progress = progress(player);
        progress.unlockCharacter(characterId);
        CharacterDefinition definition = CharacterManager.INSTANCE.get(characterId);
        for (CharacterSkinDefinition skin : definition.skins()) {
            if (skin.unlockedByDefault()) {
                progress.unlockSkin(characterId, skin.id());
            }
        }

        save(player, progress);
    }

    public static void unlockSkin(ServerPlayer player, Identifier characterId, String skinId) {
        if (!CharacterManager.INSTANCE.contains(characterId)) return;
        CharacterProgress progress = progress(player);
        if (!progress.isCharacterUnlocked(characterId)) return;
        CharacterDefinition definition = CharacterManager.INSTANCE.get(characterId);
        CharacterSkinDefinition skin = definition.skinOrDefault(skinId);
        progress.unlockSkin(characterId, skin.id());
        save(player, progress);
    }

    public static void addExperience(ServerPlayer player, Identifier characterId, int amount) {
        if (!CharacterManager.INSTANCE.contains(characterId)) return;
        CharacterProgress progress = progress(player);
        progress.addExperience(characterId, amount);
        save(player, progress);
    }

    public static void addFriendship(ServerPlayer player, Identifier characterId, int amount) {
        if (!CharacterManager.INSTANCE.contains(characterId)) return;
        CharacterProgress progress = progress(player);
        progress.addFriendship(characterId, amount);
        save(player, progress);
    }

    public static void setLevel(ServerPlayer player, Identifier characterId, int level) {
        if (!CharacterManager.INSTANCE.contains(characterId)) return;
        CharacterProgress progress = progress(player);
        progress.setLevel(characterId, level);
        save(player, progress);
    }

    public static void setFriendshipLevel(ServerPlayer player, Identifier characterId, int level) {
        if (!CharacterManager.INSTANCE.contains(characterId)) return;
        CharacterProgress progress = progress(player);
        progress.setFriendshipLevel(characterId, level);
        save(player, progress);
    }

    public static Identifier safeParse(String rawId, Identifier fallback) {
        try {
            return Identifier.parse(rawId);
        } catch (Exception exception) {
            return fallback;
        }
    }

    public static String encodeIdentifiers(Set<Identifier> identifiers) {
        return identifiers.stream().map(Identifier::toString).sorted().collect(Collectors.joining(","));
    }

    public static String encodeStrings(Set<String> values) {
        return values.stream().sorted().collect(Collectors.joining(","));
    }

    public static String encodeSkinKeys(CharacterProgress progress) {
        return progress.entries().entrySet().stream()
                .flatMap(entry -> entry.getValue().unlockedSkins().stream().map(skin -> skinKey(entry.getKey(), skin)))
                .sorted().collect(Collectors.joining(","));
    }

    public static String encodeProgressEntries(CharacterProgress progress) {
        return progress.entries().entrySet().stream()
                .map(entry -> encodeProgressEntry(entry.getKey(), entry.getValue()))
                .sorted().collect(Collectors.joining(";"));
    }

    public static String encodeProgressEntry(Identifier characterId, CharacterProgressEntry entry) {
        return characterId + "|"
                + entry.unlocked() + "|"
                + entry.selectedSkin() + "|"
                + entry.level() + "|"
                + entry.experience() + "|"
                + entry.friendship() + "|"
                + encodeStrings(entry.unlockedSkins());
    }

    public static String skinKey(Identifier characterId, String skinId) {
        return characterId + "#" + skinId;
    }

    protected static void save(ServerPlayer player, CharacterProgress progress) {
        player.setData(AstralAttachments.CHARACTER_PROGRESS, progress);
    }

}