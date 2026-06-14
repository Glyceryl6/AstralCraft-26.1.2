package com.astral_craft.client.gui.reveal;

public class CardRevealSettings {

    public int flipIntroHoldTicks = 8;
    public int flipRotateTicks = 20;
    public int flipOutroHoldTicks = 18;
    public int flipFadeTicks = 7;

    public int approachInTicks = 12;
    public int approachHoldTicks = 4;
    public int approachOutTicks = 12;

    public float approachStartScale = 0.18F;
    public float approachEndScale = 0.08F;
    public float itemGuiBaseSize = 16.0F;
    public float frontArtSizeRatio = 0.54F;
    public float frontArtYOffsetRatio = -0.040F;
    public float backArtWidthRatio = 0.58F;
    public float backArtHeightRatio = 0.80F;
    public float backArtYOffsetRatio = 0.010F;
    public float sideEdgeWidthRatio = 0.070F;
    public float sideEdgeHeightRatio = 0.865F;

    public int flipDurationTicks() {
        return this.flipIntroHoldTicks + this.flipRotateTicks + this.flipOutroHoldTicks + this.flipFadeTicks;
    }

    public int approachDurationTicks() {
        return this.approachInTicks + this.approachHoldTicks + this.approachOutTicks;
    }

}