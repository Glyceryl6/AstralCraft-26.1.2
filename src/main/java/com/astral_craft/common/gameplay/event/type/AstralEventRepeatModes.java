package com.astral_craft.common.gameplay.event.type;

import com.astral_craft.AstralCraft;
import net.minecraft.resources.Identifier;

public class AstralEventRepeatModes {

    public static final Identifier COOLDOWN = AstralCraft.prefix("cooldown");
    public static final Identifier ALWAYS = AstralCraft.prefix("always");
    public static final Identifier ONCE = AstralCraft.prefix("once");
    public static final Identifier ONCE_PER_TARGET = AstralCraft.prefix("once_per_target");
    public static final Identifier ONCE_PER_PLAYER = AstralCraft.prefix("once_per_player");
    public static final Identifier WHILE_INACTIVE = AstralCraft.prefix("while_inactive");

    public static boolean isAlways(Identifier id) {
        return AstralEventIdentifiers.equals(id, ALWAYS);
    }

    public static boolean isOncePerTarget(Identifier id) {
        return AstralEventIdentifiers.equals(id, ONCE) || AstralEventIdentifiers.equals(id, ONCE_PER_TARGET);
    }

    public static boolean isOncePerPlayer(Identifier id) {
        return AstralEventIdentifiers.equals(id, ONCE_PER_PLAYER);
    }

    public static boolean isWhileInactive(Identifier id) {
        return AstralEventIdentifiers.equals(id, WHILE_INACTIVE);
    }

}
