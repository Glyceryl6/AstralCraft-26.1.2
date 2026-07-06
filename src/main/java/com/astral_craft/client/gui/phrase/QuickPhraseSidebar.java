package com.astral_craft.client.gui.phrase;

import com.astral_craft.client.gui.components.AstralFancyButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Reusable quick-phrase drawer used by the chat screen mixin and the standalone debug screen. */
@SuppressWarnings("SameParameterValue")
public class QuickPhraseSidebar {

    public static final int PANEL_WIDTH = 288;
    public static final int PANEL_MARGIN_RIGHT = 10;
    public static final int PANEL_MARGIN_TOP = 32;
    public static final int PANEL_MARGIN_BOTTOM = 38;
    public static final int ROW_HEIGHT = 24;
    public static final int SCROLLBAR_WIDTH = 5;
    public static final int TOGGLE_WIDTH = 88;
    public static final int TOGGLE_HEIGHT = 20;
    public static final int LEFT_RAIL_WIDTH = 76;
    public static final int TAB_HEIGHT = 20;
    public static final int SEND_BUTTON_WIDTH = 34;
    public static final int CLOSE_BUTTON_SIZE = 16;

    protected Tab activeTab = Tab.MOD;
    protected boolean expanded;

    protected int selectedModGroup;
    protected int selectedPlayerPhrase = -1;

    protected float phraseScrollY;
    protected float modTabScrollY;
    protected DragTarget dragTarget = DragTarget.NONE;
    protected double dragStartY;
    protected float dragStartScrollY;

    protected boolean addDialogOpen;
    protected String addDialogText = "";
    protected int addDialogCursor;

    protected boolean hoveringInteractiveButton;

    public void render(GuiGraphicsExtractor graphics, Font font, int screenWidth, int screenHeight, int mouseX, int mouseY) {
        this.hoveringInteractiveButton = false;
        this.normalizeSelection();
        this.clampScroll(screenHeight);
        this.renderToggle(graphics, font, screenWidth, screenHeight, mouseX, mouseY);
        if (!this.expanded) {
            AstralFancyButton.setHandCursor(this.hoveringInteractiveButton);
            return;
        }

        int x = this.panelX(screenWidth);
        int y = this.panelY();
        int h = this.panelHeight(screenHeight);
        int right = x + PANEL_WIDTH;
        int bottom = y + h;
        graphics.fill(x - 2, y - 2, right + 2, bottom + 2, 0xDD101018);
        graphics.fill(x, y, right, bottom, 0xE00B0B14);
        graphics.fill(x, y, right, y + 1, 0x80FFFFFF);
        graphics.fill(x, bottom - 1, right, bottom, 0x90000000);
        graphics.fill(x, y, x + 1, bottom, 0x60FFFFFF);
        graphics.fill(right - 1, y, right, bottom, 0x70000000);
        graphics.text(font, Component.translatable("gui.astral_craft.quick_phrases.title"), x + 10, y + 8, 0xFFFFFFFF, false);
        this.renderCloseButton(graphics, font, x + PANEL_WIDTH - CLOSE_BUTTON_SIZE - 7, y + 6, mouseX, mouseY);
        this.renderTopTabs(graphics, font, x, y, mouseX, mouseY);
        this.renderContent(graphics, font, x, y, h, screenHeight, mouseX, mouseY);
        if (this.addDialogOpen) {
            this.renderAddDialog(graphics, font, screenWidth, screenHeight, mouseX, mouseY);
        }

        AstralFancyButton.setHandCursor(this.hoveringInteractiveButton);
    }

