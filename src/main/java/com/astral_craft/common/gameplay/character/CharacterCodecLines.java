package com.astral_craft.common.gameplay.character;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/** Shared typed network codec for character definitions. */
public class CharacterCodecLines {

    public static final int MAXIMUM_CHARACTERS = 256;
    public static final StreamCodec<ByteBuf, List<CharacterDefinition>> STREAM_CODEC =
            CharacterDefinition.STREAM_CODEC.apply(ByteBufCodecs.list(MAXIMUM_CHARACTERS));
}
