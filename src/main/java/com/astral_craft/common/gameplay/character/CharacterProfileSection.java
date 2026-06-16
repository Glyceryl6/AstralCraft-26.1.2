package com.astral_craft.common.gameplay.character;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record CharacterProfileSection(String titleKey, String bodyKey) {

    public static final Codec<CharacterProfileSection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("title_key").forGetter(CharacterProfileSection::titleKey),
            Codec.STRING.fieldOf("body_key").forGetter(CharacterProfileSection::bodyKey)
    ).apply(instance, CharacterProfileSection::new));

}