package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.ByIdMap;

import java.util.UUID;
import java.util.function.IntFunction;

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

        private static final IntFunction<Phase> BY_ID = ByIdMap.continuous(
                Phase::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        private static final StreamCodec<ByteBuf, Phase> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Phase::ordinal);
    }

    public enum Result {
        INJECTION,
        HOSPITALIZED;

        private static final IntFunction<Result> BY_ID = ByIdMap.continuous(
                Result::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        private static final StreamCodec<ByteBuf, Result> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Result::ordinal);
    }
}
