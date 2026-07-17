package com.astral_craft.common.network.s2c;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public record BoardCharacterSelectionEntry(
        int slot, String playerName, Identifier characterId, Identifier skinId,
        boolean selected, boolean confirmed) {

    public static final StreamCodec<ByteBuf, BoardCharacterSelectionEntry> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BoardCharacterSelectionEntry::slot,
            ByteBufCodecs.STRING_UTF8, BoardCharacterSelectionEntry::playerName,
            Identifier.STREAM_CODEC, BoardCharacterSelectionEntry::characterId,
            Identifier.STREAM_CODEC, BoardCharacterSelectionEntry::skinId,
            ByteBufCodecs.BOOL, BoardCharacterSelectionEntry::selected,
            ByteBufCodecs.BOOL, BoardCharacterSelectionEntry::confirmed,
            BoardCharacterSelectionEntry::new);

    public BoardCharacterSelectionEntry {
        slot = Math.clamp(slot, 0, 3);
    }
}
