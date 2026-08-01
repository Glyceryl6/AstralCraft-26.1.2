package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record OpenBoardRelicShopPayload(UUID boardId, int price, int starCoins, int timeoutTicks,
                                        int timeoutDurationTicks, Identifier characterId,
                                        Identifier skinId, int noticeCode) implements CustomPacketPayload {

    public static final Type<OpenBoardRelicShopPayload> TYPE = new Type<>(AstralCraft.prefix("open_board_relic_shop"));
    public static final StreamCodec<ByteBuf, OpenBoardRelicShopPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, OpenBoardRelicShopPayload::boardId,
            ByteBufCodecs.VAR_INT, OpenBoardRelicShopPayload::price,
            ByteBufCodecs.VAR_INT, OpenBoardRelicShopPayload::starCoins,
            ByteBufCodecs.VAR_INT, OpenBoardRelicShopPayload::timeoutTicks,
            ByteBufCodecs.VAR_INT, OpenBoardRelicShopPayload::timeoutDurationTicks,
            Identifier.STREAM_CODEC, OpenBoardRelicShopPayload::characterId,
            Identifier.STREAM_CODEC, OpenBoardRelicShopPayload::skinId,
            ByteBufCodecs.VAR_INT, OpenBoardRelicShopPayload::noticeCode,
            OpenBoardRelicShopPayload::new);

    public OpenBoardRelicShopPayload {
        price = Math.max(0, price);
        starCoins = Math.max(0, starCoins);
        timeoutTicks = Math.max(0, timeoutTicks);
        timeoutDurationTicks = Math.max(1, timeoutDurationTicks);
        noticeCode = Math.max(0, noticeCode);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
