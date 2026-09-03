package com.astral_craft.client.render.character;

import com.astral_craft.client.gameplay.character.ClientCharacterDefinitionCache;
import com.astral_craft.client.model.character.AstralCharacterAnimationRegistry;
import com.astral_craft.client.model.character.AstralGeoAnimationManager;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.entity.character.ExhibitionCharacterEntity;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.skin.CharacterSkinDefinition;
import com.astral_craft.common.registry.AstralStatusEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public class AstralCharacterRenderer<T extends AstralCharacterEntity> extends MobRenderer<T, AstralCharacterRenderState, PlayerModel> {

    public AstralCharacterRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.25F);
        this.addLayer(new BoardKnockoutLayer(this));
    }

    @Override
    public AstralCharacterRenderState createRenderState() {
        return new AstralCharacterRenderState();
    }

    @Override
    public void extractRenderState(T entity, AstralCharacterRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.characterId = entity.characterId();
        state.skinId = entity.skinId();
        CharacterDefinition definition = ClientCharacterDefinitionCache.INSTANCE.getOrFallback(state.characterId);
        CharacterSkinDefinition skin = definition.skinOrDefault(state.skinId);
        state.texture = skin.texture();
        state.modelKey = definition.modelKey();
        state.animationSetKey = definition.animationSetKey();
        state.knockedDown = entity.isBoardPawn() && "knockdown".equals(entity.animationAction());
        state.animationAction = AstralCharacterAnimationRegistry.clipName(state.characterId, entity.animationAction());
        state.animationTimeSeconds = entity.animationAgeTicks(partialTick) / 20.0F;
        state.rootPose = AstralGeoAnimationManager.INSTANCE.sample(state.animationSetKey, state.animationAction, "root", state.animationTimeSeconds);
        state.skin = new PlayerSkin(new ClientAsset.ResourceTexture(skin.texture()), null, null, PlayerModelType.SLIM, true);
        if (entity instanceof ExhibitionCharacterEntity exhibition) {
            state.bodyRot = exhibition.displayYaw();
            state.yRot = 0.0F;
            if (exhibition.customSkinEnabled()) {
                PlayerSkin customSkin = this.customSkin(exhibition);
                if (customSkin != null) {
                    state.skin = customSkin;
                    state.texture = customSkin.body().texturePath();
                }
            }
        }

        if (entity.hasEffect(AstralStatusEffects.SHADOW_CLOAK) || entity.hasEffect(AstralStatusEffects.ASTRAL_PHASE)) {
            state.isInvisibleToPlayer = false;
        }
    }

    @Override
    protected boolean shouldShowName(T entity, double squaredDistanceToCamera) {
        return entity.isCustomNameVisible() && super.shouldShowName(entity, squaredDistanceToCamera);
    }

    @Override
    public Identifier getTextureLocation(AstralCharacterRenderState state) {
        return state.skin.body().texturePath();
    }

    @Nullable
    private PlayerSkin customSkin(ExhibitionCharacterEntity entity) {
        String source = entity.customSkinSource();
        if (!ExhibitionCharacterEntity.validCustomSkinSource(entity.customSkinPlayer(), source)) return null;
        if (!entity.customSkinPlayer()) {
            Identifier texture = Identifier.tryParse(source);
            return texture == null ? null : new PlayerSkin(new ClientAsset.ResourceTexture(texture), null, null, PlayerModelType.SLIM, true);
        }

        UUID uuid = ExhibitionCharacterEntity.parseCustomPlayerUuid(source);
        ResolvableProfile profile = uuid == null ? ResolvableProfile.createUnresolved(source) : ResolvableProfile.createUnresolved(uuid);
        return Minecraft.getInstance().playerSkinRenderCache().getOrDefault(profile).playerSkin();
    }
}
