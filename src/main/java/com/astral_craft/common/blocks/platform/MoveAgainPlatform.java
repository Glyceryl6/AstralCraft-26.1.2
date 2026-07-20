package com.astral_craft.common.blocks.platform;

import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.gameplay.board.BoardPanelContext;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import net.minecraft.world.level.block.Block;

public class MoveAgainPlatform extends BasePlatform {

    public MoveAgainPlatform(Block.Properties properties) {
        super(properties, Trigger.LANDING);
    }

    @Override
    public void applyBoardEffect(BoardPanelContext context) {
        BoardSessionManager.beginAutomaticMoveAgain(context.level(), context.session(), context.participant());
    }

}