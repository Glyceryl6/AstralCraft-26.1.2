package com.astral_craft.common.gameplay.event.type;

import com.astral_craft.AstralCraft;
import net.minecraft.resources.Identifier;

public class AstralEventTriggers {

    public static final Identifier ANY = AstralCraft.prefix("any");
    public static final Identifier MANUAL = AstralCraft.prefix("manual");
    public static final Identifier TICK = AstralCraft.prefix("tick");
    public static final Identifier BLOCK_BREAK = AstralCraft.prefix("block_break");
    public static final Identifier PLAYER_HURT = AstralCraft.prefix("player_hurt");
    public static final Identifier ENTITY_HURT_PLAYER = AstralCraft.prefix("entity_hurt_player");
    public static final Identifier PLAYER_HURT_ENTITY = AstralCraft.prefix("player_hurt_entity");
    public static final Identifier PLAYER_KILLED = AstralCraft.prefix("player_killed");
    public static final Identifier PLAYER_KILLED_ENTITY = AstralCraft.prefix("player_killed_entity");

    public static Identifier parse(String raw) {
        return AstralEventIdentifiers.parse(raw, MANUAL);
    }

    public static boolean matches(Identifier expected, Identifier actual) {
        return AstralEventIdentifiers.equals(expected, ANY) || AstralEventIdentifiers.equals(expected, actual);
    }

}
