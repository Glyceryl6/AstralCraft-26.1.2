package com.astral_craft.common.gameplay.character;

import com.astral_craft.common.gameplay.character.skin.CharacterSkinDefinition;
import com.astral_craft.common.gameplay.character.skin.CharacterSkinManager;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@ParametersAreNonnullByDefault
public class CharacterManager extends SimpleJsonResourceReloadListener<CharacterDefinition> {

    public static final String DIRECTORY = "astral_craft/characters";
    public static final CharacterManager INSTANCE = new CharacterManager();

    protected final Map<Identifier, CharacterDefinition> definitions = new LinkedHashMap<>();

    public CharacterManager() {
        super(CharacterDefinition.CODEC, FileToIdConverter.json(DIRECTORY));
        this.resetToDefault();
    }

    @Override
    protected void apply(Map<Identifier, CharacterDefinition> elements, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, CharacterDefinition> loaded = new LinkedHashMap<>();
        CharacterDefinition fallback = CharacterDefinition.builtinDefault();
        loaded.put(fallback.id(), this.withRuntimeIdAndSkins(fallback.id(), fallback));
        for (Map.Entry<Identifier, CharacterDefinition> entry : elements.entrySet()) {
            CharacterDefinition definition = entry.getValue();
            Identifier id = entry.getKey();
            loaded.put(id, this.withRuntimeIdAndSkins(id, definition));
        }

        this.definitions.clear();
        loaded.values().stream().sorted(Comparator.comparingInt(CharacterDefinition::sortOrder).thenComparing(value -> value.id().toString()))
                .forEach(value -> this.definitions.put(value.id(), value));
        if (this.definitions.isEmpty()) {
            this.resetToDefault();
        }
    }

    public void resetToDefault() {
        this.definitions.clear();
        CharacterDefinition definition = CharacterDefinition.builtinDefault();
        this.definitions.put(definition.id(), this.withRuntimeIdAndSkins(definition.id(), definition));
    }

    protected CharacterDefinition withRuntimeIdAndSkins(Identifier id, CharacterDefinition definition) {
        List<CharacterSkinDefinition> skins = this.mergeImplicitAndAdditionalSkins(id, definition);
        Identifier previewTexture = skins.isEmpty() ? definition.previewTexture() : skins.getFirst().texture();
        return new CharacterDefinition(id,
                definition.nameKey(),
                definition.titleKey(),
                definition.modelKey(),
                previewTexture,
                definition.entityTypeKey(),
                definition.rendererKey(),
                definition.animationSetKey(),
                definition.previewAction(),
                definition.maxPveLevel(),
                definition.maxFriendshipLevel(),
                definition.baseStats(),
                definition.skills(),
                definition.profileSections(),
                skins,
                definition.hasPotential(),
                definition.potential(),
                definition.implicitDefaultSkin(),
                definition.implicitBondSkin(),
                definition.unlockedByDefault(),
                definition.unlockHintKey(),
                definition.sortOrder());
    }

    protected List<CharacterSkinDefinition> mergeImplicitAndAdditionalSkins(Identifier characterId, CharacterDefinition definition) {
        Map<String, CharacterSkinDefinition> merged = new LinkedHashMap<>();
        this.putSkin(merged, CharacterSkinManager.defaultSkin(characterId));
        if (definition.implicitBondSkin()) {
            this.putSkin(merged, CharacterSkinManager.bondSkin(characterId));
        }

        if (definition.skins() != null) {
            for (CharacterSkinDefinition skin : definition.skins()) {
                this.putSkin(merged, skin);
            }
        }

        for (CharacterSkinDefinition skin : CharacterSkinManager.INSTANCE.skinsFor(characterId)) {
            this.putSkin(merged, skin);
        }

        return new ArrayList<>(merged.values());
    }

    protected void putSkin(Map<String, CharacterSkinDefinition> merged, CharacterSkinDefinition skin) {
        if (skin.id() == null || skin.id().isBlank()) return;
        merged.put(skin.id(), skin);
    }

    public List<CharacterDefinition> values() {
        return new ArrayList<>(this.definitions.values());
    }

    public boolean contains(Identifier id) {
        return this.definitions.containsKey(id);
    }

    public CharacterDefinition get(Identifier id) {
        CharacterDefinition fallback = this.defaultCharacter();
        return this.definitions.getOrDefault(id, fallback);
    }

    public CharacterDefinition defaultCharacter() {
        return this.definitions.values().stream().findFirst().orElse(CharacterDefinition.builtinDefault());
    }

}