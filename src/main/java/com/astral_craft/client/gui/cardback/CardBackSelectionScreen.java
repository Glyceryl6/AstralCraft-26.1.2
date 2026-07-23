package com.astral_craft.client.gui.cardback;

import com.astral_craft.client.gui.components.AstralFancyButton;
import com.astral_craft.client.gui.components.AstralFancyButton.ButtonStyle;
import com.astral_craft.common.gameplay.cardback.CardBackDefinition;
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

import java.util.*;

public class CardBackSelectionScreen extends Screen {

    protected static final int MAX_PANEL_WIDTH = 540;
    protected static final int MAX_PANEL_HEIGHT = 420;
    protected static final int CARD_W = 64;
    protected static final int CARD_H = 90;
    protected static final int CARD_LABEL_H = 18;
    protected static final int CARD_GAP = 12;
    protected static final int HEADER_H = 22;
    protected static final int BUTTON_W = 112;
    protected static final int BUTTON_H = 30;

    protected final List<CardBackDefinition> definitions;
    protected final List<NamespaceSection> sections;
    protected Identifier selected;
    protected float scrollY;
    protected boolean draggingScrollbar;
    protected double dragStartY;
    protected float dragStartScrollY;

    public CardBackSelectionScreen(List<CardBackDefinition> definitions, Identifier selected) {
        super(Component.translatable("gui.astral_craft.card_back_selection.title"));
        this.definitions = mergeResourceBacks(definitions);
        this.sections = groupByNamespace(this.definitions);
        CardBackDefinition selectedDefinition = this.definitions.stream()
                .filter(definition -> definition.id().equals(selected)
                        || definition.texture().equals(selected))
                .findFirst().orElse(this.definitions.getFirst());
        this.selected = selectedDefinition.id();
    }

