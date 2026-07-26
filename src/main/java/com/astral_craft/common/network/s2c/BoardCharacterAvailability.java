package com.astral_craft.common.network.s2c;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.List;

/** Per-viewer unlock and preferred-skin snapshot used by the board character selector. */
public record BoardCharacterAvailability(Identifier characterId, Identifier preferredSkinId,
                                         boolean unlocked, List<Identifier> unlockedSkinIds) {

    public static final int MAXIMUM_SKINS = 256;
    public static final StreamCodec<ByteBuf, BoardCharacterAvailability> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, BoardCharacterAvailability::characterId,
            Identifier.STREAM_CODEC, BoardCharacterAvailability::preferredSkinId,
            ByteBufCodecs.BOOL, BoardCharacterAvailability::unlocked,
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list(MAXIMUM_SKINS)), BoardCharacterAvailability::unlockedSkinIds,
            BoardCharacterAvailability::new);

    public BoardCharacterAvailability {
        unlockedSkinIds = List.copyOf(unlockedSkinIds);
    }

    public boolean isSkinUnlocked(String skinId) {
        return this.unlockedSkinIds.stream().anyMatch(id -> id.getPath().equals(skinId));
    }

}