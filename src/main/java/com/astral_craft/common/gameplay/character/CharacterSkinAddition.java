package com.astral_craft.common.gameplay.character;

import com.astral_craft.AstralCraft;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

public record CharacterSkinAddition(Identifier character, String id, String nameKey, Identifier texture, boolean unlockedByDefault, String rarity) {

    public static final Codec<CharacterSkinAddition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("character").forGetter(CharacterSkinAddition::character),
            Codec.STRING.fieldOf("id").forGetter(CharacterSkinAddition::id),
            Codec.STRING.optionalFieldOf("name_key", "").forGetter(CharacterSkinAddition::nameKey),
            Identifier.CODEC.fieldOf("texture").forGetter(CharacterSkinAddition::texture),
            Codec.BOOL.optionalFieldOf("unlocked_by_default", false).forGetter(CharacterSkinAddition::unlockedByDefault),
            Codec.STRING.optionalFieldOf("rarity", "none").forGetter(CharacterSkinAddition::rarity)
    ).apply(instance, CharacterSkinAddition::new));

    public CharacterSkinDefinition toSkinDefinition(Identifier sourceFile) {
        String safeId = this.id == null || this.id.isBlank() ? sourceFile.getPath() : this.id;
        Identifier safeTexture = this.texture.equals(AstralCraft.prefix("entity/character/default.png"))
                ? CharacterSkinManager.defaultSkinTexture(this.character, safeId) : this.texture;
        String safeNameKey = this.nameKey == null || this.nameKey.isBlank()
                ? "character." + this.character.getNamespace() + "." + this.character.getPath() + ".skin." + safeId : this.nameKey;
        String safeRarity = this.rarity == null || this.rarity.isBlank() ? "none" : this.rarity;
        return new CharacterSkinDefinition(safeId, safeNameKey, safeTexture, this.unlockedByDefault, safeRarity);
    }

}