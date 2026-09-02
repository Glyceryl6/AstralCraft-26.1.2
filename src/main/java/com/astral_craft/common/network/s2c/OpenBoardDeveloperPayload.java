package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.character.CharacterCodecLines;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.UUID;

public record OpenBoardDeveloperPayload(UUID boardId, List<CharacterDefinition> characters, List<Identifier> cardIds) implements CustomPacketPayload {

    public static final Type<OpenBoardDeveloperPayload> TYPE = new Type<>(AstralCraft.prefix("open_board_developer"));
    public static final StreamCodec<ByteBuf, OpenBoardDeveloperPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, OpenBoardDeveloperPayload::boardId,
            CharacterCodecLines.STREAM_CODEC, OpenBoardDeveloperPayload::characters,
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list(512)), OpenBoardDeveloperPayload::cardIds,
            OpenBoardDeveloperPayload::new);

    public OpenBoardDeveloperPayload {
        characters = List.copyOf(characters);
        cardIds = List.copyOf(cardIds);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}