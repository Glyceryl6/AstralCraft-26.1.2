package com.astral_craft.client.gui.appearance;

import com.astral_craft.client.gui.board.BoardScreenEntityRenderer;
import com.astral_craft.client.gui.cardback.CardBackDefinition;
import com.astral_craft.client.gui.cardback.CardBackResourceCache;
import com.astral_craft.client.gui.components.AstralFancyButton;
import com.astral_craft.client.gui.components.AstralFancyButton.ButtonStyle;
import com.astral_craft.client.jpgloader.ScopedJpgTextureCache;
import com.astral_craft.client.util.ClientAnimationClock;
import com.astral_craft.common.entity.AstralDiceEntity;
import com.astral_craft.common.network.c2s.CardBackSelectionPayload;
import com.astral_craft.common.network.s2c.OpenCardBackSelectionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AppearanceSelectionScreen extends Screen {

    private static final int MAX_PANEL_WIDTH = 540;
    private static final int MAX_PANEL_HEIGHT = 420;
    private static final int CARD_W = 64;
    private static final int CARD_H = 90;
    private static final int CARD_LABEL_H = 18;
    private static final int CARD_GAP = 12;
    private static final int HEADER_H = 22;
    private static final int BUTTON_W = 112;
    private static final int BUTTON_H = 30;
    private static final int TAB_W = 112;
    private static final int TAB_H = 22;
    private static final int LIST_TOP = 64;
    private static final int LIST_BOTTOM = 56;

    private final List<CardBackDefinition> cardBackDefinitions;
    private final List<CardBackDefinition> diceDefinitions;
    private final AstralDiceEntity previewDice;
    private Identifier selectedCardBack;
    private Identifier selectedDiceSkin;
    private AppearanceTab activeTab = AppearanceTab.CARD_BACK;
    private List<NamespaceSection> sections;
    private float scrollY;
    private boolean draggingScrollbar;
    private double dragStartY;
    private float dragStartScrollY;

    public AppearanceSelectionScreen(Identifier selectedCardBack, Identifier selectedDiceSkin) {
        super(Component.translatable("gui.astral_craft.appearance_selection.title"));
        this.cardBackDefinitions = CardBackResourceCache.values();
        this.diceDefinitions = CardBackResourceCache.diceSkins();
        this.selectedCardBack = resolveSelection(this.cardBackDefinitions, selectedCardBack);
        this.selectedDiceSkin = resolveSelection(this.diceDefinitions, selectedDiceSkin);
        this.sections = groupByNamespace(this.cardBackDefinitions);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            this.previewDice = null;
        } else {
            this.previewDice = new AstralDiceEntity(minecraft.level, 0.0D, 0.0D, 0.0D);
            this.previewDice.startRoll(1, 10, 1, 1.0F, 10, 10, 0, true, 0.0F, 0.0F);
        }
    }

    public static void open(OpenCardBackSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(
                new AppearanceSelectionScreen(payload.selectedCardBackId(), payload.selectedDiceSkinId())));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.scrollY = Mth.clamp(this.scrollY, 0.0F, this.maxScroll());
        int x = this.panelX();
        int y = this.panelY();
        int width = this.panelWidth();
        int height = this.panelHeight();
        int right = x + width;
        int bottom = y + height;
        graphics.fill(x, y, right, bottom, 0xE00A0A12);
        graphics.fill(x, y, right, y + 2, 0x80FFFFFF);
        graphics.text(this.font, this.title, x + 16, y + 11, 0xFFFFFFFF, false);
        this.renderTabs(graphics, mouseX, mouseY);
        int listX = x + 16;
        int listY = y + LIST_TOP;
        int listW = width - 32;
        int listH = height - LIST_TOP - LIST_BOTTOM;
        graphics.enableScissor(listX, listY, listX + listW, listY + listH);
        this.renderSections(graphics, listX, listY, listW, mouseX, mouseY);
        graphics.disableScissor();
        this.renderScrollbar(graphics, listX + listW - 5, listY, listH);
        int confirmX = right - 16 - BUTTON_W * 2 - 10;
        int buttonY = bottom - 42;
        boolean confirmHovered = inside(mouseX, mouseY, confirmX, buttonY, BUTTON_W, BUTTON_H);
        boolean cancelHovered = inside(mouseX, mouseY, confirmX + BUTTON_W + 10, buttonY, BUTTON_W, BUTTON_H);
        AstralFancyButton.renderButton(graphics, this.font,
                Component.translatable("gui.astral_craft.card_back_selection.confirm"),
                confirmX, buttonY, BUTTON_W, BUTTON_H, false, confirmHovered, ButtonStyle.button(0xFF4F9D69));
        AstralFancyButton.renderButton(graphics, this.font,
                Component.translatable("gui.astral_craft.card_back_selection.cancel"),
                confirmX + BUTTON_W + 10, buttonY, BUTTON_W, BUTTON_H, false, cancelHovered,
                ButtonStyle.button(0xFF9B5360));
        AstralFancyButton.setHandCursor(confirmHovered || cancelHovered || this.hoveredEntry(mouseX, mouseY)
                || this.tabAt(mouseX, mouseY) != null);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        AppearanceTab clickedTab = this.tabAt(event.x(), event.y());
        if (clickedTab != null) {
            this.selectTab(clickedTab);
            return true;
        }

        int listX = this.panelX() + 16;
        int listY = this.panelY() + LIST_TOP;
        int listW = this.panelWidth() - 32;
        int listH = this.panelHeight() - LIST_TOP - LIST_BOTTOM;
        int scrollbarX = listX + listW - 5;
        if (this.maxScroll() > 0.0F && inside(event.x(), event.y(), scrollbarX - 2, listY, 9, listH)) {
            this.draggingScrollbar = true;
            this.dragStartY = event.y();
            this.dragStartScrollY = this.scrollY;
            return true;
        }

        CardBackDefinition clicked = this.entryAt(event.x(), event.y());
        if (clicked != null) {
            if (this.activeTab == AppearanceTab.CARD_BACK) this.selectedCardBack = clicked.id();
            else this.selectedDiceSkin = clicked.id();
            return true;
        }

        int x = this.panelX();
        int y = this.panelY();
        int width = this.panelWidth();
        int height = this.panelHeight();
        int confirmX = x + width - 16 - BUTTON_W * 2 - 10;
        int buttonY = y + height - 42;
        if (inside(event.x(), event.y(), confirmX, buttonY, BUTTON_W, BUTTON_H)) {
            ClientPacketDistributor.sendToServer(new CardBackSelectionPayload(this.selectedCardBack, this.selectedDiceSkin));
            this.onClose();
            return true;
        }

        if (inside(event.x(), event.y(), confirmX + BUTTON_W + 10, buttonY, BUTTON_W, BUTTON_H)) {
            this.onClose();
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.draggingScrollbar) {
            int listH = this.panelHeight() - LIST_TOP - LIST_BOTTOM;
            int thumbH = this.scrollbarThumbHeight(listH);
            int movable = Math.max(1, listH - thumbH);
            this.scrollY = Mth.clamp(this.dragStartScrollY
                    + (float) ((event.y() - this.dragStartY) / movable * this.maxScroll()), 0.0F, this.maxScroll());
            return true;
        }

        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && this.draggingScrollbar) {
            this.draggingScrollbar = false;
            return true;
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        int listX = this.panelX() + 16;
        int listY = this.panelY() + LIST_TOP;
        int listW = this.panelWidth() - 32;
        int listH = this.panelHeight() - LIST_TOP - LIST_BOTTOM;
        if (inside(mouseX, mouseY, listX, listY, listW, listH)) {
            this.scrollY = Mth.clamp(this.scrollY - (float) deltaY * 34.0F, 0.0F, this.maxScroll());
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public void removed() {
        AstralFancyButton.setHandCursor(false);
        super.removed();
    }

    private void renderTabs(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = this.panelX() + 16;
        int y = this.panelY() + 34;
        for (AppearanceTab tab : AppearanceTab.values()) {
            boolean hovered = inside(mouseX, mouseY, x, y, TAB_W, TAB_H);
            boolean selected = tab == this.activeTab;
            AstralFancyButton.renderButton(graphics, this.font, Component.translatable(tab.translationKey),
                    x, y, TAB_W, TAB_H, false, hovered, ButtonStyle.button(selected ? 0xFF8B68C9 : 0xFF4A4A5A));
            x += TAB_W + 8;
        }
    }

    private void renderSections(GuiGraphicsExtractor graphics, int listX, int listY, int listW, int mouseX, int mouseY) {
        int columns = this.columns(listW);
        int cursorY = listY - Math.round(this.scrollY);
        for (NamespaceSection section : this.sections) {
            graphics.text(this.font, Component.literal(section.namespace()), listX + 4, cursorY + 6, 0xFFFFD27D, false);
            cursorY += HEADER_H;
            for (int index = 0; index < section.definitions().size(); index++) {
                int column = index % columns;
                int row = index / columns;
                int entryX = listX + 4 + column * (CARD_W + CARD_GAP);
                int entryY = cursorY + row * (CARD_H + CARD_LABEL_H + CARD_GAP);
                CardBackDefinition definition = section.definitions().get(index);
                boolean hovered = inside(mouseX, mouseY, entryX - 3, entryY - 3, CARD_W + 6, CARD_H + CARD_LABEL_H + 6);
                boolean chosen = definition.id().equals(this.activeSelection());
                graphics.fill(entryX - 3, entryY - 3, entryX + CARD_W + 3, entryY + CARD_H + CARD_LABEL_H + 3,
                        chosen ? 0xAAFFF0A0 : hovered ? 0x665C5C72 : 0x44202028);
                if (this.activeTab == AppearanceTab.CARD_BACK) {
                    graphics.blit(RenderPipelines.GUI_TEXTURED, ScopedJpgTextureCache.resolve(definition.texture()), entryX, entryY,
                            0.0F, 0.0F, CARD_W, CARD_H, 256, 360, 256, 360, 0xFFFFFFFF);
                } else {
                    this.renderDicePreview(graphics, definition, entryX, entryY);
                }
                Component name = this.ellipsize(this.displayName(definition), CARD_W + 4);
                graphics.text(this.font, name, entryX + CARD_W / 2 - this.font.width(name) / 2,
                        entryY + CARD_H + 5, 0xFFFFFFFF, false);
            }
            cursorY += this.rows(section.definitions().size(), columns) * (CARD_H + CARD_LABEL_H + CARD_GAP) + 8;
        }
    }

    private void renderDicePreview(GuiGraphicsExtractor graphics, CardBackDefinition definition, int x, int y) {
        graphics.fill(x, y, x + CARD_W, y + CARD_H, 0xAA15151D);
        if (this.previewDice == null) return;
        this.previewDice.setTexture(definition.texture());
        float yaw = (float) (ClientAnimationClock.nowTicks() * 2.0D % 360.0D);
        BoardScreenEntityRenderer.render(graphics, this.previewDice, x + 3, y + 8, x + CARD_W - 3, y + CARD_H - 4,
                yaw, 1.12F, 0.0F, 0.0F, 0.0F);
    }

    private void selectTab(AppearanceTab tab) {
        if (tab == this.activeTab) return;
        this.activeTab = tab;
        this.sections = groupByNamespace(tab == AppearanceTab.CARD_BACK ? this.cardBackDefinitions : this.diceDefinitions);
        this.scrollY = 0.0F;
        this.draggingScrollbar = false;
    }

    private AppearanceTab tabAt(double mouseX, double mouseY) {
        int x = this.panelX() + 16;
        int y = this.panelY() + 34;
        for (AppearanceTab tab : AppearanceTab.values()) {
            if (inside(mouseX, mouseY, x, y, TAB_W, TAB_H)) return tab;
            x += TAB_W + 8;
        }
        return null;
    }

    private CardBackDefinition entryAt(double mouseX, double mouseY) {
        int listX = this.panelX() + 16;
        int listY = this.panelY() + LIST_TOP;
        int listW = this.panelWidth() - 32;
        int listH = this.panelHeight() - LIST_TOP - LIST_BOTTOM;
        if (!inside(mouseX, mouseY, listX, listY, listW, listH)) return null;
        int columns = this.columns(listW);
        int cursorY = listY - Math.round(this.scrollY);
        for (NamespaceSection section : this.sections) {
            cursorY += HEADER_H;
            for (int index = 0; index < section.definitions().size(); index++) {
                int entryX = listX + 4 + index % columns * (CARD_W + CARD_GAP);
                int entryY = cursorY + index / columns * (CARD_H + CARD_LABEL_H + CARD_GAP);
                if (inside(mouseX, mouseY, entryX - 3, entryY - 3, CARD_W + 6, CARD_H + CARD_LABEL_H + 6)) {
                    return section.definitions().get(index);
                }
            }

            cursorY += this.rows(section.definitions().size(), columns) * (CARD_H + CARD_LABEL_H + CARD_GAP) + 8;
        }

        return null;
    }

    private boolean hoveredEntry(double mouseX, double mouseY) {
        return this.entryAt(mouseX, mouseY) != null;
    }

    private Identifier activeSelection() {
        return this.activeTab == AppearanceTab.CARD_BACK ? this.selectedCardBack : this.selectedDiceSkin;
    }

    private Component displayName(CardBackDefinition definition) {
        if (!definition.nameKey().isBlank()) return Component.translatable(definition.nameKey());
        String path = definition.texture().getPath();
        int slash = path.lastIndexOf('/');
        int dot = path.lastIndexOf('.');
        return Component.literal(path.substring(slash + 1, dot > slash ? dot : path.length()));
    }

    private Component ellipsize(Component input, int maxWidth) {
        String text = input.getString();
        if (this.font.width(text) <= maxWidth) return input;
        String suffix = "...";
        StringBuilder out = new StringBuilder();
        for (int index = 0; index < text.length(); index++) {
            if (this.font.width(out.toString()) + this.font.width(suffix) >= maxWidth) break;
            out.append(text.charAt(index));
        }
        return Component.literal(out + suffix);
    }

    private float maxScroll() {
        int listW = this.panelWidth() - 32;
        int columns = this.columns(listW);
        int contentHeight = 0;
        for (NamespaceSection section : this.sections) {
            contentHeight += HEADER_H + this.rows(section.definitions().size(), columns)
                    * (CARD_H + CARD_LABEL_H + CARD_GAP) + 8;
        }
        return Math.max(0, contentHeight - (this.panelHeight() - LIST_TOP - LIST_BOTTOM));
    }

    private int columns(int listWidth) {
        return Math.max(1, (listWidth - 8 + CARD_GAP) / (CARD_W + CARD_GAP));
    }

    private int rows(int size, int columns) {
        return (size + columns - 1) / columns;
    }

    private int panelWidth() {
        return Math.clamp(this.width - 24, 300, MAX_PANEL_WIDTH);
    }

    private int panelHeight() {
        return Math.clamp(this.height - 24, 250, MAX_PANEL_HEIGHT);
    }

    private int panelX() {
        return (this.width - this.panelWidth()) / 2;
    }

    private int panelY() {
        return (this.height - this.panelHeight()) / 2;
    }

    private int scrollbarThumbHeight(int height) {
        int content = Math.max(height, height + Math.round(this.maxScroll()));
        return Mth.clamp(Math.round(height * (height / (float) content)), 24, height);
    }

    private void renderScrollbar(GuiGraphicsExtractor graphics, int x, int y, int height) {
        if (this.maxScroll() <= 0.0F) return;
        int thumbH = this.scrollbarThumbHeight(height);
        int thumbY = y + Math.round((height - thumbH) * (this.scrollY / this.maxScroll()));
        graphics.fill(x, y, x + 5, y + height, 0x66000000);
        graphics.fill(x, thumbY, x + 5, thumbY + thumbH, 0xCCFFFFFF);
    }

    private static Identifier resolveSelection(List<CardBackDefinition> definitions, Identifier selected) {
        return definitions.stream().filter(definition -> definition.id().equals(selected) || definition.texture().equals(selected))
                .map(CardBackDefinition::id).findFirst().orElse(definitions.getFirst().id());
    }

    private static List<NamespaceSection> groupByNamespace(List<CardBackDefinition> definitions) {
        Map<String, List<CardBackDefinition>> grouped = new LinkedHashMap<>();
        for (CardBackDefinition definition : definitions) {
            grouped.computeIfAbsent(definition.texture().getNamespace(), ignored -> new ArrayList<>()).add(definition);
        }
        return grouped.entrySet().stream().map(entry -> new NamespaceSection(entry.getKey(), entry.getValue())).toList();
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private enum AppearanceTab {
        CARD_BACK("gui.astral_craft.appearance_selection.tab.card_back"),
        DICE("gui.astral_craft.appearance_selection.tab.dice");

        private final String translationKey;

        AppearanceTab(String translationKey) {
            this.translationKey = translationKey;
        }

    }

    private record NamespaceSection(String namespace, List<CardBackDefinition> definitions) {

        private NamespaceSection {
            definitions = List.copyOf(definitions);
        }

    }

}