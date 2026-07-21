package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;
import java.util.UUID;

public record OpenBoardLotteryDrawPayload(UUID boardId, Phase phase, int finalNumber, int jackpot,
                                          List<Entry> entries, List<String> winnerNames, int awardEach,
                                          int timeoutTicks, int timeoutDurationTicks) implements CustomPacketPayload {

    public static final Type<OpenBoardLotteryDrawPayload> TYPE = new Type<>(AstralCraft.prefix("open_board_lottery_draw"));
    public static final StreamCodec<ByteBuf, OpenBoardLotteryDrawPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, OpenBoardLotteryDrawPayload::boardId,
            Phase.STREAM_CODEC, OpenBoardLotteryDrawPayload::phase,
            ByteBufCodecs.VAR_INT, OpenBoardLotteryDrawPayload::finalNumber,
            ByteBufCodecs.VAR_INT, OpenBoardLotteryDrawPayload::jackpot,
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list(4)), OpenBoardLotteryDrawPayload::entries,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(4)), OpenBoardLotteryDrawPayload::winnerNames,
            ByteBufCodecs.VAR_INT, OpenBoardLotteryDrawPayload::awardEach,
            ByteBufCodecs.VAR_INT, OpenBoardLotteryDrawPayload::timeoutTicks,
            ByteBufCodecs.VAR_INT, OpenBoardLotteryDrawPayload::timeoutDurationTicks,
            OpenBoardLotteryDrawPayload::new);

    public OpenBoardLotteryDrawPayload {
        finalNumber = Math.clamp(finalNumber, 1, 12);
        jackpot = Math.max(10, jackpot);
        entries = List.copyOf(entries);
        winnerNames = List.copyOf(winnerNames);
        awardEach = Math.max(0, awardEach);
        timeoutTicks = Math.max(0, timeoutTicks);
        timeoutDurationTicks = Math.max(1, timeoutDurationTicks);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(String name, List<Integer> numbers) {
        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Entry::name,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(12)), Entry::numbers,
                Entry::new);

        public Entry {
            name = name == null ? "" : name;
            numbers = List.copyOf(numbers);
        }
    }

    public enum Phase {
        ROLLING,
        RESULT;

        public static final StreamCodec<ByteBuf, Phase> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public Phase decode(ByteBuf buffer) {
                int ordinal = ByteBufCodecs.VAR_INT.decode(buffer);
                return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : ROLLING;
            }

            @Override
            public void encode(ByteBuf buffer, Phase value) {
                ByteBufCodecs.VAR_INT.encode(buffer, value.ordinal());
            }
        };
    }
}
