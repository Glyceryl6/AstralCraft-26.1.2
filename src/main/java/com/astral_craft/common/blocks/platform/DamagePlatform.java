package com.astral_craft.common.blocks.platform;

import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.board.BoardEntityService;
import com.astral_craft.common.gameplay.board.BoardPanelContext;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.block.Block;

public class DamagePlatform extends BasePlatform {

    public DamagePlatform(Block.Properties properties) {
        super(properties, Trigger.LANDING);
    }

    @Override
    public void applyBoardEffect(BoardPanelContext context) {
        AstralCharacterEntity character = BoardEntityService.entity(context.level(), context.participant());
        if (character != null) {
            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(context.level(), EntitySpawnReason.TRIGGERED);
            if (lightning != null) {
                lightning.snapTo(character.getX(), character.getY(), character.getZ());
                lightning.setVisualOnly(true);
                context.level().addFreshEntity(lightning);
            }
        }

        BoardSessionManager.damageFromEffect(context.level(), context.session(), context.participant().slotUuid(), 2);
    }

}