package com.astral_craft.common.gameplay.chip.type;

import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.chip.ChipDefinition;
import com.astral_craft.common.gameplay.chip.ChipPool;
import com.astral_craft.common.gameplay.chip.ChipRarity;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

public class PiggyBankChip extends ChipDefinition {

    public PiggyBankChip(Identifier id, ChipRarity rarity) {
        super(id, rarity, ChipPool.CARDS);
    }

    @Override
    public BoardParticipant afterEffectCardPlayed(ServerLevel level, BoardParticipant participant) {
        if (participant == null) return null;
        int played = participant.chipProgress().effectCardsPlayed() + 1;
        boolean reward = played >= 2;
        BoardParticipant.ChipProgress progress = participant.chipProgress().withEffectCardsPlayed(reward ? 0 : played);
        BoardParticipant updated = participant.withChipProgress(progress);
        return reward ? updated.withStats(updated.stats().addCoins(3)) : updated;
    }
}
