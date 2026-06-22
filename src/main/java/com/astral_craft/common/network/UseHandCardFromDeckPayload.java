package com.astral_craft.common.network;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record UseHandCardFromDeckPayload(String cardId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<UseHandCardFromDeckPayload> TYPE = new CustomPacketPayload.Type<>(AstralCraft.prefix("use_hand_card_from_deck"));

    public static final StreamCodec<ByteBuf, UseHandCardFromDeckPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, UseHandCardFromDeckPayload::cardId, UseHandCardFromDeckPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}