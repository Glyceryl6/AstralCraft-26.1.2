package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import com.astral_craft.common.gameplay.character.CharacterCodecLines;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.UUID;

public record OpenBoardCharacterSelectionPayload(
        UUID boardId, List<CharacterDefinition> characters, List<Identifier> occupiedCharacterIds,
        List<BoardCharacterSelectionEntry> lobbyEntries,
        Identifier selectedCharacterId, Identifier selectedSkinId,
        int timeoutTicks, int timeoutDurationTicks, boolean selectionLocked, boolean refresh) implements CustomPacketPayload {

    private static final int MAXIMUM_LOBBY_ENTRIES = 4;

    public static final Type<OpenBoardCharacterSelectionPayload> TYPE = new Type<>(AstralCraft.prefix("open_board_character_selection"));
    public static final StreamCodec<ByteBuf, OpenBoardCharacterSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, OpenBoardCharacterSelectionPayload::boardId,
            CharacterCodecLines.STREAM_CODEC, OpenBoardCharacterSelectionPayload::characters,
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list(CharacterCodecLines.MAXIMUM_CHARACTERS)),
            OpenBoardCharacterSelectionPayload::occupiedCharacterIds,
            BoardCharacterSelectionEntry.STREAM_CODEC.apply(ByteBufCodecs.list(MAXIMUM_LOBBY_ENTRIES)),
            OpenBoardCharacterSelectionPayload::lobbyEntries,
            Identifier.STREAM_CODEC, OpenBoardCharacterSelectionPayload::selectedCharacterId,
            Identifier.STREAM_CODEC, OpenBoardCharacterSelectionPayload::selectedSkinId,
            ByteBufCodecs.VAR_INT, OpenBoardCharacterSelectionPayload::timeoutTicks,
            ByteBufCodecs.VAR_INT, OpenBoardCharacterSelectionPayload::timeoutDurationTicks,
            ByteBufCodecs.BOOL, OpenBoardCharacterSelectionPayload::selectionLocked,
            ByteBufCodecs.BOOL, OpenBoardCharacterSelectionPayload::refresh,
            OpenBoardCharacterSelectionPayload::new);

    public OpenBoardCharacterSelectionPayload {
        characters = List.copyOf(characters);
        occupiedCharacterIds = List.copyOf(occupiedCharacterIds);
        lobbyEntries = List.copyOf(lobbyEntries);
        timeoutTicks = Math.max(0, timeoutTicks);
        timeoutDurationTicks = Math.max(1, timeoutDurationTicks);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}