package com.astral_craft.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public record BoardPanelOccupantView(Identifier characterId, Identifier skinId) {

    public static final StreamCodec<ByteBuf, BoardPanelOccupantView> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, BoardPanelOccupantView::characterId,
            Identifier.STREAM_CODEC, BoardPanelOccupantView::skinId,
            BoardPanelOccupantView::new);

}