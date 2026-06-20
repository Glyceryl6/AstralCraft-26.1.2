package com.astral_craft.common.gameplay.character;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SkinRarityDefinition(String nameKey, int borderColor, int badgeColor, int textColor) {

    public static final Codec<SkinRarityDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name_key").forGetter(SkinRarityDefinition::nameKey),
            Codec.INT.optionalFieldOf("border_color", 0xFFFFFFFF).forGetter(SkinRarityDefinition::borderColor),
            Codec.INT.optionalFieldOf("badge_color", 0xFFFFFFFF).forGetter(SkinRarityDefinition::badgeColor),
            Codec.INT.optionalFieldOf("text_color", 0xFF101018).forGetter(SkinRarityDefinition::textColor)
    ).apply(instance, SkinRarityDefinition::new));

    public static SkinRarityDefinition none() {
        return new SkinRarityDefinition("skin_rarity.astral_craft.none", 0xFFFFFFFF, 0x00FFFFFF, 0xFFFFFFFF);
    }

}