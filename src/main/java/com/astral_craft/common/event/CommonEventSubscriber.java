package com.astral_craft.common.event;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.gameplay.KnockdownManager;
import com.astral_craft.common.gameplay.PendingCardActionManager;
import com.astral_craft.common.gameplay.PendingCounterEffectManager;
import com.astral_craft.common.gameplay.SoulLinkManager;
import com.astral_craft.common.gameplay.board.BoardHudSyncManager;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

@EventBusSubscriber(modid = AstralCraft.MOD_ID)
public class CommonEventSubscriber {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack itemStack = event.getItemStack();
        Item item = itemStack.getItem();
        if (item instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (block instanceof BasePlatform) {
                List<Component> tooltip = event.getToolTip();
                String descriptionId = item.getDescriptionId();
                descriptionId = descriptionId.replaceFirst("block", "tooltips");
                tooltip.add(Component.translatable(descriptionId).withStyle(ChatFormatting.YELLOW));
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        SoulLinkManager.onDamagePre(event);
    }

    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        PendingCardActionManager.serverTick();
        PendingCounterEffectManager.serverTick(event.getServer());
        KnockdownManager.serverTick();
        BoardSessionManager.serverTick();
        BoardHudSyncManager.serverTick();
    }

    @SubscribeEvent
    public static void onBlockBreak(BreakBlockEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel && BoardSessionManager.isProtected(serverLevel, event.getPos())) {
            event.setCanceled(true);
            event.getPlayer().sendOverlayMessage(Component.translatable("message.astral_craft.board.protected").withStyle(ChatFormatting.YELLOW));
        }
    }

}
