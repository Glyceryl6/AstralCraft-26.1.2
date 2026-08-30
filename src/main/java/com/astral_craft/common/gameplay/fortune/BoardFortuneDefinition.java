package com.astral_craft.common.gameplay.fortune;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;

public record BoardFortuneDefinition(Identifier id, String nameKey, String descriptionKey, Identifier texture,
                                     BoardFortuneCategory category, int weight, List<AstralEventEffect> effects) {

    public static final Codec<BoardFortuneDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.optionalFieldOf("id", AstralCraft.prefix("unknown_fortune"))
                    .forGetter(BoardFortuneDefinition::id),
            Codec.STRING.optionalFieldOf("name_key", "").forGetter(BoardFortuneDefinition::nameKey),
            Codec.STRING.optionalFieldOf("description_key", "").forGetter(BoardFortuneDefinition::descriptionKey),
            Identifier.CODEC.optionalFieldOf("texture", AstralCraft.prefix("textures/gui/cards/fortune/unknown.png"))
                    .forGetter(BoardFortuneDefinition::texture),
            BoardFortuneCategory.CODEC.optionalFieldOf("category", BoardFortuneCategory.NEUTRAL)
                    .forGetter(BoardFortuneDefinition::category),
            Codec.intRange(1, 1000).optionalFieldOf("weight", 1).forGetter(BoardFortuneDefinition::weight),
            AstralEventEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(BoardFortuneDefinition::effects)
    ).apply(instance, BoardFortuneDefinition::new));

    public BoardFortuneDefinition {
        id = id == null ? AstralCraft.prefix("unknown_fortune") : id;
        nameKey = nameKey == null || nameKey.isBlank()
                ? "fortune." + id.getNamespace() + "." + id.getPath() + ".name" : nameKey;
        descriptionKey = descriptionKey == null || descriptionKey.isBlank()
                ? "fortune." + id.getNamespace() + "." + id.getPath() + ".description" : descriptionKey;
        texture = texture == null ? AstralCraft.prefix("textures/gui/cards/fortune/unknown.png") : texture;
        category = category == null ? BoardFortuneCategory.NEUTRAL : category;
        weight = Math.max(1, weight);
        effects = List.copyOf(effects == null ? List.of() : effects);
    }

}