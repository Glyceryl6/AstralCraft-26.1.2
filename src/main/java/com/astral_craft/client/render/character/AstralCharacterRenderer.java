package com.astral_craft.client.render.character;

import com.astral_craft.client.model.character.AstralCharacterAnimationRegistry;
import com.astral_craft.client.model.character.AstralGeoAnimationManager;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.CharacterManager;
import com.astral_craft.common.gameplay.character.CharacterSkinDefinition;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

/**
 * Default data-driven character renderer.
 *
 * <p>The default renderer now uses Minecraft's baked player model instead of the old flat paper-doll preview. This
 * keeps the common Astral character entity data-driven while still making character skins behave like normal 64x64
 * player skins. Characters that need special logic can still register their own entity type and renderer while keeping
 * {@link AstralCharacterEntity} as their common base class.</p>
 */
public class AstralCharacterRenderer extends MobRenderer<AstralCharacterEntity, AstralCharacterRenderState, PlayerModel> {

    public AstralCharacterRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.25F);
        // The default shared renderer intentionally uses the vanilla player model layer. Character cards, skin cards,
        // and preview panes should therefore render the same humanoid model instead of the old flat paper-doll. 
    }

    @Override
    public AstralCharacterRenderState createRenderState() {
        return new AstralCharacterRenderState();
    }

    @Override
    public void extractRenderState(AstralCharacterEntity entity, AstralCharacterRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.characterId = entity.characterId();
        state.skinId = entity.skinId();
        CharacterDefinition definition = CharacterManager.INSTANCE.get(state.characterId);
        CharacterSkinDefinition skin = definition.skinOrDefault(state.skinId);
        state.texture = skin.texture();
        state.modelKey = definition.modelKey();
        state.animationSetKey = definition.animationSetKey();
        state.animationAction = AstralCharacterAnimationRegistry.clipName(state.characterId, entity.animationAction());
        state.animationTimeSeconds = (entity.tickCount + partialTick) / 20.0F;
        state.rootPose = AstralGeoAnimationManager.INSTANCE.sample(state.animationSetKey, state.animationAction, "root", state.animationTimeSeconds);
        state.skin = new PlayerSkin(new ClientAsset.ResourceTexture(skin.texture()), null, null, PlayerModelType.WIDE, true);
    }

    @Override
    public Identifier getTextureLocation(AstralCharacterRenderState state) {
        return state.skin.body().texturePath();
    }

}