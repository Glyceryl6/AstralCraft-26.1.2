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

public record OpenBoardGamblePayload(UUID boardId, Phase phase, List<Entry> entries,
                                     boolean localCanChoose, int dieResult, int totalReward,
                                     int timeoutTicks, int timeoutDurationTicks) implements CustomPacketPayload {

    public static final Type<OpenBoardGamblePayload> TYPE = new Type<>(AstralCraft.prefix("open_board_gamble"));
    public static final StreamCodec<ByteBuf, OpenBoardGamblePayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, OpenBoardGamblePayload::boardId,
            Phase.STREAM_CODEC, OpenBoardGamblePayload::phase,
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list(4)), OpenBoardGamblePayload::entries,
            ByteBufCodecs.BOOL, OpenBoardGamblePayload::localCanChoose,
            ByteBufCodecs.VAR_INT, OpenBoardGamblePayload::dieResult,
            ByteBufCodecs.VAR_INT, OpenBoardGamblePayload::totalReward,
            ByteBufCodecs.VAR_INT, OpenBoardGamblePayload::timeoutTicks,
            ByteBufCodecs.VAR_INT, OpenBoardGamblePayload::timeoutDurationTicks,
            OpenBoardGamblePayload::new);

    public OpenBoardGamblePayload {
        entries = List.copyOf(entries);
        dieResult = Math.clamp(dieResult, 0, 6);
        totalReward = Math.max(0, totalReward);
        timeoutTicks = Math.max(0, timeoutTicks);
        timeoutDurationTicks = Math.max(1, timeoutDurationTicks);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum Phase {
        CHOOSING,
        ROLLING,
        RESULT;

        public static final StreamCodec<ByteBuf, Phase> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public Phase decode(ByteBuf buffer) {
                int ordinal = ByteBufCodecs.VAR_INT.decode(buffer);
                return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : CHOOSING;
            }

            @Override
            public void encode(ByteBuf buffer, Phase value) {
                ByteBufCodecs.VAR_INT.encode(buffer, value.ordinal());
            }
        };
    }

    public record Entry(UUID slotId, String name, Identifier characterId, Identifier skinId,
                        boolean eligible, boolean chosen, boolean winner) {
        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                BoardNetworkCodecs.UUID_STREAM_CODEC, Entry::slotId,
                ByteBufCodecs.STRING_UTF8, Entry::name,
                Identifier.STREAM_CODEC, Entry::characterId,
                Identifier.STREAM_CODEC, Entry::skinId,
                ByteBufCodecs.BOOL, Entry::eligible,
                ByteBufCodecs.BOOL, Entry::chosen,
                ByteBufCodecs.BOOL, Entry::winner,
                Entry::new);
    }

}