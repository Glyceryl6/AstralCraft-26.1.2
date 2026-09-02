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

public record BoardDeveloperConfigPayload(UUID boardId, Identifier humanCharacterId, Identifier humanSkinId, List<BotSetup> bots) implements CustomPacketPayload {

    public static final Type<BoardDeveloperConfigPayload> TYPE = new Type<>(AstralCraft.prefix("board_developer_config"));
    private static final StreamCodec<ByteBuf, CardCount> CARD_COUNT_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, CardCount::cardId,
            ByteBufCodecs.VAR_INT, CardCount::count,
            CardCount::new);
    private static final StreamCodec<ByteBuf, BotStats> BOT_STATS_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BotStats::baseAttack,
            ByteBufCodecs.VAR_INT, BotStats::baseDefense,
            ByteBufCodecs.VAR_INT, BotStats::maxHealth,
            ByteBufCodecs.VAR_INT, BotStats::health,
            ByteBufCodecs.VAR_INT, BotStats::starCoins,
            ByteBufCodecs.VAR_INT, BotStats::stars,
            ByteBufCodecs.VAR_INT, BotStats::cardPlaysPerTurn,
            ByteBufCodecs.VAR_INT, BotStats::cardPlaysRemaining,
            ByteBufCodecs.VAR_INT, BotStats::nextMoveFixed,
            BotStats::new);
    private static final StreamCodec<ByteBuf, BotSetup> BOT_SETUP_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, BotSetup::slotId,
            Identifier.STREAM_CODEC, BotSetup::characterId,
            Identifier.STREAM_CODEC, BotSetup::skinId,
            BOT_STATS_CODEC, BotSetup::stats,
            ByteBufCodecs.VAR_INT, BotSetup::skillCooldownTurns,
            ByteBufCodecs.VAR_INT, BotSetup::knockedDownTurns,
            ByteBufCodecs.VAR_INT, BotSetup::cardPlaysUsed,
            ByteBufCodecs.VAR_INT, BotSetup::maxHandSize,
            CARD_COUNT_CODEC.apply(ByteBufCodecs.list(256)), BotSetup::cards,
            BotSetup::new);
    public static final StreamCodec<ByteBuf, BoardDeveloperConfigPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, BoardDeveloperConfigPayload::boardId,
            Identifier.STREAM_CODEC, BoardDeveloperConfigPayload::humanCharacterId,
            Identifier.STREAM_CODEC, BoardDeveloperConfigPayload::humanSkinId,
            BOT_SETUP_CODEC.apply(ByteBufCodecs.list(3)), BoardDeveloperConfigPayload::bots,
            BoardDeveloperConfigPayload::new);

    public BoardDeveloperConfigPayload {
        bots = List.copyOf(bots);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record BotSetup(UUID slotId, Identifier characterId, Identifier skinId, BotStats stats,
                           int skillCooldownTurns, int knockedDownTurns, int cardPlaysUsed, int maxHandSize,
                           List<CardCount> cards) {
        public BotSetup {
            cards = List.copyOf(cards);
        }
    }

    public record BotStats(int baseAttack, int baseDefense, int maxHealth, int health, int starCoins, int stars,
                           int cardPlaysPerTurn, int cardPlaysRemaining, int nextMoveFixed) {}

    public record CardCount(Identifier cardId, int count) {}

}