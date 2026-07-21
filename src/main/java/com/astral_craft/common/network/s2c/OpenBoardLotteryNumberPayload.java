package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.UUID;

public record OpenBoardLotteryNumberPayload(UUID boardId, List<Integer> selectedNumbers,
                                             int timeoutTicks, int timeoutDurationTicks,
                                             Identifier characterId, Identifier skinId,
                                             boolean sharedEvent, boolean localCanChoose,
                                             List<Entry> entries) implements CustomPacketPayload {

    private static final StreamCodec<ByteBuf, List<Entry>> ENTRY_LIST_CODEC = Entry.STREAM_CODEC.apply(ByteBufCodecs.list(4));
    public static final Type<OpenBoardLotteryNumberPayload> TYPE = new Type<>(AstralCraft.prefix("open_board_lottery_number"));
    public static final StreamCodec<ByteBuf, OpenBoardLotteryNumberPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, OpenBoardLotteryNumberPayload::boardId,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(12)), OpenBoardLotteryNumberPayload::selectedNumbers,
            ByteBufCodecs.VAR_INT, OpenBoardLotteryNumberPayload::timeoutTicks,
            ByteBufCodecs.VAR_INT, OpenBoardLotteryNumberPayload::timeoutDurationTicks,
            Identifier.STREAM_CODEC, OpenBoardLotteryNumberPayload::characterId,
            Identifier.STREAM_CODEC, OpenBoardLotteryNumberPayload::skinId,
            ByteBufCodecs.BOOL, OpenBoardLotteryNumberPayload::sharedEvent,
            ByteBufCodecs.BOOL, OpenBoardLotteryNumberPayload::localCanChoose,
            ENTRY_LIST_CODEC, OpenBoardLotteryNumberPayload::entries,
            OpenBoardLotteryNumberPayload::new);

    public OpenBoardLotteryNumberPayload(UUID boardId, List<Integer> selectedNumbers, int timeoutTicks,
                                         int timeoutDurationTicks, Identifier characterId, Identifier skinId) {
        this(boardId, selectedNumbers, timeoutTicks, timeoutDurationTicks, characterId, skinId, false, true, List.of());
    }

    public OpenBoardLotteryNumberPayload {
        selectedNumbers = List.copyOf(selectedNumbers);
        timeoutTicks = Math.max(1, timeoutTicks);
        timeoutDurationTicks = Math.max(1, timeoutDurationTicks);
        entries = List.copyOf(entries);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(String name, Identifier characterId, Identifier skinId, boolean chosen) {
        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Entry::name,
                Identifier.STREAM_CODEC, Entry::characterId,
                Identifier.STREAM_CODEC, Entry::skinId,
                ByteBufCodecs.BOOL, Entry::chosen,
                Entry::new);

        public Entry {
            name = name == null ? "" : name;
        }
    }
}
