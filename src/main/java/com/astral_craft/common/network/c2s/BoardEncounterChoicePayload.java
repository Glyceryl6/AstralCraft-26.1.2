package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record BoardEncounterChoicePayload(UUID boardId, boolean challenge) implements CustomPacketPayload {

    public static final Type<BoardEncounterChoicePayload> TYPE = new Type<>(AstralCraft.prefix("board_encounter_choice"));
    public static final StreamCodec<ByteBuf, BoardEncounterChoicePayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, BoardEncounterChoicePayload::boardId,
            ByteBufCodecs.BOOL, BoardEncounterChoicePayload::challenge,
            BoardEncounterChoicePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}