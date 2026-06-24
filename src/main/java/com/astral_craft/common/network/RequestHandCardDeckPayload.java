package com.astral_craft.common.network;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RequestHandCardDeckPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RequestHandCardDeckPayload> TYPE = new CustomPacketPayload.Type<>(AstralCraft.prefix("request_hand_card_deck"));
    public static final StreamCodec<ByteBuf, RequestHandCardDeckPayload> STREAM_CODEC = StreamCodec.unit(new RequestHandCardDeckPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
