package com.astral_craft.common.entity.character;

import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.CharacterManager;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

public class AstralCharacterEntity extends PathfinderMob {

    protected static final EntityDataAccessor<String> DATA_CHARACTER_ID = SynchedEntityData.defineId(AstralCharacterEntity.class, EntityDataSerializers.STRING);
    protected static final EntityDataAccessor<String> DATA_SKIN_ID = SynchedEntityData.defineId(AstralCharacterEntity.class, EntityDataSerializers.STRING);
    protected static final EntityDataAccessor<Integer> DATA_LEVEL = SynchedEntityData.defineId(AstralCharacterEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Integer> DATA_FRIENDSHIP = SynchedEntityData.defineId(AstralCharacterEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Integer> DATA_STAR_COINS = SynchedEntityData.defineId(AstralCharacterEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<String> DATA_ANIMATION_ACTION = SynchedEntityData.defineId(AstralCharacterEntity.class, EntityDataSerializers.STRING);

    public AstralCharacterEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        CharacterDefinition definition = CharacterManager.INSTANCE.defaultCharacter();
        this.setCharacterId(definition.id());
        this.setSkinId(definition.skins().getFirst().id());
        this.setCharacterLevel(1);
        this.setFriendship(1);
        this.setStarCoins(0);
        this.setAnimationAction("idle");
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        CharacterDefinition definition = CharacterManager.INSTANCE.defaultCharacter();
        builder.define(DATA_CHARACTER_ID, definition.id().toString());
        builder.define(DATA_SKIN_ID, definition.skins().getFirst().id());
        builder.define(DATA_LEVEL, 1);
        builder.define(DATA_FRIENDSHIP, 1);
        builder.define(DATA_STAR_COINS, 0);
        builder.define(DATA_ANIMATION_ACTION, "idle");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void registerGoals() {}

    public Identifier characterId() {
        try {
            return Identifier.parse(this.entityData.get(DATA_CHARACTER_ID));
        } catch (Exception exception) {
            return CharacterManager.INSTANCE.defaultCharacter().id();
        }
    }

    public void setCharacterId(Identifier characterId) {
        Identifier safeId = characterId == null ? CharacterManager.INSTANCE.defaultCharacter().id() : characterId;
        this.entityData.set(DATA_CHARACTER_ID, safeId.toString());
    }

    public String skinId() {
        return this.entityData.get(DATA_SKIN_ID);
    }

    public void setSkinId(String skinId) {
        this.entityData.set(DATA_SKIN_ID, skinId);
    }

    public int characterLevel() {
        return this.entityData.get(DATA_LEVEL);
    }

    public void setCharacterLevel(int level) {
        this.entityData.set(DATA_LEVEL, Math.max(1, level));
    }

    public int friendship() {
        return this.entityData.get(DATA_FRIENDSHIP);
    }

    public void setFriendship(int friendship) {
        this.entityData.set(DATA_FRIENDSHIP, Math.max(0, friendship));
    }

    public void addFriendship(int amount) {
        this.setFriendship(this.friendship() + amount);
    }

    public int starCoins() {
        return this.entityData.get(DATA_STAR_COINS);
    }

    public void setStarCoins(int amount) {
        this.entityData.set(DATA_STAR_COINS, Math.max(0, amount));
    }

    public void addStarCoins(int amount) {
        this.setStarCoins(this.starCoins() + amount);
    }

    public String animationAction() {
        String action = this.entityData.get(DATA_ANIMATION_ACTION);
        return action == null || action.isBlank() ? "idle" : action;
    }

    public void setAnimationAction(String action) {
        this.entityData.set(DATA_ANIMATION_ACTION, action == null || action.isBlank() ? "idle" : action);
    }

}
