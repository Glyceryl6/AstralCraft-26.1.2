package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import com.astral_craft.common.network.s2c.OpenBoardPlatformTargetPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record BoardPlatformTargetPayload(UUID boardId, OpenBoardPlatformTargetPayload.Action action,
                                         int entityId) implements CustomPacketPayload {

    public static final Type<BoardPlatformTargetPayload> TYPE = new Type<>(AstralCraft.prefix("board_platform_target"));
    public static final StreamCodec<ByteBuf, BoardPlatformTargetPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, BoardPlatformTargetPayload::boardId,
            new StreamCodec<>() {
                @Override
                public OpenBoardPlatformTargetPayload.Action decode(ByteBuf buffer) {
                    int ordinal = ByteBufCodecs.VAR_INT.decode(buffer);
                    OpenBoardPlatformTargetPayload.Action[] values = OpenBoardPlatformTargetPayload.Action.values();
                    return ordinal >= 0 && ordinal < values.length ? values[ordinal] : OpenBoardPlatformTargetPayload.Action.FIRE;
                }

                @Override
                public void encode(ByteBuf buffer, OpenBoardPlatformTargetPayload.Action value) {
                    ByteBufCodecs.VAR_INT.encode(buffer, value.ordinal());
                }
            }, BoardPlatformTargetPayload::action,
            ByteBufCodecs.VAR_INT, BoardPlatformTargetPayload::entityId,
            BoardPlatformTargetPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
