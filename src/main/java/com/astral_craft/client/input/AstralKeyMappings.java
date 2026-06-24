package com.astral_craft.client.input;

import com.astral_craft.AstralCraft;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.util.Lazy;
import org.lwjgl.glfw.GLFW;

public class AstralKeyMappings {

    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(AstralCraft.prefix("category"));

    public static final Lazy<KeyMapping> CARD_BACK_SELECTION = Lazy.of(() -> new KeyMapping(
            "key.astral_craft.card_back_selection", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY));

    public static final Lazy<KeyMapping> CHARACTER_SETTINGS = Lazy.of(() -> new KeyMapping(
            "key.astral_craft.character_settings", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY));

    public static final Lazy<KeyMapping> HAND_CARD_DECK = Lazy.of(() -> new KeyMapping(
            "key.astral_craft.hand_card_deck", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY));

    public static final Lazy<KeyMapping> CHARACTER_SKILL = Lazy.of(() -> new KeyMapping(
            "key.astral_craft.character_skill", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY));

    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(CARD_BACK_SELECTION.get());
        event.register(CHARACTER_SETTINGS.get());
        event.register(HAND_CARD_DECK.get());
        event.register(CHARACTER_SKILL.get());
    }

}