package com.astral_craft.common.network;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenHandCardDeckPayload(String encodedCards, boolean creative) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenHandCardDeckPayload> TYPE = new CustomPacketPayload.Type<>(AstralCraft.prefix("open_hand_card_deck"));
    public static final StreamCodec<ByteBuf, OpenHandCardDeckPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            OpenHandCardDeckPayload::encodedCards,
            ByteBufCodecs.BOOL,
            OpenHandCardDeckPayload::creative,
            OpenHandCardDeckPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
