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
    public static final int MAX_CUSTOM_NAME_LENGTH = 64;
    public static final int MAX_CUSTOM_SKIN_SOURCE_LENGTH = 256;
    private static final EntityDataAccessor<String> DATA_SPEECH_TEXT = SynchedEntityData.defineId(ExhibitionCharacterEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_CUSTOM_SKIN_ENABLED = SynchedEntityData.defineId(ExhibitionCharacterEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_CUSTOM_SKIN_PLAYER = SynchedEntityData.defineId(ExhibitionCharacterEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_CUSTOM_SKIN_SOURCE = SynchedEntityData.defineId(ExhibitionCharacterEntity.class, EntityDataSerializers.STRING);

    public ExhibitionCharacterEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.applyDisplayInvariants();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SPEECH_TEXT, "");
        builder.define(DATA_CUSTOM_SKIN_ENABLED, false);
        builder.define(DATA_CUSTOM_SKIN_PLAYER, true);
        builder.define(DATA_CUSTOM_SKIN_SOURCE, "");
    }

    @Override
    public void tick() {
        super.tick();
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        if (!this.isNoAi() || !this.isInvulnerable() || !this.isNoGravity()) this.applyDisplayInvariants();
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

    public boolean applyConfiguration(Identifier characterId, String skinId, float yaw, float scale, String customName, boolean showName, String speechText,
                                      boolean customSkinEnabled, boolean customSkinPlayer, String customSkinSource) {
        if (!CharacterManager.INSTANCE.contains(characterId) || !Float.isFinite(yaw) || !Float.isFinite(scale)
                || (customName != null && customName.length() > MAX_CUSTOM_NAME_LENGTH)) return false;
        if (customSkinEnabled && !validCustomSkinSource(customSkinPlayer, customSkinSource)) return false;
        CharacterDefinition definition = CharacterManager.INSTANCE.get(characterId);
        CharacterSkinDefinition skin = definition.skins().stream().filter(value -> value.id().equals(skinId)).findFirst().orElse(null);
        if (skin == null) return false;
        this.setCharacterId(characterId);
        this.setSkinId(skin.id());
        this.setExhibitionYaw(yaw);
        this.setDisplayScale(scale);
        this.setDisplayCustomName(customName, showName);
        this.setSpeechText(speechText);
        this.setCustomSkinPlayer(customSkinPlayer);
        this.setCustomSkinSource(customSkinSource);
        this.setCustomSkinEnabled(customSkinEnabled);
        this.applyDisplayInvariants();
        return true;
    }

    public void openConfiguration(ServerPlayer player) {
        if (!this.canPlayerConfigure(player)) return;
        PacketDistributor.sendToPlayer(player, new OpenExhibitionCharacterConfigPayload(
                this.getId(), CharacterManager.INSTANCE.values(), this.characterId(), this.skinId(), this.getYRot(),
                this.displayScale(), this.displayCustomName(), this.isCustomNameVisible(), this.speechText(), this.customSkinEnabled(),
                this.customSkinPlayer(), this.customSkinSource()));
    }

    public boolean canPlayerConfigure(Player player) {
        if (player == null || player.distanceToSqr(this) > 64.0D) return false;
        return this.isConfigurationTool(player.getMainHandItem()) || this.isConfigurationTool(player.getOffhandItem());
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

    public void setExhibitionYaw(float yaw) {
        float safeYaw = Mth.wrapDegrees(yaw);
        this.setYRot(safeYaw);
        this.setYBodyRot(safeYaw);
        this.setYHeadRot(safeYaw);
        this.yRotO = safeYaw;
        this.yBodyRotO = safeYaw;
        this.yHeadRotO = safeYaw;
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
        this.setCustomSkinPlayer(input.getBooleanOr("exhibition_custom_skin_player", true));
        this.setCustomSkinSource(input.getStringOr("exhibition_custom_skin_source", ""));
        this.setCustomSkinEnabled(input.getBooleanOr("exhibition_custom_skin_enabled", false)
                && validCustomSkinSource(this.customSkinPlayer(), this.customSkinSource()));
        this.setExhibitionYaw(this.getYRot());
        this.applyDisplayInvariants();
        this.refreshDimensions();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("exhibition_speech", this.speechText());
        output.putBoolean("exhibition_custom_skin_enabled", this.customSkinEnabled());
        output.putBoolean("exhibition_custom_skin_player", this.customSkinPlayer());
        output.putString("exhibition_custom_skin_source", this.customSkinSource());
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
