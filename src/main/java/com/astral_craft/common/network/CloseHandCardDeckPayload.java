package com.astral_craft.common.network;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CloseHandCardDeckPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CloseHandCardDeckPayload> TYPE = new CustomPacketPayload.Type<>(AstralCraft.prefix("close_hand_card_deck"));
    public static final StreamCodec<ByteBuf, CloseHandCardDeckPayload> STREAM_CODEC = StreamCodec.unit(new CloseHandCardDeckPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}