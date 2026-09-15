package com.astral_craft.common.gameplay.character.skin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

public record CharacterSkinDefinition(String id, String nameKey, Identifier texture, boolean unlockedByDefault, String rarity, BattlePresentation battlePresentation) {

    public static final Codec<CharacterSkinDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(CharacterSkinDefinition::id),
            Codec.STRING.fieldOf("name_key").forGetter(CharacterSkinDefinition::nameKey),
            Identifier.CODEC.fieldOf("texture").forGetter(CharacterSkinDefinition::texture),
            Codec.BOOL.optionalFieldOf("unlocked_by_default", false).forGetter(CharacterSkinDefinition::unlockedByDefault),
            Codec.STRING.optionalFieldOf("rarity", "none").forGetter(CharacterSkinDefinition::rarity),
            BattlePresentation.CODEC.optionalFieldOf("battle", BattlePresentation.NONE).forGetter(CharacterSkinDefinition::battlePresentation)
    ).apply(instance, CharacterSkinDefinition::new));

    public CharacterSkinDefinition(String id, String nameKey, Identifier texture, boolean unlockedByDefault, String rarity) {
        this(id, nameKey, texture, unlockedByDefault, rarity, BattlePresentation.NONE);
    }

    public CharacterSkinDefinition(String id, String nameKey, Identifier texture, boolean unlockedByDefault) {
        this(id, nameKey, texture, unlockedByDefault, "none");
    }

    public CharacterSkinDefinition {
        battlePresentation = battlePresentation == null ? BattlePresentation.NONE : battlePresentation;
    }

    public String rarityOrNone() {
        return this.rarity == null || this.rarity.isBlank() ? "none" : this.rarity;
    }

    public String rarityOrCommon() {
        return this.rarityOrNone();
    }

    public record BattlePresentation(List<Identifier> backgrounds, int frameTicks, Optional<Identifier> bgm) {

        public static final int MAX_BACKGROUND_FRAMES = 32;
        public static final BattlePresentation NONE = new BattlePresentation(List.of(), 4, Optional.empty());
        public static final Codec<BattlePresentation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Identifier.CODEC.listOf().optionalFieldOf("backgrounds", List.of()).forGetter(BattlePresentation::backgrounds),
                Codec.intRange(1, 120).optionalFieldOf("frame_ticks", 4).forGetter(BattlePresentation::frameTicks),
                Identifier.CODEC.optionalFieldOf("bgm").forGetter(BattlePresentation::bgm)
        ).apply(instance, BattlePresentation::new));

        public BattlePresentation {
            if (backgrounds == null || backgrounds.isEmpty()) {
                backgrounds = List.of();
            } else {
                backgrounds = List.copyOf(backgrounds.subList(0, Math.min(backgrounds.size(), MAX_BACKGROUND_FRAMES)));
            }
            frameTicks = Math.clamp(frameTicks, 1, 120);
            bgm = bgm == null ? Optional.empty() : bgm;
        }

        public Identifier backgroundAt(int ageTicks) {
            if (this.backgrounds.isEmpty()) return null;
            int index = Math.floorMod(ageTicks / this.frameTicks, this.backgrounds.size());
            return this.backgrounds.get(index);
        }

    }

}