package com.astral_craft.common.gameplay.character.skill.effect;

import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

public class AstralStatusMobEffect extends MobEffect {

    protected final Identifier statusId;
    protected final Identifier iconTexture;

    public AstralStatusMobEffect(MobEffectCategory category, int color, Identifier statusId, Identifier iconTexture) {
        super(category, color);
        this.statusId = statusId;
        this.iconTexture = iconTexture;
    }

    public Identifier statusId() {
        return this.statusId;
    }

    public Identifier iconTexture() {
        return this.iconTexture;
    }

    public boolean isStatus(Identifier statusId) {
        return this.statusId != null && this.statusId.equals(statusId);
    }

    public void onEffectAdded(LivingEntity entity, MobEffectInstance instance) {
    }

    public void onEffectRemoved(LivingEntity entity) {
    }

    public void onEffectExpired(LivingEntity entity, MobEffectInstance instance) {
        this.onEffectRemoved(entity);
    }

    public void onAttackEntity(AttackEntityEvent event) {
    }

    public void onLivingChangeTarget(LivingChangeTargetEvent event) {
    }

    public void onInvulnerabilityCheck(EntityInvulnerabilityCheckEvent event) {
    }

}