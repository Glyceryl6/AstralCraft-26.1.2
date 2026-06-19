package com.astral_craft.common.gameplay.character;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@ParametersAreNonnullByDefault
public class CharacterManager extends SimpleJsonResourceReloadListener<CharacterDefinition> {

    public static final String DIRECTORY = "astral_party/characters";
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
        loaded.put(fallback.id(), fallback);
        for (Map.Entry<Identifier, CharacterDefinition> entry : elements.entrySet()) {
            CharacterDefinition definition = entry.getValue();
            Identifier id = entry.getKey();
            loaded.put(id, new CharacterDefinition(id,
                    definition.nameKey(),
                    definition.titleKey(),
                    definition.modelKey(),
                    definition.previewTexture(),
                    definition.entityTypeKey(),
                    definition.rendererKey(),
                    definition.animationSetKey(),
                    definition.previewAction(),
                    definition.maxPveLevel(),
                    definition.maxFriendshipLevel(),
                    definition.baseStats(),
                    definition.skills(),
                    definition.profileSections(),
                    definition.skins(),
                    definition.unlockedByDefault(),
                    definition.unlockHintKey(),
                    definition.sortOrder()));
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
        this.definitions.put(definition.id(), definition);
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

    public String encodeList() {
        return CharacterCodecLines.encode(this.values());
    }

}