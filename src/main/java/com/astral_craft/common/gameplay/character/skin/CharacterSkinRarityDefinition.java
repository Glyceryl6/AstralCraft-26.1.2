package com.astral_craft.common.gameplay.character.skin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record CharacterSkinRarityDefinition(String nameKey, int borderColor, int badgeColor, int textColor) {

    public static final Codec<CharacterSkinRarityDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name_key").forGetter(CharacterSkinRarityDefinition::nameKey),
            Codec.INT.optionalFieldOf("border_color", 0xFFFFFFFF).forGetter(CharacterSkinRarityDefinition::borderColor),
            Codec.INT.optionalFieldOf("badge_color", 0xFFFFFFFF).forGetter(CharacterSkinRarityDefinition::badgeColor),
            Codec.INT.optionalFieldOf("text_color", 0xFF101018).forGetter(CharacterSkinRarityDefinition::textColor)
    ).apply(instance, CharacterSkinRarityDefinition::new));

    public static CharacterSkinRarityDefinition none() {
        return new CharacterSkinRarityDefinition("skin_rarity.astral_craft.none", 0xFFFFFFFF, 0x00FFFFFF, 0xFFFFFFFF);
    }

}