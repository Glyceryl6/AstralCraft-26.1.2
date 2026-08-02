package com.astral_craft.common.gameplay.dice;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.items.AstralDiceItem;
import com.astral_craft.common.registry.AstralAttachments;
import com.astral_craft.common.registry.AstralDataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class DiceSkinPreferenceManager {

    public static final Identifier DEFAULT_TEXTURE = AstralCraft.prefix("textures/entity/dice/skins/default.png");
    private static final String RESOURCE_PREFIX = "textures/entity/dice/skins/";

    public static Identifier selectedTexture(Player player) {
        Identifier selected = player.getData(AstralAttachments.DICE_SKIN);
        return isSelectable(selected) ? selected : DEFAULT_TEXTURE;
    }

    public static void select(ServerPlayer player, Identifier texture) {
        if (!isSelectable(texture)) return;
        player.setData(AstralAttachments.DICE_SKIN, texture);
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.getItem() instanceof AstralDiceItem) stack.set(AstralDataComponents.DICE_TEXTURE, texture);
        }
        player.getInventory().setChanged();
    }

    public static void updateStack(ServerPlayer player, ItemStack stack) {
        if (stack.getItem() instanceof AstralDiceItem) stack.set(AstralDataComponents.DICE_TEXTURE, selectedTexture(player));
    }

    public static boolean isSelectable(Identifier id) {
        return id != null && id.getPath().startsWith(RESOURCE_PREFIX);
    }

}