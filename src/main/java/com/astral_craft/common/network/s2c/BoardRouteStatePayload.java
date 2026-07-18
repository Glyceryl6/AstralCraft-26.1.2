package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.UUID;

public record BoardRouteStatePayload(
        UUID boardId, List<List<BlockPos>> routes,
        List<List<BlockPos>> highlightedRoutes, List<BlockPos> branches,
        int decisionTicks, int decisionDurationTicks,
        Identifier characterId, Identifier skinId,
        boolean active) implements CustomPacketPayload {

    private static final int MAXIMUM_ROUTES = 96;
    private static final int MAXIMUM_POINTS_PER_ROUTE = 512;
    private static final StreamCodec<ByteBuf, List<BlockPos>> ROUTE_STREAM_CODEC =
            BoardNetworkCodecs.BLOCK_POS_STREAM_CODEC.apply(ByteBufCodecs.list(MAXIMUM_POINTS_PER_ROUTE));
    private static final StreamCodec<ByteBuf, List<List<BlockPos>>> ROUTES_STREAM_CODEC =
            ROUTE_STREAM_CODEC.apply(ByteBufCodecs.list(MAXIMUM_ROUTES));

    public static final Type<BoardRouteStatePayload> TYPE = new Type<>(AstralCraft.prefix("board_route_state"));
    public static final StreamCodec<ByteBuf, BoardRouteStatePayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, BoardRouteStatePayload::boardId,
            ROUTES_STREAM_CODEC, BoardRouteStatePayload::routes,
            ROUTES_STREAM_CODEC, BoardRouteStatePayload::highlightedRoutes,
            BoardNetworkCodecs.BLOCK_POS_STREAM_CODEC.apply(ByteBufCodecs.list(MAXIMUM_ROUTES)),
            BoardRouteStatePayload::branches,
            ByteBufCodecs.VAR_INT, BoardRouteStatePayload::decisionTicks,
            ByteBufCodecs.VAR_INT, BoardRouteStatePayload::decisionDurationTicks,
            Identifier.STREAM_CODEC, BoardRouteStatePayload::characterId,
            Identifier.STREAM_CODEC, BoardRouteStatePayload::skinId,
            ByteBufCodecs.BOOL, BoardRouteStatePayload::active,
            BoardRouteStatePayload::new);

    public BoardRouteStatePayload {
        routes = copyRoutes(routes);
        highlightedRoutes = copyRoutes(highlightedRoutes);
        branches = List.copyOf(branches);
        decisionTicks = Math.max(0, decisionTicks);
        decisionDurationTicks = Math.max(1, decisionDurationTicks);
    }

    private static List<List<BlockPos>> copyRoutes(List<List<BlockPos>> routes) {
        if (routes == null) return List.of();
        return routes.stream().map(List::copyOf).toList();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}