    public static void open(OpenCardBackSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new CardBackSelectionScreen(payload.options(), payload.selectedId())));
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
        graphics.text(this.font, this.title, x + 16, y + 12, 0xFFFFFFFF, false);
        int listX = x + 16;
        int listY = y + 36;
        int listW = width - 32;
        int listH = height - 92;
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
                confirmX + BUTTON_W + 10, buttonY, BUTTON_W, BUTTON_H, false,
                cancelHovered, ButtonStyle.button(0xFF9B5360));
        AstralFancyButton.setHandCursor(confirmHovered || cancelHovered || this.hoveredCard(mouseX, mouseY));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        int x = this.panelX();
        int y = this.panelY();
        int width = this.panelWidth();
        int height = this.panelHeight();
        int listX = x + 16;
        int listY = y + 36;
        int listW = width - 32;
        int listH = height - 92;
        int scrollbarX = listX + listW - 5;
        if (this.maxScroll() > 0.0F && inside(event.x(), event.y(), scrollbarX - 2, listY, 9, listH)) {
            this.draggingScrollbar = true;
            this.dragStartY = event.y();
            this.dragStartScrollY = this.scrollY;
            return true;
        }

        CardBackDefinition clicked = this.cardAt(event.x(), event.y());
        if (clicked != null) {
            this.selected = clicked.id();
            return true;
        }

        int confirmX = x + width - 16 - BUTTON_W * 2 - 10;
        int buttonY = y + height - 42;
        if (inside(event.x(), event.y(), confirmX, buttonY, BUTTON_W, BUTTON_H)) {
            ClientPacketDistributor.sendToServer(new CardBackSelectionPayload(this.selected));
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
            int listH = this.panelHeight() - 92;
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
        int listY = this.panelY() + 36;
        int listW = this.panelWidth() - 32;
        int listH = this.panelHeight() - 92;
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

    protected void renderSections(GuiGraphicsExtractor graphics, int listX, int listY, int listW, int mouseX, int mouseY) {
        int columns = this.columns(listW);
        int cursorY = listY - Math.round(this.scrollY);
        for (NamespaceSection section : this.sections) {
            graphics.text(this.font, Component.literal(section.namespace()), listX + 4, cursorY + 6, 0xFFFFD27D, false);
            cursorY += HEADER_H;
            for (int index = 0; index < section.definitions().size(); index++) {
                int column = index % columns;
                int row = index / columns;
                int cardX = listX + 4 + column * (CARD_W + CARD_GAP);
                int cardY = cursorY + row * (CARD_H + CARD_LABEL_H + CARD_GAP);
                CardBackDefinition definition = section.definitions().get(index);
                boolean hovered = inside(mouseX, mouseY, cardX - 3, cardY - 3, CARD_W + 6, CARD_H + CARD_LABEL_H + 6);
                boolean chosen = definition.id().equals(this.selected);
                graphics.fill(cardX - 3, cardY - 3, cardX + CARD_W + 3, cardY + CARD_H + CARD_LABEL_H + 3,
                        chosen ? 0xAAFFF0A0 : hovered ? 0x665C5C72 : 0x44202028);
                graphics.blit(RenderPipelines.GUI_TEXTURED, definition.texture(), cardX, cardY,
                        0.0F, 0.0F, CARD_W, CARD_H, 256, 360, 256, 360, 0xFFFFFFFF);
                Component name = this.displayName(definition);
                Component clipped = this.ellipsize(name, CARD_W + 4);
                graphics.text(this.font, clipped, cardX + CARD_W / 2 - this.font.width(clipped) / 2, cardY + CARD_H + 5, 0xFFFFFFFF, false);
            }

            cursorY += this.rows(section.definitions().size(), columns) * (CARD_H + CARD_LABEL_H + CARD_GAP) + 8;
        }
    }

    protected CardBackDefinition cardAt(double mouseX, double mouseY) {
        int listX = this.panelX() + 16;
        int listY = this.panelY() + 36;
        int listW = this.panelWidth() - 32;
        int listH = this.panelHeight() - 92;
        if (!inside(mouseX, mouseY, listX, listY, listW, listH)) return null;
        int columns = this.columns(listW);
        int cursorY = listY - Math.round(this.scrollY);
        for (NamespaceSection section : this.sections) {
            cursorY += HEADER_H;
            for (int index = 0; index < section.definitions().size(); index++) {
                int cardX = listX + 4 + index % columns * (CARD_W + CARD_GAP);
                int cardY = cursorY + index / columns * (CARD_H + CARD_LABEL_H + CARD_GAP);
                if (inside(mouseX, mouseY, cardX - 3, cardY - 3, CARD_W + 6, CARD_H + CARD_LABEL_H + 6)) {
                    return section.definitions().get(index);
                }
            }

            cursorY += this.rows(section.definitions().size(), columns) * (CARD_H + CARD_LABEL_H + CARD_GAP) + 8;
        }

        return null;
    }

    protected boolean hoveredCard(double mouseX, double mouseY) {
        return this.cardAt(mouseX, mouseY) != null;
    }

    protected Component displayName(CardBackDefinition definition) {
        if (!definition.nameKey().isBlank()) return Component.translatable(definition.nameKey());
        String path = definition.texture().getPath();
        int slash = path.lastIndexOf('/');
        int dot = path.lastIndexOf('.');
        return Component.literal(path.substring(slash + 1, dot > slash ? dot : path.length()));
    }

    protected Component ellipsize(Component input, int maxWidth) {
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

    protected float maxScroll() {
        int listW = this.panelWidth() - 32;
        int columns = this.columns(listW);
        int contentHeight = 0;
        for (NamespaceSection section : this.sections) {
            contentHeight += HEADER_H + this.rows(section.definitions().size(), columns) * (CARD_H + CARD_LABEL_H + CARD_GAP) + 8;
        }

        return Math.max(0, contentHeight - (this.panelHeight() - 92));
    }

    protected int columns(int listWidth) {
        return Math.max(1, (listWidth - 8 + CARD_GAP) / (CARD_W + CARD_GAP));
    }

    protected int rows(int size, int columns) {
        return (size + columns - 1) / columns;
    }

    protected int panelWidth() {
        return Math.clamp(this.width - 24, 300, MAX_PANEL_WIDTH);
    }

    protected int panelHeight() {
        return Math.clamp(this.height - 24, 230, MAX_PANEL_HEIGHT);
    }

    protected int panelX() {
        return (this.width - this.panelWidth()) / 2;
    }

    protected int panelY() {
        return (this.height - this.panelHeight()) / 2;
    }

    protected int scrollbarThumbHeight(int height) {
        int content = Math.max(height, height + Math.round(this.maxScroll()));
        return Mth.clamp(Math.round(height * (height / (float) content)), 24, height);
    }

    protected void renderScrollbar(GuiGraphicsExtractor graphics, int x, int y, int height) {
        if (this.maxScroll() <= 0.0F) return;
        int thumbH = this.scrollbarThumbHeight(height);
        int thumbY = y + Math.round((height - thumbH) * (this.scrollY / this.maxScroll()));
        graphics.fill(x, y, x + 5, y + height, 0x66000000);
        graphics.fill(x, thumbY, x + 5, thumbY + thumbH, 0xCCFFFFFF);
    }

    protected static List<CardBackDefinition> mergeResourceBacks(List<CardBackDefinition> serverDefinitions) {
        Map<Identifier, CardBackDefinition> byTexture = new LinkedHashMap<>();
        for (CardBackDefinition definition : serverDefinitions) {
            byTexture.put(definition.texture(), definition);
        }

        Minecraft.getInstance().getResourceManager().listResources("textures/gui/cards/back",
                identifier -> identifier.getPath().endsWith(".jpg")).keySet().forEach(texture ->
                byTexture.putIfAbsent(texture, CardBackDefinition.scanned(texture)));
        if (byTexture.isEmpty()) {
            CardBackDefinition definition = CardBackDefinition.builtinDefault();
            byTexture.put(definition.texture(), definition);
        }

        return List.copyOf(byTexture.values());
    }

    protected static List<NamespaceSection> groupByNamespace(List<CardBackDefinition> definitions) {
        Map<String, List<CardBackDefinition>> grouped = new LinkedHashMap<>();
        for (CardBackDefinition definition : definitions) {
            grouped.computeIfAbsent(definition.texture().getNamespace(), ignored -> new ArrayList<>()).add(definition);
        }

        return grouped.entrySet().stream().map(entry -> new NamespaceSection(entry.getKey(), entry.getValue())).toList();
    }

    protected static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    protected record NamespaceSection(String namespace, List<CardBackDefinition> definitions) {
        protected NamespaceSection {
            definitions = List.copyOf(definitions);
        }
    }

}