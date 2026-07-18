package com.astral_craft.common.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record BoardCardView(int handIndex, ItemStack stack) {

    public static final StreamCodec<RegistryFriendlyByteBuf, BoardCardView> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BoardCardView::handIndex,
            ItemStack.OPTIONAL_STREAM_CODEC, BoardCardView::stack,
            BoardCardView::new);

    public BoardCardView {
        stack = stack == null ? ItemStack.EMPTY : stack.copy();
    }

}