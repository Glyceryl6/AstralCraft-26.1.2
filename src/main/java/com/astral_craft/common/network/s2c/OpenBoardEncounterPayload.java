package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenBoardEncounterPayload(
        String boardId, int targetEntityId, String controllerName,
        int timeoutTicks, int timeoutDurationTicks,
        Identifier characterId, Identifier skinId) implements CustomPacketPayload {

    public static final Type<OpenBoardEncounterPayload> TYPE = new Type<>(AstralCraft.prefix("open_board_encounter"));
    public static final StreamCodec<ByteBuf, OpenBoardEncounterPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, OpenBoardEncounterPayload::boardId,
            ByteBufCodecs.VAR_INT, OpenBoardEncounterPayload::targetEntityId,
            ByteBufCodecs.STRING_UTF8, OpenBoardEncounterPayload::controllerName,
            ByteBufCodecs.VAR_INT, OpenBoardEncounterPayload::timeoutTicks,
            ByteBufCodecs.VAR_INT, OpenBoardEncounterPayload::timeoutDurationTicks,
            Identifier.STREAM_CODEC, OpenBoardEncounterPayload::characterId,
            Identifier.STREAM_CODEC, OpenBoardEncounterPayload::skinId,
            OpenBoardEncounterPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}