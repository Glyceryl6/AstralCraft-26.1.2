package com.astral_craft.client.gameplay.character;

import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.CharacterManager;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class ClientCharacterDefinitionCache {

    public static final ClientCharacterDefinitionCache INSTANCE = new ClientCharacterDefinitionCache();

    protected final Map<Identifier, CharacterDefinition> definitions = new LinkedHashMap<>();

    public void replace(Collection<CharacterDefinition> definitions) {
        this.definitions.clear();
        if (definitions == null) return;
        for (CharacterDefinition definition : definitions) {
            if (definition == null || definition.id() == null) continue;
            this.definitions.put(definition.id(), definition);
        }
    }

    public boolean contains(Identifier id) {
        if (id == null) return false;
        return this.definitions.containsKey(id) || CharacterManager.INSTANCE.contains(id);
    }

    public CharacterDefinition getOrFallback(Identifier id) {
        if (id != null) {
            CharacterDefinition definition = this.definitions.get(id);
            if (definition != null) return definition;
            if (CharacterManager.INSTANCE.contains(id)) {
                return CharacterManager.INSTANCE.get(id);
            }
        }

        return CharacterManager.INSTANCE.defaultCharacter();
    }

}