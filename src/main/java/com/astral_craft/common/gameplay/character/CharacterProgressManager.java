package com.astral_craft.common.gameplay.character;

import com.astral_craft.common.network.OpenCharacterSettingsPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CharacterProgressManager {

    protected static final Map<UUID, CharacterProgress> PROGRESS = new HashMap<>();

    public static CharacterProgress progress(ServerPlayer player) {
        return PROGRESS.computeIfAbsent(player.getUUID(), _ -> new CharacterProgress(CharacterManager.INSTANCE.defaultCharacter().id()));
    }

    public static void open(ServerPlayer player) {
        CharacterProgress progress = progress(player);
        CharacterDefinition selected = CharacterManager.INSTANCE.get(progress.selectedCharacter());
        if (!selected.skins().isEmpty() && selected.skins().stream().noneMatch(skin -> skin.id().equals(progress.selectedSkin()))) {
            progress.setSelectedSkin(selected.skins().getFirst().id());
        }

        PacketDistributor.sendToPlayer(player, new OpenCharacterSettingsPayload(
                CharacterManager.INSTANCE.encodeList(),
                progress.selectedCharacter().toString(),
                progress.selectedSkin(),
                progress.level(),
                progress.experience(),
                progress.friendship()));
    }

    public static void selectCharacter(ServerPlayer player, String rawId) {
        Identifier id = safeParse(rawId, CharacterManager.INSTANCE.defaultCharacter().id());
        if (!CharacterManager.INSTANCE.contains(id)) return;
        CharacterProgress progress = progress(player);
        progress.setSelectedCharacter(id);
        CharacterDefinition definition = CharacterManager.INSTANCE.get(id);
        if (!definition.skins().isEmpty()) {
            progress.setSelectedSkin(definition.skins().getFirst().id());
        }
    }

    public static void selectSkin(ServerPlayer player, String rawCharacterId, String skinId) {
        Identifier characterId = safeParse(rawCharacterId, progress(player).selectedCharacter());
        CharacterDefinition definition = CharacterManager.INSTANCE.get(characterId);
        CharacterProgress progress = progress(player);
        if (!definition.id().equals(progress.selectedCharacter())) return;
        CharacterSkinDefinition skin = definition.skinOrDefault(skinId);
        if (skin.unlockedByDefault() || progress.isSkinUnlocked(skin.id())) {
            progress.setSelectedSkin(skin.id());
        }
    }

    public static Identifier safeParse(String rawId, Identifier fallback) {
        try {
            return Identifier.parse(rawId);
        } catch (Exception exception) {
            return fallback;
        }
    }

}