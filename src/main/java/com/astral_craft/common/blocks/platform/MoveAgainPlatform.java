package com.astral_craft.common.blocks.platform;

import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.gameplay.board.BoardPanelContext;
import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.board.BoardSession;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

public class MoveAgainPlatform extends BasePlatform {

    public MoveAgainPlatform(Block.Properties properties) {
        super(properties, Trigger.LANDING);
    }

    @Override
    public void applyBoardEffect(BoardPanelContext context) {
        ServerLevel level = context.level();
        BoardSession session = context.session();
        BoardParticipant participant = context.participant();
        if (level == null || session == null || participant == null || participant.knockedDown()) return;
        BoardSession.MovementState movement = session.movement();
        if (movement == null || !movement.slotId().equals(participant.slotUuid()) || movement.remainingSteps() > 0) return;
        BoardSessionManager.beginMoveRoll(level, session, participant, null);
    }

}