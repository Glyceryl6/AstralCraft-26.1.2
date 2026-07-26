package com.astral_craft.common.gameplay.character;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.character.skill.CharacterSkillView;
import com.astral_craft.common.gameplay.character.skin.CharacterSkinDefinition;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.List;

/** Immutable network/UI view of a registered character. Runtime behavior remains in {@link AstralCharacter}. */
public record CharacterDefinition(
        Identifier id,
        Identifier modelKey,
        Identifier entityTypeKey,
        Identifier rendererKey,
        Identifier animationSetKey,
        String previewAction,
        CharacterStatsDefinition baseStats,
        List<CharacterSkillView> skills,
        List<CharacterProfileSection> profileSections,
        List<CharacterSkinDefinition> skins,
        CharacterPotentialDefinition potential,
        boolean implicitBondSkin,
        boolean unlockedByDefault,
        String unlockHintKey,
        int sortOrder) {

    private static final MapCodec<Presentation> PRESENTATION_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.optionalFieldOf("id", AstralCraft.prefix("mimi")).forGetter(Presentation::id),
            Identifier.CODEC.optionalFieldOf("model", AstralCraft.prefix("humanoid")).forGetter(Presentation::modelKey),
            Identifier.CODEC.optionalFieldOf("entity_type", AstralCraft.prefix("astral_character")).forGetter(Presentation::entityTypeKey),
            Identifier.CODEC.optionalFieldOf("renderer", AstralCraft.prefix("player")).forGetter(Presentation::rendererKey),
            Identifier.CODEC.optionalFieldOf("animation_set", AstralCraft.prefix("humanoid")).forGetter(Presentation::animationSetKey),
            Codec.STRING.optionalFieldOf("preview_action", "idle").forGetter(Presentation::previewAction)
    ).apply(instance, Presentation::new));

    private static final MapCodec<Content> CONTENT_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            CharacterStatsDefinition.CODEC.optionalFieldOf("base_stats", CharacterStatsDefinition.defaultStats()).forGetter(Content::baseStats),
            CharacterSkillView.CODEC.listOf().optionalFieldOf("skills", List.of()).forGetter(Content::skills),
            CharacterProfileSection.CODEC.listOf().optionalFieldOf("profile", List.of()).forGetter(Content::profileSections),
            CharacterSkinDefinition.CODEC.listOf().optionalFieldOf("skins", List.of()).forGetter(Content::skins)
    ).apply(instance, Content::new));

    private static final MapCodec<Progression> PROGRESSION_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            CharacterPotentialDefinition.CODEC.optionalFieldOf("potential", CharacterPotentialDefinition.NONE).forGetter(Progression::potential),
            Codec.BOOL.optionalFieldOf("implicit_bond_skin", true).forGetter(Progression::implicitBondSkin),
            Codec.BOOL.optionalFieldOf("unlocked_by_default", false).forGetter(Progression::unlockedByDefault),
            Codec.STRING.optionalFieldOf("unlock_hint_key", "").forGetter(Progression::unlockHintKey),
            Codec.INT.optionalFieldOf("sort_order", 1000).forGetter(Progression::sortOrder)
    ).apply(instance, Progression::new));

    public static final Codec<CharacterDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PRESENTATION_CODEC.forGetter(CharacterDefinition::presentation),
            CONTENT_CODEC.forGetter(CharacterDefinition::content),
            PROGRESSION_CODEC.forGetter(CharacterDefinition::progression)
    ).apply(instance, CharacterDefinition::fromParts));
    public static final StreamCodec<ByteBuf, CharacterDefinition> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public CharacterDefinition {
        skills = List.copyOf(skills);
        profileSections = List.copyOf(profileSections);
        skins = List.copyOf(skins);
        potential = potential == null ? CharacterPotentialDefinition.NONE : potential;
        unlockHintKey = unlockHintKey == null ? "" : unlockHintKey;
    }

    private static CharacterDefinition fromParts(Presentation presentation, Content content, Progression progression) {
        return new CharacterDefinition(presentation.id(), presentation.modelKey(), presentation.entityTypeKey(),
                presentation.rendererKey(), presentation.animationSetKey(), presentation.previewAction(), content.baseStats(),
                content.skills(), content.profileSections(), content.skins(), progression.potential(),
                progression.implicitBondSkin(), progression.unlockedByDefault(), progression.unlockHintKey(), progression.sortOrder());
    }

    private Presentation presentation() {
        return new Presentation(this.id, this.modelKey, this.entityTypeKey, this.rendererKey, this.animationSetKey, this.previewAction);
    }

    private Content content() {
        return new Content(this.baseStats, this.skills, this.profileSections, this.skins);
    }

    private Progression progression() {
        return new Progression(this.potential, this.implicitBondSkin, this.unlockedByDefault, this.unlockHintKey, this.sortOrder);
    }

    public static CharacterDefinition builtinDefault() {
        Identifier id = AstralCraft.prefix("mimi");
        CharacterSkinDefinition skin = new CharacterSkinDefinition("default", "character.astral_craft.mimi.skin.default",
                AstralCraft.prefix("entity/character/skin_mimi_default"), true);
        return new CharacterDefinition(id, AstralCraft.prefix("humanoid"), AstralCraft.prefix("astral_character"),
                AstralCraft.prefix("player"), AstralCraft.prefix("humanoid"), "idle",
                new CharacterStatsDefinition(1, 1, 9, 6),
                List.of(new CharacterSkillView("active", true, 3, -1, -1, false, false),
                        new CharacterSkillView("passive", false, 0, -1, -1, false, false)),
                List.of(new CharacterProfileSection("", "character.astral_craft.mimi.profile.basic.body")),
                List.of(skin), CharacterPotentialDefinition.NONE, true, true,
                "character.astral_craft.mimi.unlock_hint", 80);
    }

    public String getDescriptionId() {
        return "character." + this.id.getNamespace() + "." + this.id.getPath() + ".name";
    }

    public String getTitleDescriptionId() {
        return "character." + this.id.getNamespace() + "." + this.id.getPath() + ".title";
    }

    public int maxPveLevel() {
        return CharacterProgressEntry.MAX_PVE_LEVEL;
    }

    public int maxFriendshipLevel() {
        return CharacterProgressEntry.MAX_FRIENDSHIP_LEVEL;
    }

    public Identifier previewTexture() {
        return this.skins.isEmpty() ? Identifier.fromNamespaceAndPath(this.id.getNamespace(),
                "entity/character/skin_" + this.id.getPath() + "_default") : this.skins.getFirst().texture();
    }

    public boolean supportsPotential() {
        return this.potential.enabled();
    }

    public String potentialDescriptionKey() {
        return this.potentialLocalizationKey("desc");
    }

    public String potentialEffectKey() {
        return this.potentialLocalizationKey("effect");
    }

    public CharacterPotentialDefinition potentialOrDefault() {
        return this.supportsPotential() ? this.potential : CharacterPotentialDefinition.NONE;
    }

    public String skillNameKey(CharacterSkillView skill, CharacterSkillView.SkillMode mode) {
        return this.skillLocalizationKey(skill, mode, "");
    }

    public String skillDescriptionKey(CharacterSkillView skill, CharacterSkillView.SkillMode mode) {
        return this.skillLocalizationKey(skill, mode, ".desc");
    }

    public CharacterSkinDefinition skinOrDefault(String skinId) {
        for (CharacterSkinDefinition skin : this.skins) if (skin.id().equals(skinId)) return skin;
        if (!this.skins.isEmpty()) return this.skins.getFirst();
        return new CharacterSkinDefinition("default", this.getDescriptionId(), this.previewTexture(), true);
    }

    private String potentialLocalizationKey(String suffix) {
        return "character." + this.id.getNamespace() + "." + this.id.getPath() + ".potential." + suffix;
    }

    private String skillLocalizationKey(CharacterSkillView skill, CharacterSkillView.SkillMode mode, String suffix) {
        String skillId = skill == null ? "active" : skill.serializedId();
        String base = "character." + this.id.getNamespace() + "." + this.id.getPath() + ".skill." + skillId;
        if (skill != null && mode == CharacterSkillView.SkillMode.PVE && skill.hasPveSpecificText()) return base + ".pve" + suffix;
        if (skill != null && mode == CharacterSkillView.SkillMode.PVP && skill.hasPvpSpecificText()) return base + ".pvp" + suffix;
        return base + suffix;
    }

    private record Presentation(Identifier id, Identifier modelKey, Identifier entityTypeKey, Identifier rendererKey,
                                Identifier animationSetKey, String previewAction) {}

    private record Content(CharacterStatsDefinition baseStats, List<CharacterSkillView> skills,
                           List<CharacterProfileSection> profileSections, List<CharacterSkinDefinition> skins) {}

    private record Progression(CharacterPotentialDefinition potential, boolean implicitBondSkin,
                               boolean unlockedByDefault, String unlockHintKey, int sortOrder) {}

}