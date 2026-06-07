package com.astral_craft.common.items;

import com.astral_craft.common.components.CardType;
import com.astral_craft.common.registry.AstralDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** @noinspection deprecation*/
public class BaseHandCard extends Item {

    public BaseHandCard(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        CardType cardType = itemStack.getOrDefault(AstralDataComponents.CARD_TYPE, CardType.ATTACK);
        String key = String.format("tooltips.astral_craft.handcard.card_type.%s", cardType.getSerializedName());
        builder.accept(Component.translatable(key).withColor(cardType.color).withStyle(ChatFormatting.BOLD));
        MutableComponent component = Component.translatable(this.descriptionId.replaceFirst("item", "tooltips"));
        for (String line : component.getString().split("[\\n|]")) {
            builder.accept(Component.literal(line.trim()));
        }
    }

}