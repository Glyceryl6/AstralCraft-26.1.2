package com.astral_craft.common.gameplay.character.skill.effect;

import com.astral_craft.common.registry.AstralStatusEffects;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;

public class TrueInvisibilityMobEffect extends AstralStatusMobEffect {

    public static final double CLEAR_TARGET_RANGE = 64.0D;

    public TrueInvisibilityMobEffect(MobEffectCategory category, int color, Identifier statusId, Identifier iconTexture) {
        super(category, color, statusId, iconTexture);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        entity.setInvisible(true);
        this.clearMobTargets(level, entity);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return true;
    }

    @Override
    public void onEffectAdded(LivingEntity entity, MobEffectInstance instance) {
        entity.setInvisible(true);
    }

    @Override
    public void onEffectRemoved(LivingEntity entity) {
        if (!this.hasOtherTrueInvisibility(entity) && !entity.hasEffect(MobEffects.INVISIBILITY)) {
            entity.setInvisible(false);
        }
    }

    @Override
    public void onLivingChangeTarget(LivingChangeTargetEvent event) {
        event.setNewAboutToBeSetTarget(null);
    }

    protected boolean hasOtherTrueInvisibility(LivingEntity entity) {
        if (entity == null) return false;
        boolean hasShadowCloak = this != AstralStatusEffects.SHADOW_CLOAK.value() && entity.hasEffect(AstralStatusEffects.SHADOW_CLOAK);
        boolean hasAstralPhase = this != AstralStatusEffects.ASTRAL_PHASE.value() && entity.hasEffect(AstralStatusEffects.ASTRAL_PHASE);
        return hasShadowCloak || hasAstralPhase;
    }

    protected void clearMobTargets(ServerLevel level, LivingEntity target) {
        AABB bounds = target.getBoundingBox().inflate(CLEAR_TARGET_RANGE);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, bounds, mob -> mob.getTarget() == target)) {
            mob.setTarget(null);
        }
    }

}
