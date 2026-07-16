package com.astral_craft.common.blocks.platform;

import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.gameplay.board.BoardPanelContext;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import net.minecraft.world.level.block.Block;

public class HealPlatform extends BasePlatform {

    public HealPlatform(Block.Properties properties) {
        super(properties, Trigger.LANDING);
    }

    @Override
    public void applyBoardEffect(BoardPanelContext context) {
        BoardSessionManager.updateParticipant(context.level(), context.session(),
                context.participant().withStats(context.participant().stats().heal(2)));
    }
}
