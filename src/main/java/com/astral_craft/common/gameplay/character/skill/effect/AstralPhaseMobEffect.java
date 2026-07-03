package com.astral_craft.common.gameplay.character.skill.effect;

import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;

public class AstralPhaseMobEffect extends TrueInvisibilityMobEffect {

    public AstralPhaseMobEffect(MobEffectCategory category, int color, Identifier statusId, Identifier iconTexture, Identifier characterId) {
        super(category, color, statusId, iconTexture, characterId);
    }

    @Override
    public void onInvulnerabilityCheck(EntityInvulnerabilityCheckEvent event) {
        if (!event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)) {
            event.setInvulnerable(true);
        }
    }

}
