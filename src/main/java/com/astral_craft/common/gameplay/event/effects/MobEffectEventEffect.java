package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public record MobEffectEventEffect(Holder<MobEffect> effect, int durationTicks, int amplifier, boolean ambient, boolean visible, boolean showIcon) implements AstralEventEffect {

    public static final MapCodec<MobEffectEventEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            MobEffect.CODEC.fieldOf("effect").forGetter(MobEffectEventEffect::effect),
            Codec.INT.optionalFieldOf("duration_ticks", 200).forGetter(MobEffectEventEffect::durationTicks),
            Codec.INT.optionalFieldOf("amplifier", 0).forGetter(MobEffectEventEffect::amplifier),
            Codec.BOOL.optionalFieldOf("ambient", false).forGetter(MobEffectEventEffect::ambient),
            Codec.BOOL.optionalFieldOf("visible", true).forGetter(MobEffectEventEffect::visible),
            Codec.BOOL.optionalFieldOf("show_icon", true).forGetter(MobEffectEventEffect::showIcon)
    ).apply(instance, MobEffectEventEffect::new));

    public MobEffectEventEffect(Holder<MobEffect> effect, int durationTicks, int amplifier) {
        this(effect, durationTicks, amplifier, false, true, true);
    }

    @Override
    public String typeId() {
        return AstralCraft.prefix("effect").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void apply(AstralEventContext context) {
        LivingEntity target = context.targetLiving();
        if (target == null) return;
        target.addEffect(new MobEffectInstance(this.effect,
                Math.max(1, this.durationTicks),
                Math.max(0, this.amplifier),
                this.ambient, this.visible, this.showIcon));
    }

}