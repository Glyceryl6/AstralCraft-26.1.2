package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

public record OpenBoardCharacterSelectionPayload(
        String boardId, String encodedCharacters, List<Identifier> occupiedCharacterIds,
        Identifier selectedCharacterId, Identifier selectedSkinId,
        int timeoutTicks, int timeoutDurationTicks, boolean selectionLocked, boolean refresh) implements CustomPacketPayload {

    private static final int MAXIMUM_CHARACTERS = 256;

    public static final Type<OpenBoardCharacterSelectionPayload> TYPE = new Type<>(AstralCraft.prefix("open_board_character_selection"));
    public static final StreamCodec<ByteBuf, OpenBoardCharacterSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, OpenBoardCharacterSelectionPayload::boardId,
            ByteBufCodecs.STRING_UTF8, OpenBoardCharacterSelectionPayload::encodedCharacters,
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list(MAXIMUM_CHARACTERS)),
            OpenBoardCharacterSelectionPayload::occupiedCharacterIds,
            Identifier.STREAM_CODEC, OpenBoardCharacterSelectionPayload::selectedCharacterId,
            Identifier.STREAM_CODEC, OpenBoardCharacterSelectionPayload::selectedSkinId,
            ByteBufCodecs.VAR_INT, OpenBoardCharacterSelectionPayload::timeoutTicks,
            ByteBufCodecs.VAR_INT, OpenBoardCharacterSelectionPayload::timeoutDurationTicks,
            ByteBufCodecs.BOOL, OpenBoardCharacterSelectionPayload::selectionLocked,
            ByteBufCodecs.BOOL, OpenBoardCharacterSelectionPayload::refresh,
            OpenBoardCharacterSelectionPayload::new);

    public OpenBoardCharacterSelectionPayload {
        timeoutTicks = Math.max(0, timeoutTicks);
        timeoutDurationTicks = Math.max(1, timeoutDurationTicks);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}