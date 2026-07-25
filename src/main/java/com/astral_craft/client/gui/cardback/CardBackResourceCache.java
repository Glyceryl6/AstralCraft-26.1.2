package com.astral_craft.client.gui.cardback;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Scans card-back resources once for the lifetime of the running client. */
public class CardBackResourceCache {

    private static final String DIRECTORY = "textures/gui/cards/back";
    private static List<CardBackDefinition> cached;

    public static synchronized List<CardBackDefinition> values() {
        if (cached != null) return cached;
        Map<Identifier, CardBackDefinition> definitions = new LinkedHashMap<>();
        CardBackDefinition fallback = CardBackDefinition.builtinDefault();
        definitions.put(fallback.texture(), fallback);
        Minecraft.getInstance().getResourceManager().listResources(DIRECTORY,
                identifier -> identifier.getPath().endsWith(".png")).keySet().forEach(texture ->
                definitions.putIfAbsent(texture, CardBackDefinition.scanned(texture)));
        cached = List.copyOf(definitions.values());
        return cached;
    }
}
