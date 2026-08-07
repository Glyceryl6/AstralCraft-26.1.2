package com.astral_craft.common.gameplay.chip.type;

import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.gameplay.chip.ChipDefinition;
import com.astral_craft.common.gameplay.chip.ChipPool;
import com.astral_craft.common.gameplay.chip.ChipRarity;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

public class SmartwatchChip extends ChipDefinition {

    public SmartwatchChip(Identifier id, ChipRarity rarity) {
        super(id, rarity, ChipPool.CARDS);
    }

    @Override
    public BoardParticipant afterTurnEnd(ServerLevel level, BoardParticipant participant) {
        if (participant == null || participant.hand().size() >= 5) return participant;
        return BoardSessionManager.randomPvpCardId(level).map(participant::addCard).orElse(participant);
    }
}
