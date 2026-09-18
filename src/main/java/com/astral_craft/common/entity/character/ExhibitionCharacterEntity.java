package com.astral_craft.common.entity.character;

import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.CharacterManager;
import com.astral_craft.common.gameplay.character.skin.CharacterSkinDefinition;
import com.astral_craft.common.network.s2c.OpenExhibitionCharacterConfigPayload;
import com.astral_craft.common.registry.AstralItems;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.NonNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

/**
 * Persistent display-only character used by exhibition builds. It deliberately stays separate from
 * board pawns so board lifecycle, combat and card rules never own this entity.
 */
public class ExhibitionCharacterEntity extends AstralCharacterEntity {

    public static final float MIN_SCALE = 0.25F;
    public static final float MAX_SCALE = 4.0F;
    public static final int MAX_SPEECH_LENGTH = 160;
    public static final int MAX_SPEECH_IMAGE_SOURCE_LENGTH = 256;
    public static final int MAX_CUSTOM_NAME_LENGTH = 64;
    public static final int MAX_CUSTOM_SKIN_SOURCE_LENGTH = 256;
    private static final double LOOK_DISTANCE = 10.0D;
    private static final double LOOK_MIN_DOT = 0.84D;
    private static final float HEAD_TURN_SPEED = 10.0F;
    private static final float BODY_TURN_SPEED = 4.0F;
    private static final float PITCH_TURN_SPEED = 7.0F;
    private static final float BODY_FOLLOW_THRESHOLD = 50.0F;
    private static final float MAX_HEAD_BODY_ANGLE = 75.0F;
    private static final EntityDataAccessor<Float> DATA_EXHIBITION_YAW = SynchedEntityData.defineId(ExhibitionCharacterEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> DATA_SPEECH_TEXT = SynchedEntityData.defineId(ExhibitionCharacterEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_SPEECH_IMAGE = SynchedEntityData.defineId(ExhibitionCharacterEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_FACE_LOOKING_PLAYER = SynchedEntityData.defineId(ExhibitionCharacterEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_CUSTOM_SKIN_ENABLED = SynchedEntityData.defineId(ExhibitionCharacterEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_CUSTOM_SKIN_PLAYER = SynchedEntityData.defineId(ExhibitionCharacterEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_CUSTOM_SKIN_SOURCE = SynchedEntityData.defineId(ExhibitionCharacterEntity.class, EntityDataSerializers.STRING);
    private boolean clientPreviewPositionActive;
    private double clientPreviewX;
    private double clientPreviewY;
    private double clientPreviewZ;

    public ExhibitionCharacterEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.applyDisplayInvariants();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_EXHIBITION_YAW, 0.0F);
        builder.define(DATA_SPEECH_TEXT, "");
        builder.define(DATA_SPEECH_IMAGE, "");
        builder.define(DATA_FACE_LOOKING_PLAYER, false);
        builder.define(DATA_CUSTOM_SKIN_ENABLED, false);
        builder.define(DATA_CUSTOM_SKIN_PLAYER, true);
        builder.define(DATA_CUSTOM_SKIN_SOURCE, "");
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            if (this.facesLookingPlayer()) this.tickPlayerFacing();
            else this.applyDisplayRotation();
        } else if (!this.facesLookingPlayer()) {
            this.applyDisplayRotation();
        }
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        if (!this.isNoAi() || !this.isInvulnerable() || !this.isNoGravity()) this.applyDisplayInvariants();
        if (this.level().isClientSide() && this.clientPreviewPositionActive) this.applyClientPreviewPosition();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if ((DATA_EXHIBITION_YAW.equals(accessor) || DATA_FACE_LOOKING_PLAYER.equals(accessor)) && !this.facesLookingPlayer()) this.applyDisplayRotation();
    }

    @Override
    @ParametersAreNonnullByDefault
    protected @NonNull InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.canPlayerConfigure(player)) return InteractionResult.FAIL;
        if (!this.level().isClientSide() && player instanceof ServerPlayer serverPlayer) this.openConfiguration(serverPlayer);
        return InteractionResult.SUCCESS;
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean skipAttackInteraction(Entity source) {
        return true;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean isPushedByFluid(FluidType type) {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {}

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return false;
    }

    public boolean applyConfiguration(Identifier characterId, String skinId, double x, double y, double z, float yaw, float scale,
                                      String customName, boolean showName, String speechText, String speechImage, boolean faceLookingPlayer,
                                      boolean customSkinEnabled, boolean customSkinPlayer, String customSkinSource) {
        if (!CharacterManager.INSTANCE.contains(characterId) || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !Float.isFinite(yaw) || !Float.isFinite(scale) || (customName != null && customName.length() > MAX_CUSTOM_NAME_LENGTH)
                || (speechText != null && speechText.length() > MAX_SPEECH_LENGTH) || !validSpeechImageSource(speechImage)) return false;
        if (customSkinEnabled && !validCustomSkinSource(customSkinPlayer, customSkinSource)) return false;
        CharacterDefinition definition = CharacterManager.INSTANCE.get(characterId);
        CharacterSkinDefinition skin = definition.skins().stream().filter(value -> value.id().equals(skinId)).findFirst().orElse(null);
        if (skin == null) return false;
        this.setCharacterId(characterId);
        this.setSkinId(skin.id());
        this.setPos(x, y, z);
        this.setExhibitionYaw(yaw);
        this.setDisplayScale(scale);
        this.setDisplayCustomName(customName, showName);
        this.setSpeechText(speechText);
        this.setSpeechImageSource(speechImage);
        this.setFacesLookingPlayer(faceLookingPlayer);
        this.setCustomSkinPlayer(customSkinPlayer);
        this.setCustomSkinSource(customSkinSource);
        this.setCustomSkinEnabled(customSkinEnabled);
        this.applyDisplayInvariants();
        return true;
    }

    public void openConfiguration(ServerPlayer player) {
        if (!this.canPlayerConfigure(player)) return;
        PacketDistributor.sendToPlayer(player, new OpenExhibitionCharacterConfigPayload(
                this.getId(), CharacterManager.INSTANCE.values(), this.characterId(), this.skinId(), this.getX(), this.getY(), this.getZ(),
                this.displayYaw(), this.displayScale(), this.displayCustomName(), this.isCustomNameVisible(), this.speechText(),
                this.speechImageSource(), this.facesLookingPlayer(), this.customSkinEnabled(), this.customSkinPlayer(), this.customSkinSource()));
    }

    public boolean canPlayerConfigure(Player player) {
        if (player == null || player.distanceToSqr(this) > 64.0D) return false;
        return this.isConfigurationTool(player.getMainHandItem()) || this.isConfigurationTool(player.getOffhandItem());
    }

    public void setClientPreviewPosition(double x, double y, double z) {
        if (!this.level().isClientSide() || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return;
        this.clientPreviewPositionActive = true;
        this.clientPreviewX = x;
        this.clientPreviewY = y;
        this.clientPreviewZ = z;
        this.applyClientPreviewPosition();
    }

    public void clearClientPreviewPosition() {
        this.clientPreviewPositionActive = false;
    }

    private void applyClientPreviewPosition() {
        this.setPos(this.clientPreviewX, this.clientPreviewY, this.clientPreviewZ);
        this.setOldPosAndRot();
    }

    public float displayScale() {
        return Mth.clamp(this.getScale(), MIN_SCALE, MAX_SCALE);
    }

    public void setDisplayScale(float scale) {
        AttributeInstance attribute = this.getAttribute(Attributes.SCALE);
        if (attribute == null) return;
        attribute.setBaseValue(Mth.clamp(scale, MIN_SCALE, MAX_SCALE));
        this.refreshDimensions();
    }

    public float displayYaw() {
        return normalizeYaw(this.entityData.get(DATA_EXHIBITION_YAW));
    }

    public void setExhibitionYaw(float yaw) {
        this.entityData.set(DATA_EXHIBITION_YAW, normalizeYaw(yaw));
        if (!this.facesLookingPlayer()) this.applyDisplayRotation();
    }

    public static float normalizeYaw(float yaw) {
        return Mth.positiveModulo(yaw, 360.0F);
    }

    public String displayCustomName() {
        Component name = this.getCustomName();
        return name == null ? "" : name.getString();
    }

    public void setDisplayCustomName(String customName, boolean showName) {
        String safeName = customName == null ? "" : customName.strip();
        if (safeName.length() > MAX_CUSTOM_NAME_LENGTH) safeName = safeName.substring(0, MAX_CUSTOM_NAME_LENGTH);
        this.setCustomName(safeName.isBlank() ? null : Component.literal(safeName));
        this.setCustomNameVisible(showName && !safeName.isBlank());
    }

    public String speechText() {
        return this.entityData.get(DATA_SPEECH_TEXT);
    }

    public void setSpeechText(String speechText) {
        String safeText = speechText == null ? "" : speechText.strip();
        if (safeText.length() > MAX_SPEECH_LENGTH) safeText = safeText.substring(0, MAX_SPEECH_LENGTH);
        this.entityData.set(DATA_SPEECH_TEXT, safeText);
    }

    public String speechImageSource() {
        return this.entityData.get(DATA_SPEECH_IMAGE);
    }

    public Identifier speechImage() {
        return Identifier.tryParse(this.speechImageSource());
    }

    public void setSpeechImageSource(String source) {
        String safeSource = source == null ? "" : source.strip();
        if (safeSource.length() > MAX_SPEECH_IMAGE_SOURCE_LENGTH) safeSource = safeSource.substring(0, MAX_SPEECH_IMAGE_SOURCE_LENGTH);
        this.entityData.set(DATA_SPEECH_IMAGE, validSpeechImageSource(safeSource) ? safeSource : "");
    }

    public static boolean validSpeechImageSource(String source) {
        String safeSource = source == null ? "" : source.strip();
        return safeSource.isEmpty() || safeSource.length() <= MAX_SPEECH_IMAGE_SOURCE_LENGTH && Identifier.tryParse(safeSource) != null;
    }

    public boolean facesLookingPlayer() {
        return this.entityData.get(DATA_FACE_LOOKING_PLAYER);
    }

    public void setFacesLookingPlayer(boolean enabled) {
        this.entityData.set(DATA_FACE_LOOKING_PLAYER, enabled);
        if (!enabled) this.applyDisplayRotation();
    }

    public boolean customSkinEnabled() {
        return this.entityData.get(DATA_CUSTOM_SKIN_ENABLED);
    }

    public void setCustomSkinEnabled(boolean enabled) {
        this.entityData.set(DATA_CUSTOM_SKIN_ENABLED, enabled);
    }

    public boolean customSkinPlayer() {
        return this.entityData.get(DATA_CUSTOM_SKIN_PLAYER);
    }

    public void setCustomSkinPlayer(boolean playerSkin) {
        this.entityData.set(DATA_CUSTOM_SKIN_PLAYER, playerSkin);
    }

    public String customSkinSource() {
        return this.entityData.get(DATA_CUSTOM_SKIN_SOURCE);
    }

    public void setCustomSkinSource(String source) {
        String safeSource = source == null ? "" : source.strip();
        if (safeSource.length() > MAX_CUSTOM_SKIN_SOURCE_LENGTH) safeSource = safeSource.substring(0, MAX_CUSTOM_SKIN_SOURCE_LENGTH);
        this.entityData.set(DATA_CUSTOM_SKIN_SOURCE, safeSource);
    }

    public static boolean validCustomSkinSource(boolean playerSkin, String source) {
        String safeSource = source == null ? "" : source.strip();
        if (safeSource.isEmpty() || safeSource.length() > MAX_CUSTOM_SKIN_SOURCE_LENGTH) return false;
        return playerSkin ? validCustomPlayerSource(safeSource) : Identifier.tryParse(safeSource) != null;
    }

    public static boolean validCustomPlayerSource(String source) {
        String safeSource = source == null ? "" : source.strip();
        if (safeSource.isEmpty()) return false;
        if (parseCustomPlayerUuid(safeSource) != null) return true;
        if (safeSource.length() > 16) return false;
        for (int index = 0; index < safeSource.length(); index++) {
            char character = safeSource.charAt(index);
            if (!Character.isLetterOrDigit(character) && character != '_') return false;
        }
        return true;
    }

    public static UUID parseCustomPlayerUuid(String source) {
        String safeSource = source == null ? "" : source.strip();
        try {
            return UUID.fromString(safeSource);
        } catch (IllegalArgumentException ignored) {}
        if (safeSource.length() != 32) return null;
        String dashed = safeSource.substring(0, 8) + "-" + safeSource.substring(8, 12) + "-" + safeSource.substring(12, 16) + "-"
                + safeSource.substring(16, 20) + "-" + safeSource.substring(20);
        try {
            return UUID.fromString(dashed);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setSpeechText(input.getStringOr("exhibition_speech", ""));
        this.setSpeechImageSource(input.getStringOr("exhibition_speech_image", ""));
        this.setFacesLookingPlayer(input.getBooleanOr("exhibition_face_looking_player", false));
        this.setCustomSkinPlayer(input.getBooleanOr("exhibition_custom_skin_player", true));
        this.setCustomSkinSource(input.getStringOr("exhibition_custom_skin_source", ""));
        this.setCustomSkinEnabled(input.getBooleanOr("exhibition_custom_skin_enabled", false)
                && validCustomSkinSource(this.customSkinPlayer(), this.customSkinSource()));
        this.setExhibitionYaw(input.getFloatOr("exhibition_yaw", this.getYRot()));
        this.applyDisplayInvariants();
        this.refreshDimensions();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putFloat("exhibition_yaw", this.displayYaw());
        output.putString("exhibition_speech", this.speechText());
        output.putString("exhibition_speech_image", this.speechImageSource());
        output.putBoolean("exhibition_face_looking_player", this.facesLookingPlayer());
        output.putBoolean("exhibition_custom_skin_enabled", this.customSkinEnabled());
        output.putBoolean("exhibition_custom_skin_player", this.customSkinPlayer());
        output.putString("exhibition_custom_skin_source", this.customSkinSource());
    }

    private void applyDisplayRotation() {
        float yaw = this.displayYaw();
        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        this.setYHeadRot(yaw);
        this.setXRot(0.0F);
        this.yRotO = yaw;
        this.yBodyRotO = yaw;
        this.yHeadRotO = yaw;
        this.xRotO = 0.0F;
    }

    private void tickPlayerFacing() {
        Player target = this.lookingPlayer();
        if (target == null) {
            this.returnToDisplayRotation();
            return;
        }
        Vec3 targetEyes = target.getEyePosition();
        Vec3 eyes = this.getEyePosition();
        double dx = targetEyes.x - eyes.x;
        double dy = targetEyes.y - eyes.y;
        double dz = targetEyes.z - eyes.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) (Mth.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
        float targetPitch = Mth.clamp((float) (-(Mth.atan2(dy, horizontal) * 180.0D / Math.PI)), -35.0F, 35.0F);
        float bodyYaw = this.yBodyRot;
        float headYaw = Mth.approachDegrees(this.getYHeadRot(), targetYaw, HEAD_TURN_SPEED);
        if (Math.abs(Mth.wrapDegrees(headYaw - bodyYaw)) > BODY_FOLLOW_THRESHOLD) {
            bodyYaw = Mth.approachDegrees(bodyYaw, targetYaw, BODY_TURN_SPEED);
        }
        headYaw = bodyYaw + Mth.clamp(Mth.wrapDegrees(headYaw - bodyYaw), -MAX_HEAD_BODY_ANGLE, MAX_HEAD_BODY_ANGLE);
        this.setYRot(bodyYaw);
        this.setYBodyRot(bodyYaw);
        this.setYHeadRot(headYaw);
        this.setXRot(Mth.approachDegrees(this.getXRot(), targetPitch, PITCH_TURN_SPEED));
    }

    private Player lookingPlayer() {
        Player result = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Player player : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(LOOK_DISTANCE))) {
            if (player.isSpectator()) continue;
            Vec3 towardEntity = this.getEyePosition().subtract(player.getEyePosition());
            double distance = towardEntity.length();
            if (distance <= 0.001D || distance > LOOK_DISTANCE || player.getLookAngle().dot(towardEntity.scale(1.0D / distance)) < LOOK_MIN_DOT
                    || !this.hasLineOfSight(player)) continue;
            double distanceSqr = this.distanceToSqr(player);
            if (distanceSqr >= nearestDistance) continue;
            nearestDistance = distanceSqr;
            result = player;
        }
        return result;
    }

    private void returnToDisplayRotation() {
        float bodyYaw = Mth.approachDegrees(this.yBodyRot, this.displayYaw(), BODY_TURN_SPEED);
        float headYaw = Mth.approachDegrees(this.getYHeadRot(), bodyYaw, HEAD_TURN_SPEED);
        this.setYRot(bodyYaw);
        this.setYBodyRot(bodyYaw);
        this.setYHeadRot(headYaw);
        this.setXRot(Mth.approachDegrees(this.getXRot(), 0.0F, PITCH_TURN_SPEED));
    }

    private void applyDisplayInvariants() {
        this.setNoAi(true);
        this.setInvulnerable(true);
        this.setNoGravity(true);
        this.setSilent(true);
        this.setPersistenceRequired();
    }

    private boolean isConfigurationTool(ItemStack stack) {
        return stack.is(AstralItems.EXHIBITION_CHARACTER_TOOL.get());
    }
}
