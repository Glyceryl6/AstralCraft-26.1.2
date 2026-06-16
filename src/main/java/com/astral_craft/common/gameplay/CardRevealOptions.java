package com.astral_craft.common.gameplay;

import com.astral_craft.common.network.CardRevealPayload;

public record CardRevealOptions(CardRevealAudience audience, String animation, int durationTicks) {

    public static CardRevealOptions none() {
        return new CardRevealOptions(CardRevealAudience.NONE, CardRevealPayload.ANIMATION_FLIP, 0);
    }

    public static CardRevealOptions selfFlip(int durationTicks) {
        return new CardRevealOptions(CardRevealAudience.SELF, CardRevealPayload.ANIMATION_FLIP, durationTicks);
    }

    public static CardRevealOptions selfApproach(int durationTicks) {
        return new CardRevealOptions(CardRevealAudience.SELF, CardRevealPayload.ANIMATION_APPROACH, durationTicks);
    }

    public boolean enabled() {
        return this.audience != CardRevealAudience.NONE && this.durationTicks > 0;
    }

}