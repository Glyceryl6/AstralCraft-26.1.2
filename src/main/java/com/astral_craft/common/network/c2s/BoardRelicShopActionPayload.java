package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record BoardRelicShopActionPayload(UUID boardId, boolean buy) implements CustomPacketPayload {

    public static final Type<BoardRelicShopActionPayload> TYPE = new Type<>(AstralCraft.prefix("board_relic_shop_action"));
    public static final StreamCodec<ByteBuf, BoardRelicShopActionPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, BoardRelicShopActionPayload::boardId,
            ByteBufCodecs.BOOL, BoardRelicShopActionPayload::buy,
            BoardRelicShopActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
