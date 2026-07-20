package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record OpenBoardEncounterPayload(
        UUID boardId, int targetEntityId, String controllerName,
        int timeoutTicks, int timeoutDurationTicks, boolean interactive,
        Identifier characterId, Identifier skinId) implements CustomPacketPayload {

    public static final Type<OpenBoardEncounterPayload> TYPE = new Type<>(AstralCraft.prefix("open_board_encounter"));
    public static final StreamCodec<ByteBuf, OpenBoardEncounterPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, OpenBoardEncounterPayload::boardId,
            ByteBufCodecs.VAR_INT, OpenBoardEncounterPayload::targetEntityId,
            ByteBufCodecs.STRING_UTF8, OpenBoardEncounterPayload::controllerName,
            ByteBufCodecs.VAR_INT, OpenBoardEncounterPayload::timeoutTicks,
            ByteBufCodecs.VAR_INT, OpenBoardEncounterPayload::timeoutDurationTicks,
            ByteBufCodecs.BOOL, OpenBoardEncounterPayload::interactive,
            Identifier.STREAM_CODEC, OpenBoardEncounterPayload::characterId,
            Identifier.STREAM_CODEC, OpenBoardEncounterPayload::skinId,
            OpenBoardEncounterPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}