    protected void renderTopTabs(GuiGraphicsExtractor graphics, Font font, int panelX, int panelY, int mouseX, int mouseY) {
        int y = panelY + 28;
        int xA = panelX + 8;
        int w = (PANEL_WIDTH - 22) / 2;
        int gap = 4;
        this.renderTabButton(graphics, font, Component.translatable("gui.astral_craft.quick_phrases.tab_mod"), xA, y, w, TAB_HEIGHT, this.activeTab == Tab.MOD, this.isInside(mouseX, mouseY, xA, y, w, TAB_HEIGHT));
        int xB = xA + w + gap;
        this.renderTabButton(graphics, font, Component.translatable("gui.astral_craft.quick_phrases.tab_player"), xB, y, w, TAB_HEIGHT, this.activeTab == Tab.PLAYER, this.isInside(mouseX, mouseY, xB, y, w, TAB_HEIGHT));
    }

    protected void renderContent(GuiGraphicsExtractor graphics, Font font, int x, int y, int h, int screenHeight, int mouseX, int mouseY) {
        int contentY = y + 54;
        int contentH = h - 64;
        if (this.activeTab == Tab.MOD) {
            this.renderModRail(graphics, font, x + 8, contentY, LEFT_RAIL_WIDTH - 12, contentH, screenHeight, mouseX, mouseY);
        } else {
            this.renderPlayerActions(graphics, font, x + 8, contentY, LEFT_RAIL_WIDTH - 12, contentH, mouseX, mouseY);
        }

        int listX = x + LEFT_RAIL_WIDTH + 8;
        int listW = PANEL_WIDTH - LEFT_RAIL_WIDTH - 18;
        this.renderPhraseList(graphics, font, listX, contentY, listW, contentH, screenHeight, mouseX, mouseY);
    }

    protected void renderCloseButton(GuiGraphicsExtractor graphics, Font font, int x, int y, int mouseX, int mouseY) {
        boolean hovered = this.isInside(mouseX, mouseY, x, y, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE);
        this.renderButton(graphics, font, Component.literal("X"), x, y, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE, false, hovered);
    }

    protected void renderModRail(GuiGraphicsExtractor graphics, Font font, int x, int y, int w, int h, int screenHeight, int mouseX, int mouseY) {
        List<QuickPhraseData.ModPhraseGroup> groups = QuickPhraseData.modGroups();
        graphics.fill(x, y, x + w, y + h, 0x66000000);
        if (groups.isEmpty()) {
            graphics.text(font, Component.translatable("gui.astral_craft.quick_phrases.no_mods"), x + 6, y + 8, 0xFFB8B8C8, false);
            return;
        }

        int innerW = w - SCROLLBAR_WIDTH - 2;
        graphics.enableScissor(x, y, x + w, y + h);
        for (int i = 0; i < groups.size(); i++) {
            int rowY = y + i * ROW_HEIGHT - Math.round(this.modTabScrollY);
            if (rowY + ROW_HEIGHT < y || rowY > y + h) continue;
            boolean selected = i == this.selectedModGroup;
            boolean hovered = this.isInside(mouseX, mouseY, x, rowY, innerW, ROW_HEIGHT - 2);
            Component label = this.ellipsize(font, QuickPhraseData.modDisplayName(groups.get(i)), innerW - 8);
            this.renderSelectableButton(graphics, font, label, x + 1, rowY, innerW - 1, ROW_HEIGHT - 2, selected, hovered);
        }

        graphics.disableScissor();
        this.renderScrollbar(graphics, x + w - SCROLLBAR_WIDTH, y, h, this.modTabScrollY, this.maxModTabScroll(screenHeight), groups.size(), this.dragTarget == DragTarget.MOD_TABS);
    }

    protected void renderPlayerActions(GuiGraphicsExtractor graphics, Font font, int x, int y, int w, int h, int mouseX, int mouseY) {
        int btnH = 24;
        this.renderButton(graphics, font, Component.translatable("gui.astral_craft.quick_phrases.add"), x, y, w, btnH, false, this.isInside(mouseX, mouseY, x, y, w, btnH));
        int delY = y + btnH + 6;
        boolean canDelete = this.selectedPlayerPhrase >= 0 && this.selectedPlayerPhrase < PlayerQuickPhraseConfig.phrases().size();
        this.renderButton(graphics, font, Component.translatable("gui.astral_craft.quick_phrases.delete"), x, delY, w, btnH, canDelete, this.isInside(mouseX, mouseY, x, delY, w, btnH));
        graphics.text(font, this.ellipsize(font, Component.translatable("gui.astral_craft.quick_phrases.local_hint"), w - 4), x + 2, y + h - 14, 0xFFB8B8C8, false);
    }

