package com.astral_craft.common.blocks.platform;

import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.gameplay.board.BoardPanelContext;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import net.minecraft.world.level.block.Block;

public class GoldPlatform extends BasePlatform {

    private static final int[] REWARDS = {2, 3, 4, 5, 10};

    public GoldPlatform(Block.Properties properties) {
        super(properties, Trigger.LANDING);
    }

    @Override
    public void applyBoardEffect(BoardPanelContext context) {
        int reward = REWARDS[context.level().getRandom().nextInt(REWARDS.length)];
        BoardSessionManager.updateParticipant(context.level(), context.session(),
                context.participant().withStats(context.participant().stats().addCoins(reward)));
    }
}
