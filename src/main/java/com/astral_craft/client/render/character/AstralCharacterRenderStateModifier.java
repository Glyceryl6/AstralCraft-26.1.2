package com.astral_craft.client.render.character;

import com.astral_craft.client.gameplay.character.ClientCharacterDefinitionCache;
import com.astral_craft.client.model.character.AstralCharacterAnimationRegistry;
import com.astral_craft.client.model.character.AstralGeoAnimationManager;
import com.astral_craft.client.render.CardRevealEntityOverlay;
import com.astral_craft.common.gameplay.character.ActiveCharacterState;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.skin.CharacterSkinDefinition;
import com.astral_craft.common.registry.AstralAttachments;
import com.astral_craft.common.registry.AstralStatusEffects;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;

import java.util.List;

public class AstralCharacterRenderStateModifier extends AvatarRenderStateModifier {

    @Override
    public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
        state.setRenderData(CardRevealEntityOverlay.CARD_REVEAL, CardRevealEntityOverlay.activeFor(avatar));
        state.setRenderData(AstralPlayerCharacterRenderBridge.CHARACTER_RENDER_DATA, null);
        ActiveCharacterState characterState = avatar.getData(AstralAttachments.ACTIVE_CHARACTER);
        if (characterState.active() && ClientCharacterDefinitionCache.INSTANCE.contains(characterState.characterId())) {
            CharacterDefinition definition = ClientCharacterDefinitionCache.INSTANCE.getOrFallback(characterState.characterId());
            CharacterSkinDefinition skin = definition.skinOrDefault(characterState.skinId());
            state.skin = new PlayerSkin(new ClientAsset.ResourceTexture(skin.texture()), null, null, PlayerModelType.SLIM, true);
            String action = this.animationAction(avatar, definition);
            state.setRenderData(AstralPlayerCharacterRenderBridge.CHARACTER_RENDER_DATA,
                    new AstralPlayerCharacterRenderBridge.PlayerCharacterRenderData(
                            definition.modelKey(), definition.rendererKey(), definition.animationSetKey(), action,
                            avatar.tickCount / 20.0F));
        }

        if (avatar.hasEffect(AstralStatusEffects.SHADOW_CLOAK) || avatar.hasEffect(AstralStatusEffects.ASTRAL_PHASE)) {
            state.isInvisibleToPlayer = false;
        }
    }

    protected <T extends Avatar & ClientAvatarEntity> String animationAction(T avatar, CharacterDefinition definition) {
        List<String> available = AstralGeoAnimationManager.INSTANCE.animationNames(definition.animationSetKey());
        String action;
        if (avatar.isFallFlying()) {
            action = firstAvailable(available, "fall_flying", "fly", "air");
        } else if (avatar.isSwimming()) {
            action = firstAvailable(available, "swim", "swimming");
        } else if (avatar.isCrouching() && avatar.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4D) {
            action = firstAvailable(available, "crouch_walk", "sneak", "walk");
        } else if (avatar.isCrouching()) {
            action = firstAvailable(available, "crouch", "sneak", "idle");
        } else if (avatar.isUsingItem()) {
            action = firstAvailable(available, "use_item", "use", "idle");
        } else if (avatar.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4D) {
            action = firstAvailable(available, "walk", "walking", "run");
        } else {
            action = firstAvailable(available, definition.previewAction(), "idle");
        }

        return AstralCharacterAnimationRegistry.clipName(definition.id(), action);
    }

    protected static String firstAvailable(List<String> available, String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank() && available.contains(candidate)) return candidate;
        }
        return available.isEmpty() ? "idle" : available.getFirst();
    }

}