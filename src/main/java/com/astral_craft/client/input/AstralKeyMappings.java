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

    public static final Lazy<KeyMapping> QUICK_PHRASES = Lazy.of(() -> new KeyMapping("key.astral_craft.quick_phrases",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY));

    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(QUICK_PHRASES.get());
    }

}