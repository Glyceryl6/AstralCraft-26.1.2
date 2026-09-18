package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.ByIdMap;

import java.util.List;
import java.util.UUID;
import java.util.function.IntFunction;

public record OpenBoardMatchmakingModeSelectionPayload(UUID boardId, State state, int elapsedTicks,
                                                        int countdownTicks, List<PlayerEntry> players) implements CustomPacketPayload {

    public static final int MAXIMUM_PLAYERS = 4;

    public static final Type<OpenBoardMatchmakingModeSelectionPayload> TYPE = new Type<>(AstralCraft.prefix("open_board_matchmaking_mode_selection"));
    public static final StreamCodec<ByteBuf, OpenBoardMatchmakingModeSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, OpenBoardMatchmakingModeSelectionPayload::boardId,
            State.STREAM_CODEC, OpenBoardMatchmakingModeSelectionPayload::state,
            ByteBufCodecs.VAR_INT, OpenBoardMatchmakingModeSelectionPayload::elapsedTicks,
            ByteBufCodecs.VAR_INT, OpenBoardMatchmakingModeSelectionPayload::countdownTicks,
            PlayerEntry.STREAM_CODEC.apply(ByteBufCodecs.list(MAXIMUM_PLAYERS)), OpenBoardMatchmakingModeSelectionPayload::players,
            OpenBoardMatchmakingModeSelectionPayload::new);

    public OpenBoardMatchmakingModeSelectionPayload {
        state = state == null ? State.IDLE : state;
        elapsedTicks = Math.max(0, elapsedTicks);
        countdownTicks = Math.max(0, countdownTicks);
        players = List.copyOf(players);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum State {
        IDLE,
        WAITING,
        MATCHED;

        private static final IntFunction<State> BY_ID = ByIdMap.continuous(
                State::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        public static final StreamCodec<ByteBuf, State> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, State::ordinal);
    }

    public record PlayerEntry(UUID playerId, String playerName) {
        public static final StreamCodec<ByteBuf, PlayerEntry> STREAM_CODEC = StreamCodec.composite(
                BoardNetworkCodecs.UUID_STREAM_CODEC, PlayerEntry::playerId,
                ByteBufCodecs.STRING_UTF8, PlayerEntry::playerName, PlayerEntry::new);
    }

}