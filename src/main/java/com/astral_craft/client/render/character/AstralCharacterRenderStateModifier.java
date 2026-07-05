package com.astral_craft.client.render.character;

import com.astral_craft.client.gameplay.character.ClientCharacterDefinitionCache;
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

public class AstralCharacterRenderStateModifier extends AvatarRenderStateModifier {

    @Override
    public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
        state.setRenderData(CardRevealEntityOverlay.CARD_REVEAL, CardRevealEntityOverlay.activeFor(avatar));
        ActiveCharacterState characterState = avatar.getData(AstralAttachments.ACTIVE_CHARACTER);
        if (characterState.active() && ClientCharacterDefinitionCache.INSTANCE.contains(characterState.characterId())) {
            CharacterDefinition definition = ClientCharacterDefinitionCache.INSTANCE.getOrFallback(characterState.characterId());
            CharacterSkinDefinition skin = definition.skinOrDefault(characterState.skinId());
            state.skin = new PlayerSkin(new ClientAsset.ResourceTexture(skin.texture()), null, null, PlayerModelType.SLIM, true);
        }

        if (avatar.hasEffect(AstralStatusEffects.SHADOW_CLOAK) || avatar.hasEffect(AstralStatusEffects.ASTRAL_PHASE)) {
            state.isInvisibleToPlayer = false;
        }
    }

}