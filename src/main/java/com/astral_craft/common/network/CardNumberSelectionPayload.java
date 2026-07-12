package com.astral_craft.common.network;

import com.astral_craft.AstralCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

public record CardNumberSelectionPayload(ItemStack cardStack, int value) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CardNumberSelectionPayload> TYPE =
            new CustomPacketPayload.Type<>(AstralCraft.prefix("card_number_selection"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CardNumberSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC,
            CardNumberSelectionPayload::cardStack,
            ByteBufCodecs.VAR_INT,
            CardNumberSelectionPayload::value,
            CardNumberSelectionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
