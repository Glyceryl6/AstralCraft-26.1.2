package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;

public record ClearMobEffectEventEffect(Holder<MobEffect> effect, boolean all) implements AstralEventEffect {

    public static final MapCodec<ClearMobEffectEventEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            MobEffect.CODEC.fieldOf("effect").forGetter(ClearMobEffectEventEffect::effect),
            Codec.BOOL.optionalFieldOf("all", false).forGetter(ClearMobEffectEventEffect::all)
    ).apply(instance, ClearMobEffectEventEffect::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("clear_effect").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void apply(AstralEventContext context) {
        LivingEntity target = context.targetLiving();
        if (target == null) return;
        if (this.all) {
            target.removeAllEffects();
            return;
        }

        target.removeEffect(this.effect);
    }

}