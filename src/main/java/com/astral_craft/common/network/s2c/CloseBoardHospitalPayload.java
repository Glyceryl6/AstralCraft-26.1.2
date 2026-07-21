package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record CloseBoardHospitalPayload(UUID boardId) implements CustomPacketPayload {

    public static final Type<CloseBoardHospitalPayload> TYPE = new Type<>(AstralCraft.prefix("close_board_hospital"));
    public static final StreamCodec<ByteBuf, CloseBoardHospitalPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, CloseBoardHospitalPayload::boardId,
            CloseBoardHospitalPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
