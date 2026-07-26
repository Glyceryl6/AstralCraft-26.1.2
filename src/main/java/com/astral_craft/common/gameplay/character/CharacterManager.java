package com.astral_craft.common.gameplay.character;

import com.astral_craft.common.gameplay.character.skin.CharacterSkinDefinition;
import com.astral_craft.common.gameplay.character.skin.CharacterSkinManager;
import com.astral_craft.common.registry.AstralCharacters;
import net.minecraft.resources.Identifier;

import java.util.*;

/** Runtime view of registered characters plus resource-pack supplied skins. */
public class CharacterManager {

    public static final CharacterManager INSTANCE = new CharacterManager();

    public List<CharacterDefinition> values() {
        List<CharacterDefinition> values = new ArrayList<>();
        for (Identifier id : AstralCharacters.REGISTRY.keySet()) {
            AstralCharacter character = AstralCharacters.REGISTRY.getValue(id);
            if (character != null) values.add(this.withRuntimeSkins(id, character.definition(id)));
        }
        if (values.isEmpty()) values.add(CharacterDefinition.builtinDefault());
        values.sort(Comparator.comparingInt(CharacterDefinition::sortOrder).thenComparing(value -> value.id().toString()));
        return values;
    }

    public boolean contains(Identifier id) {
        return id != null && AstralCharacters.REGISTRY.getValue(id) != null;
    }

    public CharacterDefinition get(Identifier id) {
        AstralCharacter character = id == null ? null : AstralCharacters.REGISTRY.getValue(id);
        return character == null ? this.defaultCharacter() : this.withRuntimeSkins(id, character.definition(id));
    }

    public AstralCharacter character(Identifier id) {
        AstralCharacter character = id == null ? null : AstralCharacters.REGISTRY.getValue(id);
        if (character != null) return character;
        for (Identifier key : AstralCharacters.REGISTRY.keySet()) {
            AstralCharacter fallback = AstralCharacters.REGISTRY.getValue(key);
            if (fallback != null) return fallback;
        }
        return new AstralCharacter(new AstralCharacter.Properties(), CharacterProgressionDefinition.of(80).unlockedByDefault());
    }

    public CharacterDefinition defaultCharacter() {
        List<CharacterDefinition> values = this.values();
        return values.isEmpty() ? CharacterDefinition.builtinDefault() : values.getFirst();
    }

    protected CharacterDefinition withRuntimeSkins(Identifier id, CharacterDefinition definition) {
        List<CharacterSkinDefinition> skins = this.mergeImplicitAndAdditionalSkins(id, definition);
        return new CharacterDefinition(id, definition.modelKey(), definition.entityTypeKey(), definition.rendererKey(),
                definition.animationSetKey(), definition.previewAction(), definition.baseStats(), definition.skills(),
                definition.profileSections(), skins, definition.potential(), definition.implicitBondSkin(),
                definition.unlockedByDefault(), definition.unlockHintKey(), definition.sortOrder());
    }

    protected List<CharacterSkinDefinition> mergeImplicitAndAdditionalSkins(Identifier characterId, CharacterDefinition definition) {
        Map<String, CharacterSkinDefinition> merged = new LinkedHashMap<>();
        this.putSkin(merged, CharacterSkinManager.defaultSkin(characterId));
        if (definition.implicitBondSkin()) this.putSkin(merged, CharacterSkinManager.bondSkin(characterId));
        for (CharacterSkinDefinition skin : CharacterSkinManager.INSTANCE.skinsFor(characterId)) this.putSkin(merged, skin);
        return new ArrayList<>(merged.values());
    }

    protected void putSkin(Map<String, CharacterSkinDefinition> merged, CharacterSkinDefinition skin) {
        if (skin != null && skin.id() != null && !skin.id().isBlank()) merged.put(skin.id(), skin);
    }

}