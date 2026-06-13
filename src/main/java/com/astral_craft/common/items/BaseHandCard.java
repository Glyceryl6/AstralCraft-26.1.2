package com.astral_craft.common.items;

import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.CardDefinition;
import com.astral_craft.common.gameplay.CardUseService;
import com.astral_craft.common.registry.AstralDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Consumer;

/* Base plumbing for hand cards. Concrete card classes own their metadata and effect code. */
/** @noinspection deprecation*/
public class BaseHandCard extends Item {

    private final CardDefinition definition;

    public BaseHandCard(Properties properties, CardDefinition definition) {
        super(properties);
        this.definition = definition;
    }

    public CardDefinition definition() {
        return this.definition;
    }

    @Override
    public Component getName(ItemStack itemStack) {
        MutableComponent prefix = Component.translatable("prefix.astral_craft.handcard");
        return Component.literal(prefix.getString() + super.getName(itemStack).getString());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return CardUseService.use(this, level, player, hand);
    }

    public final boolean applyFromSelection(ServerPlayer user, InteractionHand hand, List<LivingEntity> targets) {
        return this.apply(user, hand, targets);
    }

    /** Override in each concrete card class. Return true when the item should be consumed. */
    protected boolean apply(ServerPlayer user, InteractionHand hand, List<LivingEntity> targets) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        CardType cardType = itemStack.getOrDefault(AstralDataComponents.CARD_TYPE, this.definition.type());
        String key = String.format("tooltips.astral_craft.handcard.card_type.%s", cardType.getSerializedName());
        builder.accept(Component.translatable(key).withColor(cardType.color).withStyle(ChatFormatting.BOLD));
        MutableComponent component = Component.translatable(this.definition.effectKey());
        for (String line : component.getString().split("[\\n|]")) {
            builder.accept(Component.literal(line.trim()));
        }
    }

}