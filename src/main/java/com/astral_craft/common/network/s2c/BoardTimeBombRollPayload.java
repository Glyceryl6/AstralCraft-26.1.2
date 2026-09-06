package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record BoardTimeBombRollPayload(UUID boardId, int result, int rollTicks, int holdTicks) implements CustomPacketPayload {

    public static final Type<BoardTimeBombRollPayload> TYPE = new Type<>(AstralCraft.prefix("board_time_bomb_roll"));
    public static final StreamCodec<ByteBuf, BoardTimeBombRollPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, BoardTimeBombRollPayload::boardId,
            ByteBufCodecs.VAR_INT, BoardTimeBombRollPayload::result,
            ByteBufCodecs.VAR_INT, BoardTimeBombRollPayload::rollTicks,
            ByteBufCodecs.VAR_INT, BoardTimeBombRollPayload::holdTicks,
            BoardTimeBombRollPayload::new);

    public BoardTimeBombRollPayload {
        result = Math.clamp(result, 1, 6);
        rollTicks = Math.max(1, rollTicks);
        holdTicks = Math.max(1, holdTicks);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}