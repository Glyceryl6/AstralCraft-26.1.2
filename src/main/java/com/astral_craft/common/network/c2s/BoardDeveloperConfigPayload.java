package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.UUID;

public record BoardDeveloperConfigPayload(UUID boardId, List<BotSetup> bots) implements CustomPacketPayload {

    public static final Type<BoardDeveloperConfigPayload> TYPE = new Type<>(AstralCraft.prefix("board_developer_config"));
    private static final StreamCodec<ByteBuf, CardCount> CARD_COUNT_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, CardCount::cardId, ByteBufCodecs.VAR_INT, CardCount::count, CardCount::new);
    private static final StreamCodec<ByteBuf, BotSetup> BOT_SETUP_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, BotSetup::characterId, Identifier.STREAM_CODEC, BotSetup::skinId,
            CARD_COUNT_CODEC.apply(ByteBufCodecs.list(256)), BotSetup::cards, BotSetup::new);
    public static final StreamCodec<ByteBuf, BoardDeveloperConfigPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, BoardDeveloperConfigPayload::boardId,
            BOT_SETUP_CODEC.apply(ByteBufCodecs.list(3)), BoardDeveloperConfigPayload::bots,
            BoardDeveloperConfigPayload::new);

    public BoardDeveloperConfigPayload {
        bots = List.copyOf(bots);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record BotSetup(Identifier characterId, Identifier skinId, List<CardCount> cards) {
        public BotSetup {
            cards = List.copyOf(cards);
        }
    }

    public record CardCount(Identifier cardId, int count) {}

}