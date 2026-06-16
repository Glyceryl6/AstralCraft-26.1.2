package com.astral_craft.client.gui.reveal;

public class CardRevealSettings {

    public int flipIntroHoldTicks = 8;
    public int flipRotateTicks = 10;
    public int flipOutroHoldTicks = 18;
    public int flipFadeTicks = 7;

    public int approachInTicks = 12;
    public int approachHoldTicks = 4;
    public int approachOutTicks = 12;

    public int modelMinSize = 96;
    public int modelMaxSize = 292;
    public float modelScreenHeightRatio = 0.62F;
    public float modelScreenWidthRatio = 0.48F;
    public int modelScreenMargin = 20;

    public float cardModelScale = 1.0F;
    public float cardTextScale = 1.0F;

    public float cardCenterYOffsetRatio = 0.0F;
    public int cardCenterYOffsetPixels = 0;

    public float approachStartScale = 0.18F;
    public float approachEndScale = 0.08F;

    public float cardFrameWidthRatio = 0.68F;
    public float cardFrameHeightRatio = 0.96F;
    public float cardFrameYOffsetRatio = 0.0F;

    public float frontArtSizeRatio = 0.54F;
    public float frontArtYOffsetRatio = -0.040F;
    public float backArtWidthRatio = 0.58F;
    public float backArtHeightRatio = 0.80F;
    public float backArtYOffsetRatio = 0.010F;
    public float sideEdgeWidthRatio = 0.070F;
    public float sideEdgeHeightRatio = 0.865F;

    public float textMaxWidthRatio = 0.46F;
    public float titleTextMaxWidthRatio = 0.46F;
    public float bodyTextMaxWidthRatio = 0.46F;
    public int minTitleTextWidth = 48;
    public int minBodyTextWidth = 58;
    public int maxTitleTextWidth = 260;
    public int maxBodyTextWidth = 260;
    public float titleYOffsetRatio = -0.32F;
    public float bodyYOffsetRatio = 0.22F;
    public float titleExtraYOffsetRatio = 0.0F;
    public float bodyExtraYOffsetRatio = 0.0F;
    public int titleExtraYOffsetPixels = 0;
    public int bodyExtraYOffsetPixels = 0;
    public int bodyMaxLines = 5;
    public boolean textShadow = false;
    public int titleTextColor = 0xFFF0B0;
    public int bodyTextColor = 0xFFFFFF;
    public float textBackdropAlpha = 0.0F;
    public float itemScaleStep = 0.0F;

    public int flipDurationTicks() {
        return this.flipIntroHoldTicks + this.flipRotateTicks + this.flipOutroHoldTicks + this.flipFadeTicks;
    }

    public int approachDurationTicks() {
        return this.approachInTicks + this.approachHoldTicks + this.approachOutTicks;
    }

    public int responsiveModelSize(int guiWidth, int guiHeight) {
        int byHeight = Math.round(guiHeight * this.modelScreenHeightRatio);
        int byWidth = Math.round(guiWidth * this.modelScreenWidthRatio);
        int byMargin = Math.max(8, Math.min(guiWidth, guiHeight) - this.modelScreenMargin * 2);
        int unclamped = Math.min(Math.min(byHeight, byWidth), byMargin);
        int lower = Math.clamp(byMargin, 8, this.modelMinSize);
        return Math.clamp(this.modelMaxSize, 8, Math.max(lower, unclamped));
    }

    public int responsiveOffset(int baseSize, float ratio, int pixels) {
        return Math.round(baseSize * ratio) + pixels;
    }

    public float pixelsToRatio(int desiredPixels, int currentResponsiveSize) {
        if (currentResponsiveSize == 0) {
            return 0.0F;
        }

        return (float) desiredPixels / (float) currentResponsiveSize;
    }

    public float quantizeItemScale(float scale) {
        if (this.itemScaleStep <= 0.0001F) {
            return scale;
        }
        float stepped = Math.round(scale / this.itemScaleStep) * this.itemScaleStep;
        return Math.max(this.itemScaleStep, stepped);
    }

    public CardRevealSettings setModelScale(float value) {
        this.cardModelScale = value;
        return this;
    }

    public CardRevealSettings setTextScale(float value) {
        this.cardTextScale = value;
        return this;
    }

    public CardRevealSettings setTextWidth(float titleRatio, float bodyRatio) {
        this.titleTextMaxWidthRatio = titleRatio;
        this.bodyTextMaxWidthRatio = bodyRatio;
        this.textMaxWidthRatio = bodyRatio;
        return this;
    }

    public CardRevealSettings setTextShadow(boolean value) {
        this.textShadow = value;
        return this;
    }

}