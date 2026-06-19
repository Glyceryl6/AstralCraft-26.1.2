package com.astral_craft.client.gui.character;

import net.minecraft.util.Mth;

public class CharacterLayout {

    public static final int MIN_SCREEN_MARGIN = 10;
    public static final int PANEL_GAP = 10;
    public static final int MAX_TOTAL_WIDTH = 790;
    public static final int MAX_TOTAL_HEIGHT = 420;
    public static final int MIN_TOTAL_HEIGHT = 250;
    public static final int MIN_LEFT_WIDTH = 104;
    public static final int MAX_LEFT_WIDTH = 190;
    public static final int MIN_RIGHT_WIDTH = 250;
    public static final int GRID_GAP = 8;
    public static final int TAB_GAP = 4;

    public final int leftX;
    public final int rightX;
    public final int topY;
    public final int leftW;
    public final int rightW;
    public final int totalH;
    public final int backX;
    public final int backY;
    public final int backW;
    public final int backH;
    public final int previewX;
    public final int previewY;
    public final int previewW;
    public final int previewH;
    public final int detailButtonX;
    public final int detailButtonY;
    public final int detailButtonW;
    public final int detailButtonH;
    public final int gridX;
    public final int gridY;
    public final int gridW;
    public final int gridH;
    public final int characterCardW;
    public final int characterCardH;
    public final int mainTabX;
    public final int mainTabY;
    public final int mainTabW;
    public final int mainTabH;
    public final int subTabX;
    public final int subTabY;
    public final int subTabW;
    public final int subTabH;
    public final int bodyX;
    public final int bodyY;
    public final int bodyW;
    public final int bodyH;
    public final int skinCardW;
    public final int skinCardH;
    public final float previewEntityScale;
    public final float cardEntityScale;
    public final float skinEntityScale;

    public CharacterLayout(int leftX, int rightX, int topY, int leftW, int rightW, int totalH, int backX, int backY, int backW, int backH,
                           int previewX, int previewY, int previewW, int previewH, int detailButtonX, int detailButtonY, int detailButtonW, int detailButtonH,
                           int gridX, int gridY, int gridW, int gridH, int characterCardW, int characterCardH, int mainTabX, int mainTabY, int mainTabW, int mainTabH,
                           int subTabX, int subTabY, int subTabW, int subTabH, int bodyX, int bodyY, int bodyW, int bodyH, int skinCardW, int skinCardH,
                           float previewEntityScale, float cardEntityScale, float skinEntityScale) {
        this.leftX = leftX;
        this.rightX = rightX;
        this.topY = topY;
        this.leftW = leftW;
        this.rightW = rightW;
        this.totalH = totalH;
        this.backX = backX;
        this.backY = backY;
        this.backW = backW;
        this.backH = backH;
        this.previewX = previewX;
        this.previewY = previewY;
        this.previewW = previewW;
        this.previewH = previewH;
        this.detailButtonX = detailButtonX;
        this.detailButtonY = detailButtonY;
        this.detailButtonW = detailButtonW;
        this.detailButtonH = detailButtonH;
        this.gridX = gridX;
        this.gridY = gridY;
        this.gridW = gridW;
        this.gridH = gridH;
        this.characterCardW = characterCardW;
        this.characterCardH = characterCardH;
        this.mainTabX = mainTabX;
        this.mainTabY = mainTabY;
        this.mainTabW = mainTabW;
        this.mainTabH = mainTabH;
        this.subTabX = subTabX;
        this.subTabY = subTabY;
        this.subTabW = subTabW;
        this.subTabH = subTabH;
        this.bodyX = bodyX;
        this.bodyY = bodyY;
        this.bodyW = bodyW;
        this.bodyH = bodyH;
        this.skinCardW = skinCardW;
        this.skinCardH = skinCardH;
        this.previewEntityScale = previewEntityScale;
        this.cardEntityScale = cardEntityScale;
        this.skinEntityScale = skinEntityScale;
    }

