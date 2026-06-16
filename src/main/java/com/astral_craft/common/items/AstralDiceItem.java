package com.astral_craft.common.items;

import com.astral_craft.common.entity.AstralDiceEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class AstralDiceItem extends Item {

    public AstralDiceItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            Vec3 look = serverPlayer.getLookAngle().normalize();
            Vec3 pos = serverPlayer.position().add(look.scale(1.8)).add(0, 1.2, 0);
            AstralDiceEntity dice = new AstralDiceEntity(serverLevel, pos.x, pos.y, pos.z);
            dice.startRoll(1, 10, 20, serverLevel.getRandom());
            serverLevel.addFreshEntity(dice);
        }

        return InteractionResult.SUCCESS;
    }

}