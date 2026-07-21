package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.board.BoardEventTargets;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record GiveItemEventEffect(Holder<Item> item, int count) implements AstralEventEffect {

    public static final MapCodec<GiveItemEventEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Item.CODEC.fieldOf("id").forGetter(GiveItemEventEffect::item),
            Codec.INT.optionalFieldOf("count", 1).forGetter(GiveItemEventEffect::count)
    ).apply(instance, GiveItemEventEffect::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("give_item").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void apply(AstralEventContext context) {
        int safeCount = Math.max(1, this.count);
        var boardTarget = BoardEventTargets.resolve(context);
        if (boardTarget.isPresent()) {
            var target = boardTarget.get();
            var cardId = BuiltInRegistries.ITEM.getKey(this.item.value());
            var updated = target.participant();
            for (int index = 0; index < safeCount; index++) updated = updated.addCard(cardId);
            BoardSessionManager.updateParticipant(target.level(), target.session(), updated);
            return;
        }

        ServerPlayer receiver = context.targetPlayer() != null ? context.targetPlayer() : context.triggerPlayer();
        if (receiver == null) return;
        ItemStack stack = new ItemStack(this.item, safeCount);
        if (!receiver.addItem(stack) && !stack.isEmpty()) receiver.drop(stack, false);
    }

}