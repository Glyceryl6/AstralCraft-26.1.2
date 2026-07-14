package com.astral_craft.common.network;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record BoardRouteStatePayload(String boardId, String encodedRoute, String encodedBranches, boolean active) implements CustomPacketPayload {

    public static final Type<BoardRouteStatePayload> TYPE = new Type<>(AstralCraft.prefix("board_route_state"));
    public static final StreamCodec<ByteBuf, BoardRouteStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BoardRouteStatePayload::boardId,
            ByteBufCodecs.STRING_UTF8, BoardRouteStatePayload::encodedRoute,
            ByteBufCodecs.STRING_UTF8, BoardRouteStatePayload::encodedBranches,
            ByteBufCodecs.BOOL, BoardRouteStatePayload::active,
            BoardRouteStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
