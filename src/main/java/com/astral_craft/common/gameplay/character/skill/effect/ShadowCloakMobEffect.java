package com.astral_craft.common.gameplay.character.skill.effect;

import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

public class ShadowCloakMobEffect extends TrueInvisibilityMobEffect {

    public ShadowCloakMobEffect(MobEffectCategory category, int color, Identifier statusId, Identifier iconTexture, Identifier characterId) {
        super(category, color, statusId, iconTexture, characterId);
    }

    @Override
    public void onAttackEntity(AttackEntityEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            this.removeFrom(event.getEntity());
        }
    }

}
