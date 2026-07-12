package com.astral_craft.common.network;

import com.astral_craft.AstralCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

public record OpenCardNumberSelectionPayload(ItemStack cardStack, int minValue, int maxValue) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenCardNumberSelectionPayload> TYPE =
            new CustomPacketPayload.Type<>(AstralCraft.prefix("open_card_number_selection"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenCardNumberSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC, OpenCardNumberSelectionPayload::cardStack,
            ByteBufCodecs.VAR_INT, OpenCardNumberSelectionPayload::minValue,
            ByteBufCodecs.VAR_INT, OpenCardNumberSelectionPayload::maxValue,
            OpenCardNumberSelectionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}