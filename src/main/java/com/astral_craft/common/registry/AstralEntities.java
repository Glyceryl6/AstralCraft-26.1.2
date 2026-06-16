package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.entity.AstralDiceEntity;
import com.astral_craft.common.entity.SoulLinkEntity;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.entity.projectile.FirecrackersProjectileEntity;
import com.astral_craft.common.entity.projectile.SlingshotProjectileEntity;
import com.astral_craft.common.entity.projectile.SnowballAttackProjectileEntity;
import com.astral_craft.common.entity.visual.FallingBrickEntity;
import com.astral_craft.common.entity.visual.LaserStrikeEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class AstralEntities {

    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(AstralCraft.MOD_ID);

    public static final Supplier<EntityType<AstralCharacterEntity>> ASTRAL_CHARACTER = ENTITIES.registerEntityType(
            "astral_character", AstralCharacterEntity::new, MobCategory.CREATURE,
            builder -> builder.sized(0.6F, 1.8F).clientTrackingRange(64).updateInterval(2));

    public static final Supplier<EntityType<AstralDiceEntity>> ASTRAL_DICE = ENTITIES.registerEntityType(
            "astral_dice", AstralDiceEntity::new, MobCategory.MISC,
            builder -> builder.sized(0.75F, 0.75F).clientTrackingRange(8).updateInterval(1).noSave());

    public static final Supplier<EntityType<SoulLinkEntity>> SOUL_LINK = ENTITIES.registerEntityType(
            "soul_link", SoulLinkEntity::new, MobCategory.MISC,
            builder -> builder.sized(0.1F, 0.1F).clientTrackingRange(128).updateInterval(1).noSave());

    public static final Supplier<EntityType<LaserStrikeEntity>> LASER_STRIKE = ENTITIES.registerEntityType(
            "laser_strike", LaserStrikeEntity::new, MobCategory.MISC,
            builder -> builder.sized(0.1F, 0.1F).clientTrackingRange(96).updateInterval(1).noSave());

    public static final Supplier<EntityType<FirecrackersProjectileEntity>> FIRECRACKERS_PROJECTILE = ENTITIES.registerEntityType(
            "firecrackers_projectile", FirecrackersProjectileEntity::new, MobCategory.MISC,
            builder -> builder.sized(0.35F, 0.35F).clientTrackingRange(96).updateInterval(1).noSave());

    public static final Supplier<EntityType<SlingshotProjectileEntity>> SLINGSHOT_PROJECTILE = ENTITIES.registerEntityType(
            "slingshot_projectile", SlingshotProjectileEntity::new, MobCategory.MISC,
            builder -> builder.sized(0.22F, 0.22F).clientTrackingRange(96).updateInterval(1).noSave());

    public static final Supplier<EntityType<SnowballAttackProjectileEntity>> SNOWBALL_ATTACK_PROJECTILE = ENTITIES.registerEntityType(
            "snowball_attack_projectile", SnowballAttackProjectileEntity::new, MobCategory.MISC,
            builder -> builder.sized(0.25F, 0.25F).clientTrackingRange(96).updateInterval(1).noSave());

    public static final Supplier<EntityType<FallingBrickEntity>> FALLING_BRICK = ENTITIES.registerEntityType(
            "falling_brick", FallingBrickEntity::new, MobCategory.MISC,
            builder -> builder.sized(0.45F, 0.45F).clientTrackingRange(96).updateInterval(1).noSave());

}
