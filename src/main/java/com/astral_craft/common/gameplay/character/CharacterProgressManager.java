package com.astral_craft.common.gameplay.character;

import com.astral_craft.common.gameplay.character.skill.AstralCharacterSkillService;
import com.astral_craft.common.gameplay.character.skin.CharacterSkinDefinition;
import com.astral_craft.common.network.OpenCharacterSettingsPayload;
import com.astral_craft.common.registry.AstralAttachments;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Set;
import java.util.stream.Collectors;

public class CharacterProgressManager {

    public static final Identifier NORMAL_PLAYER_ID = Identifier.fromNamespaceAndPath("minecraft", "player");
    public static final Identifier NORMAL_CHARACTER_ID = Identifier.fromNamespaceAndPath("minecraft", "normal");
    public static final Identifier NONE_CHARACTER_ID = Identifier.fromNamespaceAndPath("minecraft", "none");

    public static CharacterProgress progress(ServerPlayer player) {
        CharacterProgress progress = player.getData(AstralAttachments.CHARACTER_PROGRESS);
        progress.syncUnlockedDefaults(CharacterManager.INSTANCE.values());
        player.setData(AstralAttachments.CHARACTER_PROGRESS, progress);
        return progress;
    }

    public static ActiveCharacterState activeState(ServerPlayer player) {
        ActiveCharacterState state = player.getData(AstralAttachments.ACTIVE_CHARACTER);
        if (!state.active()) {
            return state;
        }

        if (!CharacterManager.INSTANCE.contains(state.characterId())) {
            player.setData(AstralAttachments.ACTIVE_CHARACTER, ActiveCharacterState.NONE);
            return ActiveCharacterState.NONE;
        }

        CharacterProgress progress = progress(player);
        CharacterDefinition definition = CharacterManager.INSTANCE.get(state.characterId());
        CharacterProgressEntry entry = progress.entry(definition.id());
        ActiveCharacterState refreshed = ActiveCharacterState.of(definition, entry);
        if (!refreshed.equals(state)) {
            player.setData(AstralAttachments.ACTIVE_CHARACTER, refreshed);
        }
        return refreshed;
    }

    public static boolean hasActiveCharacter(ServerPlayer player) {
        return player.getData(AstralAttachments.ACTIVE_CHARACTER).active();
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
                    .filter(value -> isDefaultSkin(finalSelected, value) || value.unlockedByDefault() || progress.isSkinUnlocked(finalSelected.id(), value.id()))
                    .findFirst().orElse(selected.skins().getFirst());
            progress.unlockSkin(selected.id(), skin.id());
            progress.setSelectedSkin(skin.id());
        }

