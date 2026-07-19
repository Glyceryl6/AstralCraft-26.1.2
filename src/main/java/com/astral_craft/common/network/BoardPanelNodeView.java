package com.astral_craft.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.List;

public record BoardPanelNodeView(Identifier nodeId, BlockPos position, boolean valid, List<BoardPanelOccupantView> occupants) {

    public static final StreamCodec<ByteBuf, BoardPanelNodeView> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, BoardPanelNodeView::nodeId,
            BoardNetworkCodecs.BLOCK_POS_STREAM_CODEC, BoardPanelNodeView::position,
            ByteBufCodecs.BOOL, BoardPanelNodeView::valid,
            BoardPanelOccupantView.STREAM_CODEC.apply(ByteBufCodecs.list(4)), BoardPanelNodeView::occupants,
            BoardPanelNodeView::new);

    public BoardPanelNodeView {
        occupants = List.copyOf(occupants);
    }

}