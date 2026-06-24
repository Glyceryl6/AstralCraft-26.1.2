package com.astral_craft.common.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.List;

public record CardUseRestriction(List<Identifier> characters, boolean requireSelectedCharacterUnlocked, boolean creativeBypass) {

    public static final CardUseRestriction NONE = new CardUseRestriction(List.of(), Boolean.TRUE, Boolean.TRUE);

    public static final Codec<CardUseRestriction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.listOf().optionalFieldOf("characters", List.of()).forGetter(CardUseRestriction::characters),
            Codec.BOOL.optionalFieldOf("require_selected_character_unlocked", true).forGetter(CardUseRestriction::requireSelectedCharacterUnlocked),
            Codec.BOOL.optionalFieldOf("creative_bypass", true).forGetter(CardUseRestriction::creativeBypass)
    ).apply(instance, CardUseRestriction::new));

    public static final StreamCodec<ByteBuf, CardUseRestriction> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public boolean unrestricted() {
        return this.characters.isEmpty();
    }

}