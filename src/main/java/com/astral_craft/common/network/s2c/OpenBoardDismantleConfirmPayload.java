package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record OpenBoardDismantleConfirmPayload(UUID boardId, int panelCount) implements CustomPacketPayload {

    public static final Type<OpenBoardDismantleConfirmPayload> TYPE = new Type<>(AstralCraft.prefix("open_board_dismantle_confirm"));
    public static final StreamCodec<ByteBuf, OpenBoardDismantleConfirmPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, OpenBoardDismantleConfirmPayload::boardId,
            ByteBufCodecs.VAR_INT, OpenBoardDismantleConfirmPayload::panelCount,
            OpenBoardDismantleConfirmPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
