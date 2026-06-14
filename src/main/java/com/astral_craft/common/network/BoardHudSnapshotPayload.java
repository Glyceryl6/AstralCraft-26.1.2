package com.astral_craft.common.network;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Compact snapshot for the client HUD board/minimap. */
public record BoardHudSnapshotPayload(String encoded) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BoardHudSnapshotPayload> TYPE = new CustomPacketPayload.Type<>(AstralCraft.prefix("board_hud_snapshot"));

    public static final StreamCodec<ByteBuf, BoardHudSnapshotPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BoardHudSnapshotPayload::encoded, BoardHudSnapshotPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}