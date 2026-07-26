package com.astral_craft.common.gameplay.buff.type;

import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.board.BoardSession;
import com.astral_craft.common.gameplay.board.BoardWorldObjectService;
import com.astral_craft.common.gameplay.buff.BoardBuff;
import com.astral_craft.common.gameplay.buff.BoardBuffInstance;
import net.minecraft.server.level.ServerLevel;

public class LeakingPocketBoardBuff extends BoardBuff {

    public LeakingPocketBoardBuff(int color) {
        super(Properties.of(color).stacking().permanent());
    }

    @Override
    public BoardParticipant onMovementFinished(
            ServerLevel level, BoardSession session, BoardSession.MovementState movement,
            BoardParticipant participant, BoardBuffInstance instance) {
        int amount = Math.min(participant.stats().starCoins(), movement.route().size());
        BoardParticipant updated = participant.withStats(participant.stats().spendCoins(amount).removeBuff(this));
        if (amount > 0) BoardWorldObjectService.dropCoins(level, session, participant.currentNodeKey(), amount);
        return updated;
    }

}