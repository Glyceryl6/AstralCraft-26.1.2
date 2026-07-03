package com.astral_craft.common.items;

import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.handcard.CardRangeResolver;
import com.astral_craft.common.gameplay.handcard.CardRevealOptions;
import com.astral_craft.common.gameplay.handcard.CardUseService;
import com.astral_craft.common.registry.AstralDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class BaseHandCard extends Item {

    public BaseHandCard(Properties properties) {
        super(properties);
    }

    public CardDefinition definition(ItemStack itemStack) {
        return itemStack.getOrDefault(AstralDataComponents.CARD_DEFINITION, CardDefinition.fallback());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return CardUseService.use(this, level, player, hand);
    }

    public boolean applyFromSelection(ServerPlayer user, InteractionHand hand, List<LivingEntity> targets) {
        return this.apply(user, hand, targets);
    }

    public CardRevealOptions revealOptions(ServerPlayer user, InteractionHand hand, ItemStack itemStack, CardDefinition definition, List<LivingEntity> targets) {
        if (!definition.shouldRevealOnUse()) {
            return CardRevealOptions.none();
        }

        return CardRevealOptions.selfFlip(CardUseService.CARD_REVEAL_DURATION_TICKS);
    }

    protected boolean apply(ServerPlayer user, InteractionHand hand, List<LivingEntity> targets) {
        return false;
    }

    public void appendHoverText(ItemStack itemStack, TooltipContext context, Consumer<Component> builder, TooltipFlag tooltipFlag, @Nullable Player player) {
        CardDefinition definition = this.definition(itemStack);
        CardType cardType = itemStack.getOrDefault(AstralDataComponents.CARD_TYPE, definition.type());
        String key = String.format("tooltips.astral_craft.handcard.card_type.%s", cardType.getSerializedName());
        builder.accept(Component.translatable(key).withColor(cardType.color).withStyle(ChatFormatting.BOLD));
        int effectiveRange = CardRangeResolver.effectiveRange(player, itemStack, definition);
        Component component = Component.translatable(definition.effectKey(), effectiveRange);
        for (String line : component.getString().split("[\\n|]")) {
            builder.accept(Component.literal(line.trim()));
        }

        if (!definition.restrictions().unrestricted()) {
            builder.accept(Component.translatable("tooltips.astral_craft.handcard.restricted").withStyle(ChatFormatting.GRAY));
        }
    }

}