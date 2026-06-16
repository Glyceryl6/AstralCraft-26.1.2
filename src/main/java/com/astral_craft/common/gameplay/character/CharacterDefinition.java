package com.astral_craft.common.gameplay.character;

import com.astral_craft.AstralCraft;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;

public record CharacterDefinition(
        Identifier id,
        String nameKey,
        String titleKey,
        Identifier modelKey,
        Identifier previewTexture,
        CharacterStatsDefinition baseStats,
        List<CharacterSkillDefinition> skills,
        List<CharacterProfileSection> profileSections,
        List<CharacterSkinDefinition> skins) {

    public static final Codec<CharacterDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.optionalFieldOf("id", AstralCraft.prefix("mimi")).forGetter(CharacterDefinition::id),
            Codec.STRING.fieldOf("name_key").forGetter(CharacterDefinition::nameKey),
            Codec.STRING.optionalFieldOf("title_key", "character.astral_craft.default.title").forGetter(CharacterDefinition::titleKey),
            Identifier.CODEC.optionalFieldOf("model", AstralCraft.prefix("humanoid")).forGetter(CharacterDefinition::modelKey),
            Identifier.CODEC.optionalFieldOf("preview_texture", AstralCraft.prefix("textures/entity/character/default.png")).forGetter(CharacterDefinition::previewTexture),
            CharacterStatsDefinition.CODEC.optionalFieldOf("base_stats", CharacterStatsDefinition.defaultStats()).forGetter(CharacterDefinition::baseStats),
            CharacterSkillDefinition.CODEC.listOf().optionalFieldOf("skills", List.of()).forGetter(CharacterDefinition::skills),
            CharacterProfileSection.CODEC.listOf().optionalFieldOf("profile", List.of()).forGetter(CharacterDefinition::profileSections),
            CharacterSkinDefinition.CODEC.listOf().optionalFieldOf("skins", List.of()).forGetter(CharacterDefinition::skins)
    ).apply(instance, CharacterDefinition::new));

    public static CharacterDefinition builtinDefault() {
        Identifier id = AstralCraft.prefix("mimi");
        return new CharacterDefinition(id,
                "character.astral_craft.mimi.name",
                "character.astral_craft.mimi.title",
                AstralCraft.prefix("humanoid"),
                AstralCraft.prefix("textures/entity/character/mimi.png"),
                new CharacterStatsDefinition(1, 2, 10, 0),
                List.of(new CharacterSkillDefinition("trouble_maker", "character.astral_craft.mimi.skill.active", "character.astral_craft.mimi.skill.active.desc", 3)),
                List.of(new CharacterProfileSection("character.astral_craft.mimi.profile.basic", "character.astral_craft.mimi.profile.basic.body")),
                List.of(new CharacterSkinDefinition("default", "character.astral_craft.mimi.skin.default", AstralCraft.prefix("textures/entity/character/mimi.png"), true)));
    }

    public CharacterSkinDefinition skinOrDefault(String skinId) {
        for (CharacterSkinDefinition skin : this.skins) {
            if (skin.id().equals(skinId)) return skin;
        }

        return this.skins.isEmpty() ? new CharacterSkinDefinition("default", this.nameKey, this.previewTexture, true) : this.skins.getFirst();
    }

}