    protected void renderPhraseList(GuiGraphicsExtractor graphics, Font font, int x, int y, int w, int h, int screenHeight, int mouseX, int mouseY) {
        List<DisplayPhrase> phrases = this.currentPhrases();
        graphics.fill(x, y, x + w, y + h, 0x55000000);
        if (phrases.isEmpty()) {
            graphics.text(font, Component.translatable("gui.astral_craft.quick_phrases.empty"), x + 8, y + 8, 0xFFB8B8C8, false);
            return;
        }

        int rowW = w - SCROLLBAR_WIDTH - 4;
        int textW = rowW - SEND_BUTTON_WIDTH - 12;
        graphics.enableScissor(x, y, x + w, y + h);
        for (int i = 0; i < phrases.size(); i++) {
            int rowY = y + i * ROW_HEIGHT - Math.round(this.phraseScrollY);
            if (rowY + ROW_HEIGHT < y || rowY > y + h) continue;
            boolean selected = this.activeTab == Tab.PLAYER && i == this.selectedPlayerPhrase;
            boolean hovered = this.isInside(mouseX, mouseY, x, rowY, rowW, ROW_HEIGHT - 2);
            int fill = selected ? 0xCC92FF22 : hovered ? 0x884AFF20 : 0x663A3A48;
            graphics.fill(x + 1, rowY, x + rowW, rowY + ROW_HEIGHT - 2, fill);
            graphics.text(font, this.ellipsize(font, phrases.get(i).component(), textW), x + 7, rowY + 8, selected || hovered ? 0xFF101018 : 0xFFEFEFFF, false);
            int sendX = x + rowW - SEND_BUTTON_WIDTH - 2;
            boolean sendHovered = this.isInside(mouseX, mouseY, sendX, rowY + 2, SEND_BUTTON_WIDTH, ROW_HEIGHT - 6);
            this.renderButton(graphics, font, Component.translatable("gui.astral_craft.quick_phrases.send"), sendX, rowY + 2, SEND_BUTTON_WIDTH, ROW_HEIGHT - 6, false, sendHovered);
        }

        graphics.disableScissor();
        this.renderScrollbar(graphics, x + w - SCROLLBAR_WIDTH, y, h, this.phraseScrollY, this.maxPhraseScroll(screenHeight), phrases.size(), this.dragTarget == DragTarget.PHRASES);
    }

    protected void renderAddDialog(GuiGraphicsExtractor graphics, Font font, int screenWidth, int screenHeight, int mouseX, int mouseY) {
        graphics.fill(0, 0, screenWidth, screenHeight, 0x88000000);
        int w = Math.min(300, screenWidth - 40);
        int h = 118;
        int x = (screenWidth - w) / 2;
        int y = (screenHeight - h) / 2;
        graphics.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xEE101018);
        graphics.fill(x, y, x + w, y + h, 0xF0181824);
        graphics.text(font, Component.translatable("gui.astral_craft.quick_phrases.add_title"), x + 10, y + 10, 0xFFFFFFFF, false);
        this.renderCloseButton(graphics, font, x + w - CLOSE_BUTTON_SIZE - 8, y + 8, mouseX, mouseY);
        int inputX = x + 10;
        int inputY = y + 34;
        int inputW = w - 20;
        int inputH = 22;
        graphics.fill(inputX, inputY, inputX + inputW, inputY + inputH, 0xFF090910);
        graphics.fill(inputX, inputY, inputX + inputW, inputY + 1, 0xAAFFFFFF);
        Component display = this.addDialogText.isEmpty() ? Component.translatable("gui.astral_craft.quick_phrases.input_hint") : Component.literal(this.addDialogText);
        int textColor = this.addDialogText.isEmpty() ? 0xFF777788 : 0xFFFFFFFF;
        graphics.text(font, display, inputX + 5, inputY + 7, textColor, false);
        int cursorX = inputX + 5 + font.width(this.addDialogText.substring(0, Math.min(this.addDialogCursor, this.addDialogText.length())));
        if ((System.currentTimeMillis() / 500L) % 2L == 0L) {
            graphics.fill(cursorX, inputY + 5, cursorX + 1, inputY + inputH - 5, 0xFFFFFFFF);
        }

