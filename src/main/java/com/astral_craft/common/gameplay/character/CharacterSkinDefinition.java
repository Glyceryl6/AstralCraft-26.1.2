package com.astral_craft.common.gameplay.character;

import com.astral_craft.AstralCraft;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

public record CharacterSkinDefinition(String id, String nameKey, Identifier texture, boolean unlockedByDefault) {

    public static final Codec<CharacterSkinDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(CharacterSkinDefinition::id),
            Codec.STRING.fieldOf("name_key").forGetter(CharacterSkinDefinition::nameKey),
            Identifier.CODEC.optionalFieldOf("texture", AstralCraft.prefix("textures/entity/character/default.png")).forGetter(CharacterSkinDefinition::texture),
            Codec.BOOL.optionalFieldOf("unlocked_by_default", false).forGetter(CharacterSkinDefinition::unlockedByDefault)
    ).apply(instance, CharacterSkinDefinition::new));

}