package com.astral_craft.common.gameplay.character;

import com.astral_craft.AstralCraft;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;

public record CharacterDefinition(
        Identifier id,
        String nameKey,
        String titleKey,
        Identifier modelKey,
        Identifier previewTexture,
        Identifier entityTypeKey,
        Identifier rendererKey,
        Identifier animationSetKey,
        String previewAction,
        int maxPveLevel,
        int maxFriendshipLevel,
        CharacterStatsDefinition baseStats,
        List<CharacterSkillDefinition> skills,
        List<CharacterProfileSection> profileSections,
        List<CharacterSkinDefinition> skins,
        boolean unlockedByDefault,
        String unlockHintKey,
        int sortOrder) {

    private static final MapCodec<CharacterIdentity> IDENTITY_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.optionalFieldOf("id", AstralCraft.prefix("mimi")).forGetter(CharacterIdentity::id),
            Codec.STRING.fieldOf("name_key").forGetter(CharacterIdentity::nameKey),
            Codec.STRING.optionalFieldOf("title_key", "character.astral_craft.default.title").forGetter(CharacterIdentity::titleKey),
            Identifier.CODEC.optionalFieldOf("model", AstralCraft.prefix("humanoid")).forGetter(CharacterIdentity::modelKey),
            Identifier.CODEC.fieldOf("preview_texture").forGetter(CharacterIdentity::previewTexture),
            Identifier.CODEC.optionalFieldOf("entity_type", AstralCraft.prefix("astral_character")).forGetter(CharacterIdentity::entityTypeKey),
            Identifier.CODEC.optionalFieldOf("renderer", AstralCraft.prefix("player")).forGetter(CharacterIdentity::rendererKey),
            Identifier.CODEC.optionalFieldOf("animation_set", AstralCraft.prefix("humanoid")).forGetter(CharacterIdentity::animationSetKey),
            Codec.STRING.optionalFieldOf("preview_action", "idle").forGetter(CharacterIdentity::previewAction)
    ).apply(instance, CharacterIdentity::new));

    private static final MapCodec<CharacterProgressionMetadata> PROGRESSION_METADATA_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.optionalFieldOf("max_pve_level", 6).forGetter(CharacterProgressionMetadata::maxPveLevel),
            Codec.INT.optionalFieldOf("max_friendship_level", 5).forGetter(CharacterProgressionMetadata::maxFriendshipLevel),
            Codec.BOOL.optionalFieldOf("unlocked_by_default", false).forGetter(CharacterProgressionMetadata::unlockedByDefault),
            Codec.STRING.fieldOf("unlock_hint_key").forGetter(CharacterProgressionMetadata::unlockHintKey),
            Codec.INT.optionalFieldOf("sort_order", 1000).forGetter(CharacterProgressionMetadata::sortOrder)
    ).apply(instance, CharacterProgressionMetadata::new));

    private static final MapCodec<CharacterContent> CONTENT_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            CharacterStatsDefinition.CODEC.optionalFieldOf("base_stats", CharacterStatsDefinition.defaultStats()).forGetter(CharacterContent::baseStats),
            CharacterSkillDefinition.CODEC.listOf().optionalFieldOf("skills", List.of()).forGetter(CharacterContent::skills),
            CharacterProfileSection.CODEC.listOf().optionalFieldOf("profile", List.of()).forGetter(CharacterContent::profileSections),
            CharacterSkinDefinition.CODEC.listOf().optionalFieldOf("skins", List.of()).forGetter(CharacterContent::skins)
    ).apply(instance, CharacterContent::new));

    public static final Codec<CharacterDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IDENTITY_CODEC.forGetter(CharacterDefinition::identity),
            PROGRESSION_METADATA_CODEC.forGetter(CharacterDefinition::progressionMetadata),
            CONTENT_CODEC.forGetter(CharacterDefinition::content)
    ).apply(instance, CharacterDefinition::fromCodecParts));

    private static CharacterDefinition fromCodecParts(CharacterIdentity identity, CharacterProgressionMetadata progressionMetadata, CharacterContent content) {
        return new CharacterDefinition(
                identity.id(),
                identity.nameKey(),
                identity.titleKey(),
                identity.modelKey(),
                identity.previewTexture(),
                identity.entityTypeKey(),
                identity.rendererKey(),
                identity.animationSetKey(),
                identity.previewAction(),
                progressionMetadata.maxPveLevel(),
                progressionMetadata.maxFriendshipLevel(),
                content.baseStats(),
                content.skills(),
                content.profileSections(),
                content.skins(),
                progressionMetadata.unlockedByDefault(),
                progressionMetadata.unlockHintKey(),
                progressionMetadata.sortOrder());
    }

    private CharacterIdentity identity() {
        return new CharacterIdentity(
                this.id,
                this.nameKey,
                this.titleKey,
                this.modelKey,
                this.previewTexture,
                this.entityTypeKey,
                this.rendererKey,
                this.animationSetKey,
                this.previewAction);
    }

    private CharacterProgressionMetadata progressionMetadata() {
        return new CharacterProgressionMetadata(
                this.maxPveLevel,
                this.maxFriendshipLevel,
                this.unlockedByDefault,
                this.unlockHintKey,
                this.sortOrder);
    }

    private CharacterContent content() {
        return new CharacterContent(
                this.baseStats,
                this.skills,
                this.profileSections,
                this.skins);
    }

    private record CharacterIdentity(
            Identifier id,
            String nameKey,
            String titleKey,
            Identifier modelKey,
            Identifier previewTexture,
            Identifier entityTypeKey,
            Identifier rendererKey,
            Identifier animationSetKey,
            String previewAction) {
    }

    private record CharacterProgressionMetadata(
            int maxPveLevel,
            int maxFriendshipLevel,
            boolean unlockedByDefault,
            String unlockHintKey,
            int sortOrder) {
    }

    private record CharacterContent(
            CharacterStatsDefinition baseStats,
            List<CharacterSkillDefinition> skills,
            List<CharacterProfileSection> profileSections,
            List<CharacterSkinDefinition> skins) {
    }

    public static CharacterDefinition builtinDefault() {
        Identifier id = AstralCraft.prefix("mimi");
        return new CharacterDefinition(id,
                "character.astral_craft.mimi.name",
                "character.astral_craft.mimi.title",
                AstralCraft.prefix("humanoid"),
                AstralCraft.prefix("entity/character/skin_mimi_default"),
                AstralCraft.prefix("astral_character"),
                AstralCraft.prefix("player"),
                AstralCraft.prefix("humanoid"),
                "idle",
                6,
                5,
                new CharacterStatsDefinition(1, 1, 9),
                List.of(new CharacterSkillDefinition("active", "character.astral_craft.mimi.skill.active", "character.astral_craft.mimi.skill.active.desc", 3)),
                List.of(new CharacterProfileSection("", "character.astral_craft.mimi.profile.basic.body")),
                List.of(new CharacterSkinDefinition("default", "character.astral_craft.mimi.skin.default", AstralCraft.prefix("entity/character/skin_mimi_default"), true)),
                true,
                "character.astral_craft.unlock_hint.default",
                80);
    }

    public CharacterSkinDefinition skinOrDefault(String skinId) {
        for (CharacterSkinDefinition skin : this.skins) {
            if (skin.id().equals(skinId)) return skin;
        }

        return this.skins.isEmpty() ? new CharacterSkinDefinition("default", this.nameKey, this.previewTexture, true) : this.skins.getFirst();
    }

}