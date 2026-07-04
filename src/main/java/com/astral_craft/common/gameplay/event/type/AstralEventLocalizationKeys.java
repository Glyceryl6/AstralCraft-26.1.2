package com.astral_craft.common.gameplay.event.type;

import net.minecraft.resources.Identifier;

import java.util.Locale;

public class AstralEventLocalizationKeys {

    public static String name(Identifier id) {
        return base(id) + ".name";
    }

    public static String description(Identifier id) {
        return base(id) + ".description";
    }

    public static String normalizeName(Identifier id, String key) {
        String expected = name(id);
        return expected.equals(key) ? key : expected;
    }

    public static String normalizeDescription(Identifier id, String key) {
        String expected = description(id);
        return expected.equals(key) ? key : expected;
    }

    private static String base(Identifier id) {
        Identifier safeId = id == null ? AstralEventIdentifiers.parse("unknown_event", null) : id;
        return "event." + safeId.getNamespace().toLowerCase(Locale.ROOT) + "." + safeId.getPath().replace('/', '.').toLowerCase(Locale.ROOT);
    }

}
