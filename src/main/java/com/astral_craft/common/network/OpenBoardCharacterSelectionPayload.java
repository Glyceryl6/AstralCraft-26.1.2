package com.astral_craft.common.network;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenBoardCharacterSelectionPayload(
        String boardId, String encodedCharacters, String occupiedCharacterIds,
        String selectedCharacterId, String selectedSkinId, int timeoutTicks, boolean refresh) implements CustomPacketPayload {

    public static final Type<OpenBoardCharacterSelectionPayload> TYPE = new Type<>(AstralCraft.prefix("open_board_character_selection"));
    public static final StreamCodec<ByteBuf, OpenBoardCharacterSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, OpenBoardCharacterSelectionPayload::boardId,
            ByteBufCodecs.STRING_UTF8, OpenBoardCharacterSelectionPayload::encodedCharacters,
            ByteBufCodecs.STRING_UTF8, OpenBoardCharacterSelectionPayload::occupiedCharacterIds,
            ByteBufCodecs.STRING_UTF8, OpenBoardCharacterSelectionPayload::selectedCharacterId,
            ByteBufCodecs.STRING_UTF8, OpenBoardCharacterSelectionPayload::selectedSkinId,
            ByteBufCodecs.VAR_INT, OpenBoardCharacterSelectionPayload::timeoutTicks,
            ByteBufCodecs.BOOL, OpenBoardCharacterSelectionPayload::refresh,
            OpenBoardCharacterSelectionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
