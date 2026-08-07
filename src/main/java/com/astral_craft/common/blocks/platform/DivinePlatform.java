package com.astral_craft.common.blocks.platform;

import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.gameplay.board.BoardFortuneService;
import com.astral_craft.common.gameplay.board.BoardPanelContext;
import com.astral_craft.common.gameplay.board.BoardSession;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

import java.util.UUID;

public class DivinePlatform extends BasePlatform {

    public DivinePlatform(Block.Properties properties) {
        super(properties, Trigger.LANDING);
    }

    @Override
    public void applyBoardEffect(BoardPanelContext context) {
        if (BoardFortuneService.triggerDivination(context.level(), context.session(), context.participant())) {
            this.activateBoardEffect(context.session());
        } else {
            BoardSessionManager.resumeMovementAfterPanel(context.level(), context.session());
        }
    }

    @Override
    protected void tickPendingBoardEffect(ServerLevel level, BoardSession session) {
        if (BoardFortuneService.tick(level, session)) return;
        this.deactivateBoardEffect(session.id());
        BoardSessionManager.resumeMovementAfterPanel(level, session);
    }

    @Override
    protected void pendingParticipantBecameAutomated(ServerLevel level, BoardSession session, UUID slotId) {
        BoardFortuneService.participantBecameAutomated(level, session, slotId);
    }

    @Override
    protected void discardPendingBoardEffect(UUID boardId) {
        BoardFortuneService.clear(boardId);
    }

}