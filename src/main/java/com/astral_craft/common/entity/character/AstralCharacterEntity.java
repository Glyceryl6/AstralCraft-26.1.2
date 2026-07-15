package com.astral_craft.common.entity.character;

import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.CharacterManager;
import com.astral_craft.common.stats.AstralPlayerStats;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Optional;
import java.util.UUID;

/**
 * Visual and logical pawn used by persistent board matches. The entity never navigates by itself;
 * movement is driven by {@link BoardSessionManager} one panel at a time.
 */
public class AstralCharacterEntity extends PathfinderMob {

    private static final byte BOARD_DAMAGE_FLASH_EVENT = 61;

    protected static final EntityDataAccessor<String> DATA_CHARACTER_ID = SynchedEntityData.defineId(AstralCharacterEntity.class, EntityDataSerializers.STRING);
    protected static final EntityDataAccessor<String> DATA_SKIN_ID = SynchedEntityData.defineId(AstralCharacterEntity.class, EntityDataSerializers.STRING);
    protected static final EntityDataAccessor<Integer> DATA_LEVEL = SynchedEntityData.defineId(AstralCharacterEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Integer> DATA_FRIENDSHIP = SynchedEntityData.defineId(AstralCharacterEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Integer> DATA_STAR_COINS = SynchedEntityData.defineId(AstralCharacterEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<String> DATA_ANIMATION_ACTION = SynchedEntityData.defineId(AstralCharacterEntity.class, EntityDataSerializers.STRING);
    protected static final EntityDataAccessor<Integer> DATA_ANIMATION_STARTED_TICK = SynchedEntityData.defineId(AstralCharacterEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<String> DATA_BOARD_SESSION_ID = SynchedEntityData.defineId(AstralCharacterEntity.class, EntityDataSerializers.STRING);
    protected static final EntityDataAccessor<String> DATA_BOARD_PARTICIPANT_ID = SynchedEntityData.defineId(AstralCharacterEntity.class, EntityDataSerializers.STRING);
    protected static final EntityDataAccessor<Integer> DATA_BOARD_DIRECTION = SynchedEntityData.defineId(AstralCharacterEntity.class, EntityDataSerializers.INT);
    private int boardReactionTicks;

    public AstralCharacterEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        CharacterDefinition definition = CharacterManager.INSTANCE.defaultCharacter();
        this.setCharacterId(definition.id());
        this.setSkinId(definition.skins().getFirst().id());
        this.setCharacterLevel(1);
        this.setFriendship(1);
        this.setStarCoins(0);
        this.setAnimationAction("idle");
        this.setPersistenceRequired();
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
        builder.define(DATA_ANIMATION_STARTED_TICK, 0);
        builder.define(DATA_BOARD_SESSION_ID, "");
        builder.define(DATA_BOARD_PARTICIPANT_ID, "");
        builder.define(DATA_BOARD_DIRECTION, 0);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {}

    @Override
    public void tick() {
        super.tick();
        if (!this.isBoardPawn()) return;
        if (!this.level().isClientSide() && !BoardSessionManager.isValidPawn(this)) {
            this.discard();
            return;
        }
        if (!this.level().isClientSide() && this.boardReactionTicks > 0) {
            this.boardReactionTicks--;
            if (this.boardReactionTicks == 0
                    && ("hurt".equals(this.animationAction()) || "attack".equals(this.animationAction()))) {
                this.setAnimationAction("idle");
            }
        }
        this.getNavigation().stop();
        this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
        this.applyBoardRotation(this.boardDirection());
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide() && player instanceof ServerPlayer serverPlayer && this.isBoardPawn()) {
            return BoardSessionManager.openTurnScreen(serverPlayer, this)
                    ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        return this.isBoardPawn() ? InteractionResult.SUCCESS : super.mobInteract(player, hand);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (!this.isBoardPawn()) return super.hurtServer(level, source, amount);
        AstralPlayerStats current = BoardSessionManager.statsForEntity(this, AstralPlayerStats.DEFAULT);
        int damage = Math.max(1, (int) Math.ceil(amount));
        AstralPlayerStats next = current.withHealth(Math.max(0, current.health() - damage));
        if (!BoardSessionManager.setStatsForEntity(this, next)) return false;
        BoardSessionManager.onParticipantDamaged(this, next);
        return true;
    }

    @Override
    public boolean isPushable() {
        return !this.isBoardPawn() && super.isPushable();
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {
        if (!this.isBoardPawn()) super.doPush(entity);
    }

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

    public String skinId() { return this.entityData.get(DATA_SKIN_ID); }
    public void setSkinId(String skinId) { this.entityData.set(DATA_SKIN_ID, skinId == null || skinId.isBlank() ? "default" : skinId); }
    public int characterLevel() { return this.entityData.get(DATA_LEVEL); }
    public void setCharacterLevel(int level) { this.entityData.set(DATA_LEVEL, Math.max(1, level)); }
    public int friendship() { return this.entityData.get(DATA_FRIENDSHIP); }
    public void setFriendship(int friendship) { this.entityData.set(DATA_FRIENDSHIP, Math.max(0, friendship)); }
    public void addFriendship(int amount) { this.setFriendship(this.friendship() + amount); }
    public int starCoins() { return this.entityData.get(DATA_STAR_COINS); }
    public void setStarCoins(int amount) { this.entityData.set(DATA_STAR_COINS, Math.max(0, amount)); }
    public void addStarCoins(int amount) { this.setStarCoins(this.starCoins() + amount); }

    public String animationAction() {
        String action = this.entityData.get(DATA_ANIMATION_ACTION);
        return action.isBlank() ? "idle" : action;
    }

    public void setAnimationAction(String action) {
        String safeAction = action == null || action.isBlank() ? "idle" : action;
        if (!safeAction.equals(this.animationAction())) {
            this.entityData.set(DATA_ANIMATION_STARTED_TICK, this.tickCount);
        }
        this.entityData.set(DATA_ANIMATION_ACTION, safeAction);
    }

    private void restartAnimationAction(String action) {
        String safeAction = action == null || action.isBlank() ? "idle" : action;
        this.entityData.set(DATA_ANIMATION_STARTED_TICK, this.tickCount);
        this.entityData.set(DATA_ANIMATION_ACTION, safeAction);
    }

    public int animationStartedTick() {
        return this.entityData.get(DATA_ANIMATION_STARTED_TICK);
    }

    public float animationAgeTicks(float partialTick) {
        return Math.max(0.0F, this.tickCount + partialTick - this.animationStartedTick());
    }

    public void playBoardHurtAnimation(int ticks) {
        if (!this.isBoardPawn()) return;
        int duration = Math.max(1, ticks);
        this.boardReactionTicks = Math.max(this.boardReactionTicks, duration);
        this.hurtDuration = duration;
        this.hurtTime = duration;
        this.restartAnimationAction("hurt");
        if (!this.level().isClientSide()) this.level().broadcastEntityEvent(this, (byte) 2);
    }

    public void flashBoardDamage(int ticks) {
        if (!this.isBoardPawn()) return;
        int duration = Math.max(1, ticks);
        this.hurtDuration = duration;
        this.hurtTime = duration;
        if (!this.level().isClientSide()) this.level().broadcastEntityEvent(this, BOARD_DAMAGE_FLASH_EVENT);
    }

    public void playBoardAttackAnimation(int ticks) {
        if (!this.isBoardPawn()) return;
        this.boardReactionTicks = Math.max(this.boardReactionTicks, Math.max(1, ticks));
        this.restartAnimationAction("attack");
    }

    @Override
    public void handleEntityEvent(byte id) {
        super.handleEntityEvent(id);
        if (id == 2 && this.isBoardPawn()) {
            this.hurtDuration = Math.max(this.hurtDuration, 10);
            this.hurtTime = Math.max(this.hurtTime, 10);
            this.boardReactionTicks = Math.max(this.boardReactionTicks, 10);
            this.restartAnimationAction("hurt");
        } else if (id == BOARD_DAMAGE_FLASH_EVENT && this.isBoardPawn()) {
            this.hurtDuration = Math.max(this.hurtDuration, 10);
            this.hurtTime = Math.max(this.hurtTime, 10);
        }
    }

    public boolean isBoardPawn() { return !this.entityData.get(DATA_BOARD_SESSION_ID).isBlank(); }
    public Optional<UUID> boardSessionUuid() { return parseUuid(this.entityData.get(DATA_BOARD_SESSION_ID)); }
    public Optional<UUID> boardParticipantUuid() { return parseUuid(this.entityData.get(DATA_BOARD_PARTICIPANT_ID)); }
    public void setBoardSessionId(UUID id) { this.entityData.set(DATA_BOARD_SESSION_ID, id == null ? "" : id.toString()); }
    public void setBoardParticipantId(UUID id) { this.entityData.set(DATA_BOARD_PARTICIPANT_ID, id == null ? "" : id.toString()); }
    public Direction boardDirection() { return Direction.from2DDataValue(this.boardDirectionIndex()); }
    public int boardDirectionIndex() { return Math.floorMod(this.entityData.get(DATA_BOARD_DIRECTION), 4); }
    public void setBoardDirection(int direction) {
        Direction boardDirection = Direction.from2DDataValue(Math.floorMod(direction, 4));
        this.entityData.set(DATA_BOARD_DIRECTION, boardDirection.get2DDataValue());
        this.applyBoardRotation(boardDirection);
    }

    public void setBoardDirection(Direction direction) {
        if (direction == null || !direction.getAxis().isHorizontal()) return;
        this.entityData.set(DATA_BOARD_DIRECTION, direction.get2DDataValue());
        this.applyBoardRotation(direction);
    }

    private void applyBoardRotation(Direction direction) {
        float yaw = direction.toYRot();
        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        this.setYHeadRot(yaw);
        this.yRotO = yaw;
        this.yBodyRotO = yaw;
        this.yHeadRotO = yaw;
    }

    private static Optional<UUID> parseUuid(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        try { return Optional.of(UUID.fromString(raw)); } catch (Exception ignored) { return Optional.empty(); }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setCharacterId(parseIdentifier(input.getStringOr("character_id", "")));
        this.setSkinId(input.getStringOr("skin_id", "default"));
        this.setCharacterLevel(input.getIntOr("character_level", 1));
        this.setFriendship(input.getIntOr("friendship", 1));
        this.setStarCoins(input.getIntOr("star_coins", 0));
        String savedAction = input.getStringOr("animation_action", "idle");
        this.setAnimationAction(("hurt".equals(savedAction) || "attack".equals(savedAction)) ? "idle" : savedAction);
        this.entityData.set(DATA_ANIMATION_STARTED_TICK, this.tickCount);
        this.entityData.set(DATA_BOARD_SESSION_ID, input.getStringOr("board_session_id", ""));
        this.entityData.set(DATA_BOARD_PARTICIPANT_ID, input.getStringOr("board_participant_id", ""));
        this.setBoardDirection(input.getIntOr("board_direction", 0));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("character_id", this.characterId().toString());
        output.putString("skin_id", this.skinId());
        output.putInt("character_level", this.characterLevel());
        output.putInt("friendship", this.friendship());
        output.putInt("star_coins", this.starCoins());
        output.putString("animation_action", this.animationAction());
        output.putString("board_session_id", this.entityData.get(DATA_BOARD_SESSION_ID));
        output.putString("board_participant_id", this.entityData.get(DATA_BOARD_PARTICIPANT_ID));
        output.putInt("board_direction", this.boardDirectionIndex());
    }

    private static Identifier parseIdentifier(String raw) {
        try { return Identifier.parse(raw); } catch (Exception ignored) { return CharacterManager.INSTANCE.defaultCharacter().id(); }
    }

}