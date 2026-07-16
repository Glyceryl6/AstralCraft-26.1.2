package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BoardRouteStatePayload(
        String boardId, String encodedRoute,
        String encodedHighlightedRoute, String encodedBranches,
        int decisionTicks, int decisionDurationTicks,
        Identifier characterId, Identifier skinId,
        boolean active) implements CustomPacketPayload {

    public static final Type<BoardRouteStatePayload> TYPE = new Type<>(AstralCraft.prefix("board_route_state"));
    public static final StreamCodec<ByteBuf, BoardRouteStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BoardRouteStatePayload::boardId,
            ByteBufCodecs.STRING_UTF8, BoardRouteStatePayload::encodedRoute,
            ByteBufCodecs.STRING_UTF8, BoardRouteStatePayload::encodedHighlightedRoute,
            ByteBufCodecs.STRING_UTF8, BoardRouteStatePayload::encodedBranches,
            ByteBufCodecs.VAR_INT, BoardRouteStatePayload::decisionTicks,
            ByteBufCodecs.VAR_INT, BoardRouteStatePayload::decisionDurationTicks,
            Identifier.STREAM_CODEC, BoardRouteStatePayload::characterId,
            Identifier.STREAM_CODEC, BoardRouteStatePayload::skinId,
            ByteBufCodecs.BOOL, BoardRouteStatePayload::active,
            BoardRouteStatePayload::new);

    public BoardRouteStatePayload {
        decisionTicks = Math.max(0, decisionTicks);
        decisionDurationTicks = Math.max(1, decisionDurationTicks);
        characterId = characterId == null ? AstralCraft.prefix("mimi") : characterId;
        skinId = skinId == null ? Identifier.withDefaultNamespace("default") : skinId;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}