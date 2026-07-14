package com.astral_craft.common.network;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record BoardEncounterChoicePayload(String boardId, boolean challenge) implements CustomPacketPayload {

    public static final Type<BoardEncounterChoicePayload> TYPE = new Type<>(AstralCraft.prefix("board_encounter_choice"));
    public static final StreamCodec<ByteBuf, BoardEncounterChoicePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BoardEncounterChoicePayload::boardId,
            ByteBufCodecs.BOOL, BoardEncounterChoicePayload::challenge,
            BoardEncounterChoicePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
