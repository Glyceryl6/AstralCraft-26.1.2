package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record OpenBoardHospitalPayload(UUID boardId, Phase phase, Result result,
                                       int timeoutTicks, int timeoutDurationTicks) implements CustomPacketPayload {

    public static final Type<OpenBoardHospitalPayload> TYPE = new Type<>(AstralCraft.prefix("open_board_hospital"));
    public static final StreamCodec<ByteBuf, OpenBoardHospitalPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, OpenBoardHospitalPayload::boardId,
            Phase.STREAM_CODEC, OpenBoardHospitalPayload::phase,
            Result.STREAM_CODEC, OpenBoardHospitalPayload::result,
            ByteBufCodecs.VAR_INT, OpenBoardHospitalPayload::timeoutTicks,
            ByteBufCodecs.VAR_INT, OpenBoardHospitalPayload::timeoutDurationTicks,
            OpenBoardHospitalPayload::new);

    public OpenBoardHospitalPayload {
        timeoutTicks = Math.max(0, timeoutTicks);
        timeoutDurationTicks = Math.max(1, timeoutDurationTicks);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum Phase {
        CHECKING,
        RESULT;

        private static final StreamCodec<ByteBuf, Phase> STREAM_CODEC = enumCodec(values(), CHECKING);
    }

    public enum Result {
        INJECTION,
        HOSPITALIZED;

        private static final StreamCodec<ByteBuf, Result> STREAM_CODEC = enumCodec(values(), INJECTION);
    }

    private static <T> StreamCodec<ByteBuf, T> enumCodec(T[] values, T fallback) {
        return new StreamCodec<>() {
            @Override
            public T decode(ByteBuf buffer) {
                int ordinal = ByteBufCodecs.VAR_INT.decode(buffer);
                return ordinal >= 0 && ordinal < values.length ? values[ordinal] : fallback;
            }

            @Override
            public void encode(ByteBuf buffer, T value) {
                ByteBufCodecs.VAR_INT.encode(buffer, ((Enum<?>) value).ordinal());
            }
        };
    }
}
