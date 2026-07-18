package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import com.astral_craft.common.network.s2c.OpenBoardShopPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;
import java.util.UUID;

public record BoardShopActionPayload(UUID boardId, List<Integer> offerIndexes, boolean leave) implements CustomPacketPayload {

    public static final Type<BoardShopActionPayload> TYPE = new Type<>(AstralCraft.prefix("board_shop_action"));
    public static final StreamCodec<ByteBuf, BoardShopActionPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, BoardShopActionPayload::boardId,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(OpenBoardShopPayload.MAXIMUM_ENCODED_OFFERS)), BoardShopActionPayload::offerIndexes,
            ByteBufCodecs.BOOL, BoardShopActionPayload::leave, BoardShopActionPayload::new);

    public BoardShopActionPayload {
        offerIndexes = List.copyOf(offerIndexes);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}