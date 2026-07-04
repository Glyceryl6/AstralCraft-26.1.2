package com.astral_craft.common.gameplay.event.type;

import com.astral_craft.AstralCraft;
import net.minecraft.resources.Identifier;

public class AstralEventKinds {

    public static final Identifier NEUTRAL = AstralCraft.prefix("neutral");
    public static final Identifier GOOD = AstralCraft.prefix("good");
    public static final Identifier BAD = AstralCraft.prefix("bad");

    public static boolean good(Identifier id) {
        return AstralEventIdentifiers.equals(id, GOOD);
    }

    public static boolean bad(Identifier id) {
        return AstralEventIdentifiers.equals(id, BAD);
    }

    public static String suffix(Identifier id) {
        if (good(id)) return "good";
        if (bad(id)) return "bad";
        return "neutral";
    }

    public static Identifier texture(Identifier id) {
        return AstralCraft.prefix("textures/gui/cards/event_" + suffix(id) + ".png");
    }

}
