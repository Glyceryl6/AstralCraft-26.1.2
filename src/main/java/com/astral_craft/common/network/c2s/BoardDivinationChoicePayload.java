package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record BoardDivinationChoicePayload(UUID boardId, int selectedIndex) implements CustomPacketPayload {

    public static final Type<BoardDivinationChoicePayload> TYPE = new Type<>(AstralCraft.prefix("board_divination_choice"));
    public static final StreamCodec<ByteBuf, BoardDivinationChoicePayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, BoardDivinationChoicePayload::boardId,
            ByteBufCodecs.VAR_INT, BoardDivinationChoicePayload::selectedIndex,
            BoardDivinationChoicePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
