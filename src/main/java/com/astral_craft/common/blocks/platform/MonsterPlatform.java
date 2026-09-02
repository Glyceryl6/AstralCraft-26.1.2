package com.astral_craft.common.blocks.platform;

import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.gameplay.board.BoardMonsterService;
import com.astral_craft.common.gameplay.board.BoardPanelContext;
import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import net.minecraft.world.level.block.Block;

public class MonsterPlatform extends BasePlatform {

    public MonsterPlatform(Block.Properties properties) {
        super(properties, Trigger.LANDING);
    }

    @Override
    public void applyBoardEffect(BoardPanelContext context) {
        BoardParticipant monster = BoardMonsterService.spawnDefault(
                context.level(), context.session(), context.participant().currentNodeKey());
        if (monster != null) BoardSessionManager.beginPanelEncounter(
                context.level(), context.session(), context.participant(), monster);
    }
}
