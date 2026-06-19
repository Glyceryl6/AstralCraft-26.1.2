package com.astral_craft.client.model.character;

import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

public class AstralCharacterAnimationRegistry {

    protected static final Map<Identifier, Map<String, String>> CHARACTER_ACTIONS = new HashMap<>();

    public static void registerAction(Identifier characterId, String actionId, String clipName) {
        if (characterId == null || actionId == null || actionId.isBlank() || clipName == null || clipName.isBlank()) return;
        CHARACTER_ACTIONS.computeIfAbsent(characterId, id -> new HashMap<>()).put(actionId, clipName);
    }

    public static String clipName(Identifier characterId, String actionId) {
        if (characterId == null || actionId == null || actionId.isBlank()) {
            return AstralCharacterAction.IDLE.id();
        }
        Map<String, String> actions = CHARACTER_ACTIONS.get(characterId);
        if (actions == null) return actionId;
        return actions.getOrDefault(actionId, actionId);
    }

    public static void clear(Identifier characterId) {
        if (characterId != null) {
            CHARACTER_ACTIONS.remove(characterId);
        }
    }

}