        save(player, progress);
        ActiveCharacterState activeState = activeState(player);
        PacketDistributor.sendToPlayer(player, new OpenCharacterSettingsPayload(
                CharacterManager.INSTANCE.encodeList(),
                progress.selectedCharacter().toString(),
                progress.selectedSkin(),
                activeState.active() ? activeState.characterId().toString() : "",
                activeState.active() ? activeState.skinId() : "",
                progress.level(),
                progress.experience(),
                progress.friendship(),
                encodeIdentifiers(progress.unlockedCharacters()),
                encodeSkinKeys(progress),
                encodeProgressEntries(progress)));
    }

    public static void selectCharacter(ServerPlayer player, String rawId) {
        if (isInactiveCharacterId(rawId)) {
            deactivateCharacter(player);
            return;
        }

        Identifier id = safeParse(rawId, CharacterManager.INSTANCE.defaultCharacter().id());
        if (!CharacterManager.INSTANCE.contains(id)) return;
        CharacterDefinition definition = CharacterManager.INSTANCE.get(id);
        CharacterProgress progress = progress(player);
        if (!definition.unlockedByDefault() && !progress.isCharacterUnlocked(id)) return;
        progress.setSelectedCharacter(id);
        if (!definition.skins().isEmpty()) {
            CharacterSkinDefinition skin = definition.skins().stream()
                    .filter(value -> isDefaultSkin(definition, value) || value.unlockedByDefault() || progress.isSkinUnlocked(definition.id(), value.id()))
                    .findFirst().orElse(definition.skins().getFirst());
            progress.unlockSkin(definition.id(), skin.id());
            progress.setSelectedSkin(skin.id());
        }

        save(player, progress);
        refreshActiveCharacter(player, definition.id());
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.astral_craft.character_settings.character_active", net.minecraft.network.chat.Component.translatable(definition.nameKey())), true);
    }

    public static void deactivateCharacter(ServerPlayer player) {
        if (player == null) return;
        ActiveCharacterState previous = player.getData(AstralAttachments.ACTIVE_CHARACTER);
        player.setData(AstralAttachments.ACTIVE_CHARACTER, previous.inactive());
        AstralCharacterSkillService.clearStatusEffects(player);
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.astral_craft.character_settings.character_inactive"), true);
    }

    public static void selectSkin(ServerPlayer player, String rawCharacterId, String skinId) {
        CharacterProgress progress = progress(player);
        Identifier characterId = safeParse(rawCharacterId, progress.selectedCharacter());
        if (!CharacterManager.INSTANCE.contains(characterId)) return;
        CharacterDefinition definition = CharacterManager.INSTANCE.get(characterId);
        if (!definition.unlockedByDefault() && !progress.isCharacterUnlocked(definition.id())) return;
        CharacterSkinDefinition skin = definition.skinOrDefault(skinId);
        if (isDefaultSkin(definition, skin) || skin.unlockedByDefault() || progress.isSkinUnlocked(definition.id(), skin.id())) {
            progress.setSelectedCharacter(definition.id());
            progress.unlockSkin(definition.id(), skin.id());
            progress.setSelectedSkin(skin.id());
            save(player, progress);
            refreshActiveCharacter(player, definition.id());
        }
    }



    public static void activatePotential(ServerPlayer player, String rawCharacterId) {
        if (player == null) return;
        Identifier characterId = safeParse(rawCharacterId, CharacterManager.INSTANCE.defaultCharacter().id());
        if (!CharacterManager.INSTANCE.contains(characterId)) return;
        CharacterDefinition definition = CharacterManager.INSTANCE.get(characterId);
        CharacterPotentialDefinition potential = definition.potentialOrDefault();
        if (!definition.supportsPotential()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.astral_craft.character_settings.potential_missing"), true);
            return;
        }

        CharacterProgress progress = progress(player);
        CharacterProgressEntry entry = progress.entry(characterId);
        if (!entry.unlocked() && !definition.unlockedByDefault()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.astral_craft.character_settings.potential_locked"), true);
            return;
        }
        if (entry.potentialActivated()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.astral_craft.character_settings.potential_already_active"), true);
            return;
        }
        if (!potential.canActivate(entry, definition, player)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.astral_craft.character_settings.potential_requirement_not_met"), true);
            return;
        }

        potential.consumeMaterials(player);
        progress.activatePotential(characterId);
        save(player, progress);
        refreshActiveIfSame(player, characterId);
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.astral_craft.character_settings.potential_activated", net.minecraft.network.chat.Component.translatable(definition.nameKey())), true);
        open(player);
    }

    public static void unlockAllForTesting(ServerPlayer player) {
        if (player == null) return;
        if (!player.getAbilities().instabuild) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.astral_craft.character_settings.unlock_all_forbidden"), true);
            return;
        }

        CharacterProgress progress = progress(player);
        for (CharacterDefinition definition : CharacterManager.INSTANCE.values()) {
            CharacterProgressEntry entry = progress.entry(definition.id()).unlock();
            for (CharacterSkinDefinition skin : definition.skins()) {
                entry = entry.unlockSkin(skin.id());
            }
            progress.setEntry(definition.id(), entry);
        }

        save(player, progress);
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.astral_craft.character_settings.unlock_all_done"), true);
        open(player);
    }

    public static void unlockCharacter(ServerPlayer player, Identifier characterId) {
        if (!CharacterManager.INSTANCE.contains(characterId)) return;
        CharacterProgress progress = progress(player);
        progress.unlockCharacter(characterId);
        CharacterDefinition definition = CharacterManager.INSTANCE.get(characterId);
        for (CharacterSkinDefinition skin : definition.skins()) {
            if (isDefaultSkin(definition, skin) || skin.unlockedByDefault()) {
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

    private static boolean isDefaultSkin(CharacterDefinition definition, CharacterSkinDefinition skin) {
        if (definition == null || skin == null) return false;
        if ("default".equals(skin.id())) return true;
        return !definition.skins().isEmpty() && definition.skins().getFirst().id().equals(skin.id());
    }

    public static void addExperience(ServerPlayer player, Identifier characterId, int amount) {
        if (!CharacterManager.INSTANCE.contains(characterId)) return;
        CharacterProgress progress = progress(player);
        progress.addExperience(characterId, amount);
        save(player, progress);
        refreshActiveIfSame(player, characterId);
    }

    public static void addFriendship(ServerPlayer player, Identifier characterId, int amount) {
        if (!CharacterManager.INSTANCE.contains(characterId)) return;
        CharacterProgress progress = progress(player);
        progress.addFriendship(characterId, amount);
        save(player, progress);
        refreshActiveIfSame(player, characterId);
    }

    public static void setLevel(ServerPlayer player, Identifier characterId, int level) {
        if (!CharacterManager.INSTANCE.contains(characterId)) return;
        CharacterProgress progress = progress(player);
        progress.setLevel(characterId, level);
        save(player, progress);
        refreshActiveIfSame(player, characterId);
    }

    public static void setFriendshipLevel(ServerPlayer player, Identifier characterId, int level) {
        if (!CharacterManager.INSTANCE.contains(characterId)) return;
        CharacterProgress progress = progress(player);
        progress.setFriendshipLevel(characterId, level);
        save(player, progress);
        refreshActiveIfSame(player, characterId);
    }

    public static void refreshActiveIfSame(ServerPlayer player, Identifier characterId) {
        ActiveCharacterState state = player.getData(AstralAttachments.ACTIVE_CHARACTER);
        if (state.active() && state.characterId().equals(characterId)) {
            refreshActiveCharacter(player, characterId);
        }
    }

    public static void refreshActiveCharacter(ServerPlayer player, Identifier characterId) {
        if (!CharacterManager.INSTANCE.contains(characterId)) return;
        CharacterProgress progress = progress(player);
        CharacterDefinition definition = CharacterManager.INSTANCE.get(characterId);
        CharacterProgressEntry entry = progress.entry(characterId);
        player.setData(AstralAttachments.ACTIVE_CHARACTER, ActiveCharacterState.of(definition, entry));
    }


    public static boolean isInactiveCharacterId(String rawId) {
        if (rawId == null || rawId.isBlank()) return true;
        Identifier id = safeParse(rawId, NONE_CHARACTER_ID);
        return NONE_CHARACTER_ID.equals(id) || NORMAL_CHARACTER_ID.equals(id) || NORMAL_PLAYER_ID.equals(id);
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
                + encodeStrings(entry.unlockedSkins()) + "|"
                + entry.potentialActivated();
    }

    public static String skinKey(Identifier characterId, String skinId) {
        return characterId + "#" + skinId;
    }

    protected static void save(ServerPlayer player, CharacterProgress progress) {
        player.setData(AstralAttachments.CHARACTER_PROGRESS, progress);
    }

}