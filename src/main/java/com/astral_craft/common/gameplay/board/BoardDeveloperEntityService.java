package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.character.CharacterManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/** Entity mutations that exist only for the live developer editor. */
public class BoardDeveloperEntityService {

    public static void syncIdentity(ServerLevel level, BoardParticipant participant) {
        if (level == null || participant == null || participant.monster()) return;
        AstralCharacterEntity entity = BoardEntityService.entity(level, participant);
        if (entity == null) return;
        entity.setCharacterId(participant.characterId());
        entity.setSkinId(participant.skinName());
        entity.setCustomName(Component.translatable(CharacterManager.INSTANCE.get(participant.characterId()).getDescriptionId()));
        entity.setAnimationAction("idle");
    }
}
