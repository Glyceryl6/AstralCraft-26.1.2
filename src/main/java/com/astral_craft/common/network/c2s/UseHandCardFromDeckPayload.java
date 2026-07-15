package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record UseHandCardFromDeckPayload(Identifier cardId) implements CustomPacketPayload {

    public static final Type<UseHandCardFromDeckPayload> TYPE = new Type<>(AstralCraft.prefix("use_hand_card_from_deck"));
    public static final StreamCodec<ByteBuf, UseHandCardFromDeckPayload> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, UseHandCardFromDeckPayload::cardId, UseHandCardFromDeckPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}