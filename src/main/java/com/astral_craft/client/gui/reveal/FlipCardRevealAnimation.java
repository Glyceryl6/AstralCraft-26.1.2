package com.astral_craft.client.gui.reveal;

import net.minecraft.util.Mth;

public class FlipCardRevealAnimation implements CardRevealAnimation {

    @Override
    public String id() {
        return "flip";
    }

    @Override
    public int defaultDuration(CardRevealSettings settings) {
        return settings.flipDurationTicks();
    }

    @Override
    public void render(CardRevealRenderContext context, CardRevealRenderer renderer) {
        CardRevealFrame frame = this.frame(context.ageTicks(), context.settings());
        renderer.renderCard(context.graphics(), context.reveal(), context.settings(), context.centerX(), context.centerY(), context.modelSize(), frame);
    }

    public CardRevealFrame frame(float ageTicks, CardRevealSettings settings) {
        float alpha = this.fade(ageTicks, settings.flipDurationTicks());
        if (ageTicks < settings.flipIntroHoldTicks) {
            return new CardRevealFrame(false, 1.0F, 1.0F, alpha, 0, false);
        }

        if (ageTicks < settings.flipIntroHoldTicks + settings.flipRotateTicks) {
            float t = (ageTicks - settings.flipIntroHoldTicks) / Math.max(1.0F, settings.flipRotateTicks);
            float eased = this.easeInOut(t);
            float widthScale = Math.max(0.035F, Math.abs(Mth.cos(eased * Mth.PI)));
            boolean front = eased >= 0.5F;
            return new CardRevealFrame(front, widthScale, 1.0F, alpha, 0, front && widthScale > 0.33F);
        }

        return new CardRevealFrame(true, 1.0F, 1.0F, alpha, 0, true);
    }

    public float fade(float ageTicks, int durationTicks) {
        float in = Mth.clamp(ageTicks / 6.0F, 0.0F, 1.0F);
        float out = Mth.clamp((durationTicks - ageTicks) / 9.0F, 0.0F, 1.0F);
        return Math.min(in, out);
    }

    public float easeInOut(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

}