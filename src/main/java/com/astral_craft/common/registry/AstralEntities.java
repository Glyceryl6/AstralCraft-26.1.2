package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.entity.AstralDiceEntity;
import com.astral_craft.common.entity.BoardMonsterZombieEntity;
import com.astral_craft.common.entity.BoardWorldObjectEntity;
import com.astral_craft.common.entity.CustomPaintingEntity;
import com.astral_craft.common.entity.SoulLinkEntity;
import com.astral_craft.common.entity.StarCoinEntity;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.entity.character.ExhibitionCharacterEntity;
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

    public static final Supplier<EntityType<BoardMonsterZombieEntity>> BOARD_MONSTER_ZOMBIE = ENTITIES.registerEntityType(
            "board_monster_zombie", BoardMonsterZombieEntity::new, MobCategory.MISC,
            builder -> builder.sized(0.6F, 1.95F).clientTrackingRange(64).updateInterval(1).noSave());

    public static final Supplier<EntityType<ExhibitionCharacterEntity>> EXHIBITION_CHARACTER = ENTITIES.registerEntityType(
            "exhibition_character", ExhibitionCharacterEntity::new, MobCategory.MISC,
            builder -> builder.sized(0.6F, 1.8F).clientTrackingRange(96).updateInterval(2));

    public static final Supplier<EntityType<AstralDiceEntity>> ASTRAL_DICE = ENTITIES.registerEntityType(
            "astral_dice", AstralDiceEntity::new, MobCategory.MISC,
            builder -> builder.sized(0.75F, 0.75F).clientTrackingRange(8).updateInterval(1).noSave());

    public static final Supplier<EntityType<SoulLinkEntity>> SOUL_LINK = ENTITIES.registerEntityType(
            "soul_link", SoulLinkEntity::new, MobCategory.MISC,
            builder -> builder.sized(0.1F, 0.1F).clientTrackingRange(128).updateInterval(1).noSave());

    public static final Supplier<EntityType<BoardWorldObjectEntity>> BOARD_WORLD_OBJECT = ENTITIES.registerEntityType(
            "board_world_object", BoardWorldObjectEntity::new, MobCategory.MISC,
            builder -> builder.sized(0.55F, 0.55F).clientTrackingRange(128).updateInterval(1).noSave());

    public static final Supplier<EntityType<CustomPaintingEntity>> CUSTOM_PAINTING = ENTITIES.registerEntityType(
            "custom_painting", CustomPaintingEntity::new, MobCategory.MISC,
            builder -> builder.sized(1.0F, 1.0F).clientTrackingRange(128).updateInterval(10));

    public static final Supplier<EntityType<StarCoinEntity>> STAR_COIN = ENTITIES.registerEntityType(
            "star_coin", StarCoinEntity::new, MobCategory.MISC,
            builder -> builder.sized(0.25F, 0.25F).clientTrackingRange(128).updateInterval(1).noSave());

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