package com.astral_craft.common.gameplay.character.skill.effect;

import com.astral_craft.common.registry.AstralStatusEffects;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

public class ShadowCloakMobEffect extends TrueInvisibilityMobEffect {

    public ShadowCloakMobEffect(MobEffectCategory category, int color, Identifier statusId, Identifier iconTexture) {
        super(category, color, statusId, iconTexture);
    }

    @Override
    public void onAttackEntity(AttackEntityEvent event) {
        if (!event.getEntity().level().isClientSide() && event.getEntity().hasEffect(AstralStatusEffects.SHADOW_CLOAK)) {
            event.getEntity().removeEffect(AstralStatusEffects.SHADOW_CLOAK);
        }
    }

}
