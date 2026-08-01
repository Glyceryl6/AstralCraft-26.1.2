package com.astral_craft.common.items;

import com.astral_craft.common.gameplay.dice.AstralDiceRollService;
import com.astral_craft.common.gameplay.dice.DiceSkinPreferenceManager;
import com.astral_craft.common.registry.AstralDataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class AstralDiceItem extends Item {

    public AstralDiceItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (!(owner instanceof ServerPlayer player)) return;
        var selected = DiceSkinPreferenceManager.selectedTexture(player);
        if (!selected.equals(stack.getOrDefault(AstralDataComponents.DICE_TEXTURE, DiceSkinPreferenceManager.DEFAULT_TEXTURE))) {
            stack.set(AstralDataComponents.DICE_TEXTURE, selected);
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            DiceSkinPreferenceManager.updateStack(serverPlayer, player.getItemInHand(hand));
            Vec3 look = serverPlayer.getLookAngle().normalize();
            Vec3 origin = serverPlayer.position().add(look.scale(1.8D)).add(0.0D, 1.2D, 0.0D);
            AstralDiceRollService.rollNextMove(serverPlayer, origin);
        }

        return InteractionResult.SUCCESS;
    }

}