    public static CharacterLayout create(int screenWidth, int screenHeight, CharacterSettingsScreen.ScreenMode mode, CharacterSettingsScreen.MainTab mainTab) {
        int margin = MIN_SCREEN_MARGIN;
        int availableWidth = Math.max(260, screenWidth - margin * 2);
        int availableHeight = Math.max(120, screenHeight - margin * 2);
        int totalW = Math.min(MAX_TOTAL_WIDTH, availableWidth);
        int totalH = Mth.clamp(availableHeight, Math.min(MIN_TOTAL_HEIGHT, availableHeight), MAX_TOTAL_HEIGHT);
        int leftW = Mth.clamp((int) (totalW * 0.26F), MIN_LEFT_WIDTH, MAX_LEFT_WIDTH);
        int rightW = totalW - PANEL_GAP - leftW;
        if (rightW < MIN_RIGHT_WIDTH) {
            leftW = Math.max(92, totalW - PANEL_GAP - MIN_RIGHT_WIDTH);
            rightW = totalW - PANEL_GAP - leftW;
        }

        int realW = leftW + PANEL_GAP + rightW;
        int leftX = Math.max(margin, (screenWidth - realW) / 2);
        int topY = Math.max(margin, (screenHeight - totalH) / 2);
        int rightX = leftX + leftW + PANEL_GAP;
        int backX = leftX + 8;
        int backY = topY + 6;
        int backW = Math.min(58, leftW - 16);
        int backH = 20;
        int previewX = leftX + 10;
        int previewY = topY + 38;
        int previewW = leftW - 20;
        int previewH = Math.max(96, totalH - 96);
        int detailButtonW = Math.clamp(rightW / 4, 86, 120);
        int detailButtonH = 26;
        int detailButtonX = rightX + rightW - detailButtonW - 14;
        int detailButtonY = topY + 6;
        int gridX = rightX + 14;
        int gridY = topY + 80;
        int gridW = rightW - 28;
        int gridH = totalH - 94;
        int characterCardW = Mth.clamp((gridW - GRID_GAP * 3) / 4, 58, 84);
        int characterCardH = Mth.clamp((int) (characterCardW * 1.28F), 74, 108);
        int mainTabX = rightX + 14;
        int mainTabY = topY + 44;
        int mainTabH = 30;
        int mainTabW = Math.max(50, (rightW - 28 - TAB_GAP * (CharacterSettingsScreen.MainTab.values().length - 1)) / CharacterSettingsScreen.MainTab.values().length);
        int subTabX = rightX + 16;
        int subTabY = mainTabY + mainTabH + 8;
        int subTabH = 24;
        int subTabW = Math.max(48, (rightW - 32 - TAB_GAP * (CharacterSettingsScreen.ArchiveTab.values().length - 1)) / CharacterSettingsScreen.ArchiveTab.values().length);
        int bodyX = rightX + 14;
        int bodyY = mode == CharacterSettingsScreen.ScreenMode.DETAIL && mainTab == CharacterSettingsScreen.MainTab.ARCHIVE ? subTabY + subTabH + 8 : mainTabY + mainTabH + 10;
        int bodyW = rightW - 28;
        int bodyH = topY + totalH - bodyY - 12;
        int skinCardW = Mth.clamp((bodyW - 32) / 3, 72, 104);
        int skinCardH = Mth.clamp((int) (skinCardW * 1.62F), 112, 168);
        float previewEntityScale = Mth.clamp(leftW / 168.0F, 0.68F, 1.08F);
        float cardEntityScale = Mth.clamp(characterCardW / 75.0F, 0.50F, 0.72F);
        float skinEntityScale = Mth.clamp(skinCardW / 84.0F, 0.56F, 0.80F);
        return new CharacterLayout(leftX, rightX, topY, leftW, rightW, totalH, backX, backY, backW, backH,
                previewX, previewY, previewW, previewH, detailButtonX, detailButtonY, detailButtonW, detailButtonH,
                gridX, gridY, gridW, gridH, characterCardW, characterCardH, mainTabX, mainTabY, mainTabW, mainTabH,
                subTabX, subTabY, subTabW, subTabH, bodyX, bodyY, bodyW, bodyH, skinCardW, skinCardH,
                previewEntityScale, cardEntityScale, skinEntityScale);
    }

}