package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import com.astral_craft.common.network.CardTargetCandidate;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.ByIdMap;

import java.util.List;
import java.util.UUID;
import java.util.function.IntFunction;

public record OpenBoardPlatformTargetPayload(UUID boardId, Action action, List<CardTargetCandidate> candidates,
                                             int timeoutTicks, int timeoutDurationTicks) implements CustomPacketPayload {

    public static final Type<OpenBoardPlatformTargetPayload> TYPE = new Type<>(AstralCraft.prefix("open_board_platform_target"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenBoardPlatformTargetPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, OpenBoardPlatformTargetPayload::boardId,
            Action.STREAM_CODEC, OpenBoardPlatformTargetPayload::action,
            CardTargetCandidate.STREAM_CODEC.apply(ByteBufCodecs.list(OpenTargetSelectionPayload.MAX_CANDIDATES)), OpenBoardPlatformTargetPayload::candidates,
            ByteBufCodecs.VAR_INT, OpenBoardPlatformTargetPayload::timeoutTicks,
            ByteBufCodecs.VAR_INT, OpenBoardPlatformTargetPayload::timeoutDurationTicks,
            OpenBoardPlatformTargetPayload::new);

    public OpenBoardPlatformTargetPayload {
        candidates = List.copyOf(candidates);
        timeoutTicks = Math.max(0, timeoutTicks);
        timeoutDurationTicks = Math.max(1, timeoutDurationTicks);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum Action {
        FIRE,
        ASSAULT;

        private static final IntFunction<Action> BY_ID = ByIdMap.continuous(
                Action::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        public static final StreamCodec<ByteBuf, Action> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Action::ordinal);
    }
}
