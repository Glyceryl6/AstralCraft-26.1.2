package com.astral_craft.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public record BoardPanelEdgeView(Identifier firstNodeId, Identifier secondNodeId) {

    public static final StreamCodec<ByteBuf, BoardPanelEdgeView> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, BoardPanelEdgeView::firstNodeId,
            Identifier.STREAM_CODEC, BoardPanelEdgeView::secondNodeId,
            BoardPanelEdgeView::new);

}