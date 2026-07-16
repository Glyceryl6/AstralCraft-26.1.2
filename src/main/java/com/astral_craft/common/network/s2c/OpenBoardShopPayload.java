package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

public record OpenBoardShopPayload(
        String boardId,
        List<Identifier> offers,
        int purchasedMask,
        int starCoins,
        int cardPrice,
        int timeoutTicks,
        int timeoutDurationTicks,
        Identifier characterId,
        Identifier skinId,
        int noticeCode) implements CustomPacketPayload {

    public static final int MAXIMUM_OFFERS = 3;
    public static final Type<OpenBoardShopPayload> TYPE = new Type<>(AstralCraft.prefix("open_board_shop"));
    public static final StreamCodec<ByteBuf, OpenBoardShopPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, OpenBoardShopPayload::boardId,
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list(MAXIMUM_OFFERS)), OpenBoardShopPayload::offers,
            ByteBufCodecs.VAR_INT, OpenBoardShopPayload::purchasedMask,
            ByteBufCodecs.VAR_INT, OpenBoardShopPayload::starCoins,
            ByteBufCodecs.VAR_INT, OpenBoardShopPayload::cardPrice,
            ByteBufCodecs.VAR_INT, OpenBoardShopPayload::timeoutTicks,
            ByteBufCodecs.VAR_INT, OpenBoardShopPayload::timeoutDurationTicks,
            Identifier.STREAM_CODEC, OpenBoardShopPayload::characterId,
            Identifier.STREAM_CODEC, OpenBoardShopPayload::skinId,
            ByteBufCodecs.VAR_INT, OpenBoardShopPayload::noticeCode,
            OpenBoardShopPayload::new);

    public OpenBoardShopPayload {
        offers = List.copyOf(offers).stream().limit(MAXIMUM_OFFERS).toList();
        purchasedMask = Math.max(0, purchasedMask);
        starCoins = Math.max(0, starCoins);
        cardPrice = Math.max(0, cardPrice);
        timeoutTicks = Math.max(0, timeoutTicks);
        timeoutDurationTicks = Math.max(1, timeoutDurationTicks);
        noticeCode = Math.max(0, noticeCode);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}