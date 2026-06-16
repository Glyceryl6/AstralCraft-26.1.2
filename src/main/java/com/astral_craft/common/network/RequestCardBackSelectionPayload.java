package com.astral_craft.common.network;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RequestCardBackSelectionPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RequestCardBackSelectionPayload> TYPE = new CustomPacketPayload.Type<>(AstralCraft.prefix("request_card_back_selection"));
    public static final StreamCodec<ByteBuf, RequestCardBackSelectionPayload> STREAM_CODEC = StreamCodec.unit(new RequestCardBackSelectionPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}