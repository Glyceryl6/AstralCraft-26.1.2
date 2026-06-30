package com.astral_craft.common.gameplay.character.skill.effect;

import com.astral_craft.common.registry.AstralStatusEffects;
import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;

public class AstralPhaseMobEffect extends TrueInvisibilityMobEffect {

    public AstralPhaseMobEffect(MobEffectCategory category, int color, Identifier statusId, Identifier iconTexture) {
        super(category, color, statusId, iconTexture);
    }

    @Override
    public void onInvulnerabilityCheck(EntityInvulnerabilityCheckEvent event) {
        if (event.getEntity() instanceof LivingEntity entity && entity.hasEffect(AstralStatusEffects.ASTRAL_PHASE) && !event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)) {
            event.setInvulnerable(true);
        }
    }

}
