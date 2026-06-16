package com.astral_craft.common.entity.character;

import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.CharacterManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

public class AstralCharacterEntity extends PathfinderMob {

    protected Identifier characterId;
    protected String skinId;
    protected int level;
    protected int friendship;
    protected int starCoins;

    public AstralCharacterEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        CharacterDefinition definition = CharacterManager.INSTANCE.defaultCharacter();
        this.characterId = definition.id();
        this.skinId = definition.skins().isEmpty() ? "default" : definition.skins().getFirst().id();
        this.level = 1;
        this.friendship = 0;
        this.starCoins = 0;
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
        return this.characterId;
    }

    public void setCharacterId(Identifier characterId) {
        this.characterId = characterId;
    }

    public String skinId() {
        return this.skinId;
    }

    public void setSkinId(String skinId) {
        this.skinId = skinId;
    }

    public int characterLevel() {
        return this.level;
    }

    public void setCharacterLevel(int level) {
        this.level = Math.max(1, level);
    }

    public int friendship() {
        return this.friendship;
    }

    public void addFriendship(int amount) {
        this.friendship = Math.max(0, this.friendship + amount);
    }

    public int starCoins() {
        return this.starCoins;
    }

    public void addStarCoins(int amount) {
        this.starCoins = Math.max(0, this.starCoins + amount);
    }

}