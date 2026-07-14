package com.astral_craft.common.network;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenBoardEncounterPayload(String boardId, int targetEntityId, String controllerName, int timeoutTicks) implements CustomPacketPayload {

    public static final Type<OpenBoardEncounterPayload> TYPE = new Type<>(AstralCraft.prefix("open_board_encounter"));
    public static final StreamCodec<ByteBuf, OpenBoardEncounterPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, OpenBoardEncounterPayload::boardId,
            ByteBufCodecs.VAR_INT, OpenBoardEncounterPayload::targetEntityId,
            ByteBufCodecs.STRING_UTF8, OpenBoardEncounterPayload::controllerName,
            ByteBufCodecs.VAR_INT, OpenBoardEncounterPayload::timeoutTicks,
            OpenBoardEncounterPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
