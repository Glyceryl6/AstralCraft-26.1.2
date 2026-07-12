package com.astral_craft.common.items;

import com.astral_craft.common.gameplay.dice.AstralDiceRollService;
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
        if (player instanceof ServerPlayer serverPlayer) {
            Vec3 look = serverPlayer.getLookAngle().normalize();
            Vec3 origin = serverPlayer.position().add(look.scale(1.8D)).add(0.0D, 1.2D, 0.0D);
            AstralDiceRollService.rollNextMove(serverPlayer, origin);
        }

        return InteractionResult.SUCCESS;
    }

}