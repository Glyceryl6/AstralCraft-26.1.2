package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CardBackSelectionPayload(Identifier selectedId) implements CustomPacketPayload {

    public static final Type<CardBackSelectionPayload> TYPE = new Type<>(AstralCraft.prefix("card_back_selection"));
    public static final StreamCodec<ByteBuf, CardBackSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, CardBackSelectionPayload::selectedId, CardBackSelectionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}