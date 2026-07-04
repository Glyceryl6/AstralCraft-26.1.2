package com.astral_craft.common.gameplay.event.type;

import com.astral_craft.AstralCraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;

public class AstralEventEntityCategories {

    public static final Identifier ANY = AstralCraft.prefix("any");
    public static final Identifier PLAYER = AstralCraft.prefix("player");
    public static final Identifier PLAYERS = AstralCraft.prefix("players");
    public static final Identifier LIVING = AstralCraft.prefix("living");
    public static final Identifier MOB = AstralCraft.prefix("mob");
    public static final Identifier MOBS = AstralCraft.prefix("mobs");
    public static final Identifier MONSTER = AstralCraft.prefix("monster");
    public static final Identifier MONSTERS = AstralCraft.prefix("monsters");
    public static final Identifier HOSTILE = AstralCraft.prefix("hostile");
    public static final Identifier ANIMAL = AstralCraft.prefix("animal");
    public static final Identifier ANIMALS = AstralCraft.prefix("animals");
    public static final Identifier PASSIVE = AstralCraft.prefix("passive");

    public static boolean matches(Entity entity, Identifier category) {
        if (category == null || AstralEventIdentifiers.equals(category, ANY)) return true;
        if (AstralEventIdentifiers.equals(category, PLAYER) || AstralEventIdentifiers.equals(category, PLAYERS)) return entity instanceof ServerPlayer;
        if (AstralEventIdentifiers.equals(category, LIVING)) return entity instanceof LivingEntity;
        if (AstralEventIdentifiers.equals(category, MOB) || AstralEventIdentifiers.equals(category, MOBS)) return entity instanceof Mob;
        if (AstralEventIdentifiers.equals(category, MONSTER) || AstralEventIdentifiers.equals(category, MONSTERS) || AstralEventIdentifiers.equals(category, HOSTILE)) return entity instanceof Monster;
        if (AstralEventIdentifiers.equals(category, ANIMAL) || AstralEventIdentifiers.equals(category, ANIMALS) || AstralEventIdentifiers.equals(category, PASSIVE)) return entity instanceof Animal;
        return false;
    }

}