        int btnW = 72;
        int btnY = y + h - 34;
        int saveX = x + w - btnW * 2 - 18;
        int cancelX = x + w - btnW - 10;
        this.renderButton(graphics, font, Component.translatable("gui.astral_craft.quick_phrases.save"), saveX, btnY, btnW, 22, false, this.isInside(mouseX, mouseY, saveX, btnY, btnW, 22));
        this.renderButton(graphics, font, Component.translatable("gui.astral_craft.quick_phrases.cancel"), cancelX, btnY, btnW, 22, false, this.isInside(mouseX, mouseY, cancelX, btnY, btnW, 22));
    }

    protected void renderToggle(GuiGraphicsExtractor graphics, Font font, int screenWidth, int screenHeight, int mouseX, int mouseY) {
        int x = this.toggleX(screenWidth);
        int y = this.toggleY(screenHeight);
        boolean hovered = this.isInside(mouseX, mouseY, x, y, TOGGLE_WIDTH, TOGGLE_HEIGHT);
        this.markButtonHover(hovered);
        Component label = Component.translatable("gui.astral_craft.quick_phrases.open_button");
        AstralFancyButton.ButtonStyle style = this.expanded
                ? AstralFancyButton.selectedButtonStyle().withBoxMetrics(2, 2, 2, 2)
                : AstralFancyButton.pinkButtonStyle().withBoxMetrics(2, 2, 2, 2);
        AstralFancyButton.renderButton(graphics, font, label, x, y, TOGGLE_WIDTH, TOGGLE_HEIGHT, this.expanded, hovered, style);
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick, int screenWidth, int screenHeight) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        double mx = event.x();
        double my = event.y();

        if (this.addDialogOpen) {
            return this.handleDialogClick(mx, my, screenWidth, screenHeight);
        }

        int toggleX = this.toggleX(screenWidth);
        int toggleY = this.toggleY(screenHeight);
        if (this.isInside(mx, my, toggleX, toggleY, TOGGLE_WIDTH, TOGGLE_HEIGHT)) {
            this.expanded = !this.expanded;
            return true;
        }

        if (!this.expanded) return false;
        int x = this.panelX(screenWidth);
        int y = this.panelY();
        int h = this.panelHeight(screenHeight);
        if (!this.isInside(mx, my, x, y, PANEL_WIDTH, h)) {
            this.expanded = false;
            this.dragTarget = DragTarget.NONE;
            return false;
        }

        if (this.isInside(mx, my, x + PANEL_WIDTH - CLOSE_BUTTON_SIZE - 7, y + 6, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)) {
            this.expanded = false;
            this.dragTarget = DragTarget.NONE;
            return true;
        }

        int contentY = y + 54;
        int contentH = h - 64;
        int tabY = y + 28;
        int tabW = (PANEL_WIDTH - 22) / 2;
        int tabAX = x + 8;
        int tabBX = tabAX + tabW + 4;
        if (this.isInside(mx, my, tabAX, tabY, tabW, TAB_HEIGHT)) {
            this.activeTab = Tab.MOD;
            this.phraseScrollY = 0.0F;
            return true;
        }

        if (this.isInside(mx, my, tabBX, tabY, tabW, TAB_HEIGHT)) {
            this.activeTab = Tab.PLAYER;
            this.phraseScrollY = 0.0F;
            return true;
        }

        if (this.activeTab == Tab.MOD && this.handleModRailClick(mx, my, x + 8, contentY, LEFT_RAIL_WIDTH - 12, contentH, screenHeight)) {
            return true;
        }

        if (this.activeTab == Tab.PLAYER && this.handlePlayerActionClick(mx, my, x + 8, contentY, LEFT_RAIL_WIDTH - 12)) {
            return true;
        }

        int listX = x + LEFT_RAIL_WIDTH + 8;
        int listW = PANEL_WIDTH - LEFT_RAIL_WIDTH - 18;
        return this.handlePhraseListClick(mx, my, listX, contentY, listW, contentH, screenHeight, doubleClick) || this.isInside(mx, my, x, y, PANEL_WIDTH, h);
    }

    protected boolean handleDialogClick(double mx, double my, int screenWidth, int screenHeight) {
        int w = Math.min(300, screenWidth - 40);
        int h = 118;
        int x = (screenWidth - w) / 2;
        int y = (screenHeight - h) / 2;
        if (!this.isInside(mx, my, x, y, w, h)) {
            this.closeDialog();
            return true;
        }

        if (this.isInside(mx, my, x + w - CLOSE_BUTTON_SIZE - 8, y + 8, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)) {
            this.closeDialog();
            return true;
        }

        int btnW = 72;
        int btnY = y + h - 34;
        int saveX = x + w - btnW * 2 - 18;
        int cancelX = x + w - btnW - 10;
        if (this.isInside(mx, my, saveX, btnY, btnW, 22)) {
            this.saveDialogPhrase();
            return true;
        }

        if (this.isInside(mx, my, cancelX, btnY, btnW, 22)) {
            this.closeDialog();
            return true;
        }

        return true;
    }

    protected boolean handleModRailClick(double mx, double my, int x, int y, int w, int h, int screenHeight) {
        List<QuickPhraseData.ModPhraseGroup> groups = QuickPhraseData.modGroups();
        if (groups.isEmpty()) return false;
        if (this.isScrollbarVisible(this.maxModTabScroll(screenHeight)) && mx >= x + w - SCROLLBAR_WIDTH && mx <= x + w && my >= y && my <= y + h) {
            this.startDrag(DragTarget.MOD_TABS, my);
            return true;
        }

        if (this.isInside(mx, my, x, y, w - SCROLLBAR_WIDTH - 2, h)) {
            int index = (int) ((my - y + this.modTabScrollY) / ROW_HEIGHT);
            if (index >= 0 && index < groups.size()) {
                this.selectedModGroup = index;
                this.phraseScrollY = 0.0F;
                return true;
            }
        }

        return false;
    }

    protected boolean handlePlayerActionClick(double mx, double my, int x, int y, int w) {
        if (this.isInside(mx, my, x, y, w, 24)) {
            this.addDialogOpen = true;
            this.addDialogText = "";
            this.addDialogCursor = 0;
            return true;
        }

        if (this.isInside(mx, my, x, y + 30, w, 24)) {
            PlayerQuickPhraseConfig.delete(this.selectedPlayerPhrase);
            this.selectedPlayerPhrase = Math.min(this.selectedPlayerPhrase, PlayerQuickPhraseConfig.phrases().size() - 1);
            return true;
        }

        return false;
    }

    protected boolean handlePhraseListClick(double mx, double my, int x, int y, int w, int h, int screenHeight, boolean doubleClick) {
        if (this.isScrollbarVisible(this.maxPhraseScroll(screenHeight)) && mx >= x + w - SCROLLBAR_WIDTH && mx <= x + w && my >= y && my <= y + h) {
            this.startDrag(DragTarget.PHRASES, my);
            return true;
        }

        if (this.isInside(mx, my, x, y, w - SCROLLBAR_WIDTH - 4, h)) {
            int index = (int) ((my - y + this.phraseScrollY) / ROW_HEIGHT);
            List<DisplayPhrase> phrases = this.currentPhrases();
            if (index >= 0 && index < phrases.size()) {
                int rowY = y + index * ROW_HEIGHT - Math.round(this.phraseScrollY);
                int rowW = w - SCROLLBAR_WIDTH - 4;
                int sendX = x + rowW - SEND_BUTTON_WIDTH - 2;
                if (this.isInside(mx, my, sendX, rowY + 2, SEND_BUTTON_WIDTH, ROW_HEIGHT - 6)) {
                    this.sendPhrase(phrases.get(index).component());
                    this.expanded = false;
                    return true;
                }

                if (this.activeTab == Tab.PLAYER) {
                    this.selectedPlayerPhrase = index;
                }

                if (doubleClick) {
                    this.sendPhrase(phrases.get(index).component());
                    this.expanded = false;
                }

                return true;
            }
        }

        return false;
    }

    public boolean mouseDragged(MouseButtonEvent event, int screenHeight) {
        if (this.dragTarget == DragTarget.NONE) return false;
        float maxScroll = this.dragTarget == DragTarget.MOD_TABS ? this.maxModTabScroll(screenHeight) : this.maxPhraseScroll(screenHeight);
        int listH = this.panelHeight(screenHeight) - 64;
        int thumbH = this.scrollbarThumbHeight(maxScroll, listH, this.dragTarget == DragTarget.MOD_TABS ? QuickPhraseData.modGroups().size() : this.currentPhrases().size());
        int movable = Math.max(1, listH - thumbH);
        float next = Mth.clamp(this.dragStartScrollY + (float) ((event.y() - this.dragStartY) / movable * maxScroll), 0.0F, maxScroll);
        if (this.dragTarget == DragTarget.MOD_TABS) {
            this.modTabScrollY = next;
        } else {
            this.phraseScrollY = next;
        }

        return true;
    }

    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.dragTarget != DragTarget.NONE) {
            this.dragTarget = DragTarget.NONE;
            return true;
        }

        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double deltaY, int screenWidth, int screenHeight) {
        if (!this.expanded || this.addDialogOpen) return false;
        int x = this.panelX(screenWidth);
        int y = this.panelY();
        int contentY = y + 54;
        int contentH = this.panelHeight(screenHeight) - 64;
        int listX = x + LEFT_RAIL_WIDTH + 8;
        int listW = PANEL_WIDTH - LEFT_RAIL_WIDTH - 18;
        if (this.isInside(mouseX, mouseY, listX, contentY, listW, contentH)) {
            this.phraseScrollY = Mth.clamp(this.phraseScrollY - (float) deltaY * 24.0F, 0.0F, this.maxPhraseScroll(screenHeight));
            return true;
        }

        if (this.activeTab == Tab.MOD && this.isInside(mouseX, mouseY, x + 8, contentY, LEFT_RAIL_WIDTH - 12, contentH)) {
            this.modTabScrollY = Mth.clamp(this.modTabScrollY - (float) deltaY * 24.0F, 0.0F, this.maxModTabScroll(screenHeight));
            return true;
        }

        return this.isInside(mouseX, mouseY, x, y, PANEL_WIDTH, this.panelHeight(screenHeight));
    }

    public boolean keyPressed(KeyEvent event) {
        if (!this.addDialogOpen) return false;
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            this.closeDialog();
            return true;
        }

        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            this.saveDialogPhrase();
            return true;
        }

        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (this.addDialogCursor > 0 && !this.addDialogText.isEmpty()) {
                this.addDialogText = this.addDialogText.substring(0, this.addDialogCursor - 1) + this.addDialogText.substring(this.addDialogCursor);
                this.addDialogCursor--;
            }
            return true;
        }

        if (key == GLFW.GLFW_KEY_DELETE) {
            if (this.addDialogCursor < this.addDialogText.length()) {
                this.addDialogText = this.addDialogText.substring(0, this.addDialogCursor) + this.addDialogText.substring(this.addDialogCursor + 1);
            }
            return true;
        }

        if (key == GLFW.GLFW_KEY_LEFT) {
            this.addDialogCursor = Math.max(0, this.addDialogCursor - 1);
            return true;
        }

        if (key == GLFW.GLFW_KEY_RIGHT) {
            this.addDialogCursor = Math.min(this.addDialogText.length(), this.addDialogCursor + 1);
            return true;
        }

        if (key == GLFW.GLFW_KEY_HOME) {
            this.addDialogCursor = 0;
            return true;
        }

        if (key == GLFW.GLFW_KEY_END) {
            this.addDialogCursor = this.addDialogText.length();
            return true;
        }

        return true;
    }

    public boolean charTyped(int codePoint) {
        if (!this.addDialogOpen) return false;
        if (Character.isISOControl(codePoint)) return true;
        String text = new String(Character.toChars(codePoint));
        if (this.addDialogText.length() + text.length() <= 256) {
            this.addDialogText = this.addDialogText.substring(0, this.addDialogCursor) + text + this.addDialogText.substring(this.addDialogCursor);
            this.addDialogCursor += text.length();
        }

        return true;
    }

    protected void startDrag(DragTarget target, double mouseY) {
        this.dragTarget = target;
        this.dragStartY = mouseY;
        this.dragStartScrollY = target == DragTarget.MOD_TABS ? this.modTabScrollY : this.phraseScrollY;
    }

    protected void saveDialogPhrase() {
        PlayerQuickPhraseConfig.add(this.addDialogText);
        this.selectedPlayerPhrase = PlayerQuickPhraseConfig.phrases().size() - 1;
        this.closeDialog();
    }

    protected void closeDialog() {
        this.addDialogOpen = false;
        this.addDialogText = "";
        this.addDialogCursor = 0;
    }

    protected List<DisplayPhrase> currentPhrases() {
        if (this.activeTab == Tab.PLAYER) {
            List<String> raw = PlayerQuickPhraseConfig.phrases();
            List<DisplayPhrase> out = new ArrayList<>(raw.size());
            for (String phrase : raw) {
                out.add(new DisplayPhrase(Component.literal(phrase)));
            }

            return out;
        }

        List<QuickPhraseData.ModPhraseGroup> groups = QuickPhraseData.modGroups();
        if (groups.isEmpty()) return List.of();
        int index = Mth.clamp(this.selectedModGroup, 0, groups.size() - 1);
        List<QuickPhraseData.Phrase> phrases = groups.get(index).phrases();
        List<DisplayPhrase> out = new ArrayList<>(phrases.size());
        for (QuickPhraseData.Phrase phrase : phrases) {
            out.add(new DisplayPhrase(QuickPhraseData.phraseComponent(phrase)));
        }

        return out;
    }

    protected void sendPhrase(Component phrase) {
        String text = phrase.getString();
        if (text.isBlank()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.connection.sendChat(text);
        }
    }

    protected Component ellipsize(Font font, Component input, int maxWidth) {
        String text = input.getString();
        if (font.width(text) <= maxWidth) return input;
        String suffix = "...";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (font.width(out.toString()) + font.width(suffix) >= maxWidth) break;
            out.append(text.charAt(i));
        }

        return Component.literal(out + suffix);
    }

    public boolean isExpanded() {
        return this.expanded;
    }

    protected void normalizeSelection() {
        List<QuickPhraseData.ModPhraseGroup> groups = QuickPhraseData.modGroups();
        this.selectedModGroup = groups.isEmpty() ? 0 : Mth.clamp(this.selectedModGroup, 0, groups.size() - 1);
        int playerSize = PlayerQuickPhraseConfig.phrases().size();
        if (playerSize == 0) {
            this.selectedPlayerPhrase = -1;
        } else if (this.selectedPlayerPhrase >= playerSize) {
            this.selectedPlayerPhrase = playerSize - 1;
        }
    }

    protected int panelX(int screenWidth) {
        return screenWidth - PANEL_MARGIN_RIGHT - PANEL_WIDTH;
    }

    protected int panelY() {
        return PANEL_MARGIN_TOP;
    }

    protected int panelHeight(int screenHeight) {
        return Math.max(132, screenHeight - PANEL_MARGIN_TOP - PANEL_MARGIN_BOTTOM);
    }

    protected int toggleX(int screenWidth) {
        return screenWidth - PANEL_MARGIN_RIGHT - TOGGLE_WIDTH;
    }

    protected int toggleY(int screenHeight) {
        return Math.max(28, screenHeight - 44);
    }

    protected float maxPhraseScroll(int screenHeight) {
        return Math.max(0.0F, this.currentPhrases().size() * ROW_HEIGHT - (this.panelHeight(screenHeight) - 64));
    }

    protected float maxModTabScroll(int screenHeight) {
        return Math.max(0.0F, QuickPhraseData.modGroups().size() * ROW_HEIGHT - (this.panelHeight(screenHeight) - 64));
    }

    protected void clampScroll(int screenHeight) {
        this.phraseScrollY = Mth.clamp(this.phraseScrollY, 0.0F, this.maxPhraseScroll(screenHeight));
        this.modTabScrollY = Mth.clamp(this.modTabScrollY, 0.0F, this.maxModTabScroll(screenHeight));
    }

    protected boolean isScrollbarVisible(float maxScroll) {
        return maxScroll > 0.0F;
    }

    protected int scrollbarThumbHeight(float maxScroll, int listH, int rows) {
        return Mth.clamp(Math.round(listH * (listH / (float) Math.max(listH, rows * ROW_HEIGHT))), 18, listH);
    }

    protected void renderScrollbar(GuiGraphicsExtractor graphics, int x, int y, int h, float scroll, float maxScroll, int rows, boolean active) {
        if (!this.isScrollbarVisible(maxScroll)) return;
        int thumbH = this.scrollbarThumbHeight(maxScroll, h, rows);
        int thumbY = y + Math.round((h - thumbH) * (scroll / maxScroll));
        graphics.fill(x, y, x + SCROLLBAR_WIDTH, y + h, 0x66000000);
        graphics.fill(x, thumbY, x + SCROLLBAR_WIDTH, thumbY + thumbH, active ? 0xFFE83CA8 : 0xCCFFFFFF);
    }

    protected void renderTabButton(GuiGraphicsExtractor graphics, Font font, Component label, int x, int y, int w, int h, boolean selected, boolean hovered) {
        this.renderSelectableButton(graphics, font, label, x, y, w, h, selected, hovered);
    }

    protected void renderButton(GuiGraphicsExtractor graphics, Font font, Component label, int x, int y, int w, int h, boolean selected, boolean hovered) {
        this.renderSelectableButton(graphics, font, label, x, y, w, h, selected, hovered);
    }

    protected void renderSelectableButton(GuiGraphicsExtractor graphics, Font font, Component label, int x, int y, int w, int h, boolean selected, boolean hovered) {
        this.markButtonHover(hovered);
        AstralFancyButton.ButtonStyle style = selected
                ? AstralFancyButton.selectedButtonStyle().withBoxMetrics(2, 2, 2, 2)
                : AstralFancyButton.pinkButtonStyle().withBoxMetrics(2, 2, 2, 2);
        AstralFancyButton.renderButton(graphics, font, label, x, y, w, h, selected, hovered, style);
    }

    protected void markButtonHover(boolean hovered) {
        if (hovered) {
            this.hoveringInteractiveButton = true;
        }
    }

    protected boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    protected enum Tab { MOD, PLAYER }

    protected enum DragTarget { NONE, MOD_TABS, PHRASES }

    protected record DisplayPhrase(Component component) {}

}