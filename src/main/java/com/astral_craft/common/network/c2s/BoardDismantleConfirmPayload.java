package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.ByIdMap;

import java.util.UUID;
import java.util.function.IntFunction;

public record BoardDismantleConfirmPayload(UUID boardId, Action action) implements CustomPacketPayload {

    public static final Type<BoardDismantleConfirmPayload> TYPE = new Type<>(AstralCraft.prefix("board_dismantle_confirm"));
    public static final StreamCodec<ByteBuf, BoardDismantleConfirmPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, BoardDismantleConfirmPayload::boardId,
            Action.STREAM_CODEC, BoardDismantleConfirmPayload::action,
            BoardDismantleConfirmPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum Action {

        REMOVE_DATA_ONLY,
        REMOVE_DATA_AND_PANELS;

        private static final IntFunction<Action> BY_ID = ByIdMap.continuous(Action::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        public static final StreamCodec<ByteBuf, Action> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Action::ordinal);

    }

}