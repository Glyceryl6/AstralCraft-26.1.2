package com.astral_craft.common.network;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CardBackSelectionPayload(String selectedId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CardBackSelectionPayload> TYPE = new CustomPacketPayload.Type<>(AstralCraft.prefix("card_back_selection"));

    public static final StreamCodec<ByteBuf, CardBackSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, CardBackSelectionPayload::selectedId, CardBackSelectionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}