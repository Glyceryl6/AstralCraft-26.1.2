package com.astral_craft.common.gameplay.chip.type;

import com.astral_craft.common.gameplay.buff.BoardBuff;
import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.chip.ChipDefinition;
import com.astral_craft.common.gameplay.chip.ChipPool;
import com.astral_craft.common.gameplay.chip.ChipRarity;
import com.astral_craft.common.registry.AstralBoardBuffs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

public class TurnStartBuffChip extends ChipDefinition {

    private final int amount;

    public TurnStartBuffChip(Identifier id, ChipRarity rarity, ChipPool pool, Identifier keywordId, int amount) {
        super(id, rarity, pool, keywordId);
        this.amount = amount;
    }

    @Override
    public BoardParticipant beforeTurnStart(ServerLevel level, BoardParticipant participant) {
        BoardBuff buff = this.keywordId().map(AstralBoardBuffs.REGISTRY::getValue).orElse(null);
        return participant == null || buff == null ? participant
                : participant.withStats(participant.stats().addPermanentBuff(buff, this.amount));
    }
}
