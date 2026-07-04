package com.astral_craft.common.gameplay.event.type;

import com.astral_craft.AstralCraft;
import net.minecraft.resources.Identifier;

public class AstralEventTimings {

    public static final Identifier INSTANT = AstralCraft.prefix("instant");
    public static final Identifier DURATION = AstralCraft.prefix("duration");

    public static boolean duration(Identifier id) {
        return AstralEventIdentifiers.equals(id, DURATION);
    }

}