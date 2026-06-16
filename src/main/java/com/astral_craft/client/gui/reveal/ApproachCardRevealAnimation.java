package com.astral_craft.client.gui.reveal;

import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class ApproachCardRevealAnimation implements CardRevealAnimation {

    @Override
    public Identifier id() {
        return CardRevealAnimations.APPROACH;
    }

    @Override
    public int defaultDuration(CardRevealSettings settings) {
        return settings.approachDurationTicks();
    }

    @Override
    public void render(CardRevealRenderContext context, CardRevealRenderer renderer) {
        CardRevealFrame frame = this.frame(context.ageTicks(), context.settings());
        renderer.renderCard(context.graphics(), context.reveal(), context.settings(),
                context.centerX(), context.centerY(), context.modelSize(), frame);
    }

    public CardRevealFrame frame(float ageTicks, CardRevealSettings settings) {
        int inTicks = Math.max(1, settings.approachInTicks);
        int holdTicks = Math.max(0, settings.approachHoldTicks);
        int outTicks = Math.max(1, settings.approachOutTicks);
        if (ageTicks < inTicks) {
            float t = Mth.clamp(ageTicks / inTicks, 0.0F, 1.0F);
            float eased = this.easeOutCubic(t);
            float scale = Mth.lerp(eased, settings.approachStartScale, 1.0F);
            float alpha = Mth.clamp(t * 1.75F, 0.0F, 1.0F);
            int y = Math.round((1.0F - scale) * 34.0F);
            return new CardRevealFrame(true, scale, scale, 1.0F, 1.0F, alpha, y, scale > 0.36F);
        }

        if (ageTicks < inTicks + holdTicks) {
            return new CardRevealFrame(true, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0, true);
        }

        float t = Mth.clamp((ageTicks - inTicks - holdTicks) / outTicks, 0.0F, 1.0F);
        float eased = this.easeInCubic(t);
        float scale = Mth.lerp(eased, 1.0F, settings.approachEndScale);
        float alpha = 1.0F - eased;
        int y = -Math.round(eased * 20.0F);
        return new CardRevealFrame(true, scale, scale, 1.0F, 1.0F, alpha, y, scale > 0.30F);
    }

    public float easeOutCubic(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        float u = 1.0F - t;
        return 1.0F - u * u * u;
    }

    public float easeInCubic(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        return t * t * t;
    }

}