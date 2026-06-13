package com.astral_craft.client.gui;

import com.astral_craft.common.network.CardTargetSelectionPayload;
import com.astral_craft.common.network.OpenTargetSelectionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TargetSelectionScreen extends Screen {

    private static final int CARD_WIDTH = 124;
    private static final int CARD_HEIGHT = 74;
    private static final int CARD_GAP = 10;
    private static final int PANEL_PADDING = 12;
    private static final int PANEL_MIN_WIDTH = 360;
    private static final int PANEL_HEIGHT = 178;
    private static final int SCROLLBAR_HEIGHT = 6;

    /**
     * Entity preview pose. These defaults imitate the inventory/block-item isometric feeling:
     * a slight top-down pitch plus a 3/4 side view. Tweak these to make the model face your UI.
     */
    private static final float ENTITY_PREVIEW_ROT_X_DEGREES = 24.0F;
    private static final float ENTITY_PREVIEW_ROT_Y_DEGREES = 225.0F;
    private static final float ENTITY_PREVIEW_ROT_Z_DEGREES = 180.0F;
    private static final float ENTITY_PREVIEW_TRANSLATE_Y_RATIO = 0.48F;
    private static final float ENTITY_PREVIEW_WIDTH_PADDING = 1.75F;
    private static final float ENTITY_PREVIEW_HEIGHT_PADDING = 1.20F;

    private final OpenTargetSelectionPayload payload;
    private final List<Candidate> candidates;
    private final Set<Integer> selected = new LinkedHashSet<>();

    private Button confirmButton;
    private float scrollX;
    private boolean draggingScrollbar;
    private double dragStartMouseX;
    private float dragStartScrollX;

    public TargetSelectionScreen(OpenTargetSelectionPayload payload) {
        super(Component.translatable("gui.astral_craft.target_selection.title"));
        this.payload = payload;
        this.candidates = Candidate.parse(payload.candidates());
    }

    public static void open(OpenTargetSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new TargetSelectionScreen(payload)));
    }

    @Override
    protected void init() {
        int bottom = panelY() + PANEL_HEIGHT - 28;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.astral_craft.cancel"), _ -> this.onClose())
                .bounds(this.width / 2 - 106, bottom, 96, 20).build());
        this.confirmButton = Button.builder(Component.translatable("gui.astral_craft.confirm"), _ -> confirm())
                .bounds(this.width / 2 + 10, bottom, 96, 20).build();
        this.confirmButton.active = false;
        this.addRenderableWidget(this.confirmButton);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Keep the world visible. The target selector draws its own semi-transparent panel.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.clampScroll();
        int panelX = panelX();
        int panelY = panelY();
        int panelW = panelWidth();
        int panelRight = panelX + panelW;
        int panelBottom = panelY + PANEL_HEIGHT;
        int viewX = cardsViewX();
        int viewY = cardsViewY();
        int viewW = cardsViewWidth();
        int viewRight = viewX + viewW;
        graphics.fill(panelX, panelY, panelRight, panelBottom, 0xB0101018);
        graphics.fill(panelX, panelY, panelRight, panelY + 1, 0x80FFFFFF);
        graphics.fill(panelX, panelBottom - 1, panelRight, panelBottom, 0x80000000);
        graphics.fill(panelX, panelY, panelX + 1, panelBottom, 0x60FFFFFF);
        graphics.fill(panelRight - 1, panelY, panelRight, panelBottom, 0x60000000);
        graphics.text(this.font, this.title, this.width / 2 - this.font.width(this.title) / 2, panelY + 9, 0xFFFFFFFF, true);
        Component hint = Component.translatable("gui.astral_craft.target_selection.hint", this.payload.minTargets(), this.payload.maxTargets(), this.payload.range());
        graphics.text(this.font, hint, this.width / 2 - this.font.width(hint) / 2, panelY + 24, 0xFFE0E0E0, false);
        graphics.enableScissor(viewX, viewY - 2, viewRight, viewY + CARD_HEIGHT + 2);
        for (int i = 0; i < this.candidates.size(); i++) {
            int x = candidateX(i);
            if (x + CARD_WIDTH < viewX || x > viewRight) continue;
            this.renderCandidate(graphics, this.font, this.candidates.get(i), x, viewY, mouseX, mouseY);
        }

        graphics.disableScissor();
        this.renderScrollbar(graphics);
        this.updateConfirmButton();
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button == 0 && this.isScrollbarVisible() && this.isMouseOverScrollbar(mouseX, mouseY)) {
            if (!this.isMouseOverScrollbarThumb(mouseX, mouseY)) this.jumpScrollTo(mouseX);
            this.draggingScrollbar = true;
            this.dragStartMouseX = mouseX;
            this.dragStartScrollX = this.scrollX;
            return true;
        }

        if (button == 0 && mouseY >= this.cardsViewY() && mouseY <= this.cardsViewY() + CARD_HEIGHT
                && mouseX >= this.cardsViewX() && mouseX <= this.cardsViewX() + this.cardsViewWidth()) {
            for (int i = 0; i < this.candidates.size(); i++) {
                int x = this.candidateX(i);
                if (mouseX >= x && mouseX <= x + CARD_WIDTH) {
                    this.toggle(this.candidates.get(i).entityId());
                    return true;
                }
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.draggingScrollbar) {
            int trackW = this.scrollbarTrackWidth();
            int thumbW = this.scrollbarThumbWidth();
            int movable = Math.max(1, trackW - thumbW);
            float max = this.maxScroll();
            this.scrollX = Mth.clamp(this.dragStartScrollX + (float) ((event.x() - this.dragStartMouseX) / movable * max), 0.0F, max);
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
        if (mouseX >= this.cardsViewX() && mouseX <= this.cardsViewX() + this.cardsViewWidth()
                && mouseY >= this.cardsViewY() - 8 && mouseY <= this.cardsViewY() + CARD_HEIGHT + 22) {
            float amount = (float) ((Math.abs(deltaX) > 0.0D ? -deltaX : -deltaY) * 30.0D);
            this.scrollX = Mth.clamp(this.scrollX + amount, 0.0F, maxScroll());
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    private void renderCandidate(GuiGraphicsExtractor graphics, Font font, Candidate candidate, int x, int y, int mouseX, int mouseY) {
        boolean active = this.selected.contains(candidate.entityId());
        boolean hovered = mouseX >= x && mouseX <= x + CARD_WIDTH && mouseY >= y && mouseY <= y + CARD_HEIGHT;
        int bg = active ? 0xCC3A6B48 : hovered ? 0xCC303038 : 0xAA202028;
        int border = active ? 0xFF85FF9E : hovered ? 0xFFE0E0E0 : 0xFF808080;
        graphics.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, bg);
        graphics.fill(x, y, x + CARD_WIDTH, y + 1, border);
        graphics.fill(x, y + CARD_HEIGHT - 1, x + CARD_WIDTH, y + CARD_HEIGHT, border);
        graphics.fill(x, y, x + 1, y + CARD_HEIGHT, border);
        graphics.fill(x + CARD_WIDTH - 1, y, x + CARD_WIDTH, y + CARD_HEIGHT, border);
        int modelLeft = x + 7;
        int modelTop = y + 8;
        int modelRight = x + 49;
        int modelBottom = y + CARD_HEIGHT - 9;
        graphics.fill(modelLeft, modelTop, modelRight, modelBottom, 0x66000000);
        Entity entity = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getEntity(candidate.entityId());
        if (entity instanceof LivingEntity living) {
            renderEntityModel(graphics, living, modelLeft, modelTop, modelRight, modelBottom);
        }

        graphics.text(font, Component.literal(ellipsize(font, candidate.name(), 62)), x + 55, y + 17, 0xFFFFFFFF, false);
        Component distance = Component.translatable("gui.astral_craft.target_selection.distance", candidate.distance());
        graphics.text(font, distance, x + 55, y + 34, 0xFFB0B0B0, false);
    }

    private void renderScrollbar(GuiGraphicsExtractor graphics) {
        if (!this.isScrollbarVisible()) return;
        int trackX = this.scrollbarTrackX();
        int trackY = this.scrollbarTrackY();
        int trackW = this.scrollbarTrackWidth();
        int thumbX = this.scrollbarThumbX();
        int thumbW = this.scrollbarThumbWidth();
        graphics.fill(trackX, trackY, trackX + trackW, trackY + SCROLLBAR_HEIGHT, 0x66000000);
        graphics.fill(thumbX, trackY, thumbX + thumbW, trackY + SCROLLBAR_HEIGHT, 0xCCFFFFFF);
    }

    private void toggle(int entityId) {
        if (this.selected.contains(entityId)) {
            this.selected.remove(entityId);
            this.updateConfirmButton();
            return;
        }

        if (this.selected.size() >= this.payload.maxTargets()) {
            Integer first = this.selected.iterator().next();
            this.selected.remove(first);
        }

        this.selected.add(entityId);
        this.updateConfirmButton();
    }

    private void confirm() {
        if (this.selected.size() < this.payload.minTargets()) return;
        StringBuilder ids = new StringBuilder();
        for (int id : this.selected) {
            if (!ids.isEmpty()) ids.append(',');
            ids.append(id);
        }

        ClientPacketDistributor.sendToServer(new CardTargetSelectionPayload(this.payload.cardId(), this.payload.handIndex(), ids.toString()));
        this.onClose();
    }

    private void updateConfirmButton() {
        if (this.confirmButton != null) {
            this.confirmButton.active = this.selected.size() >= this.payload.minTargets();
        }
    }

    private void clampScroll() {
        this.scrollX = Mth.clamp(this.scrollX, 0.0F, maxScroll());
    }

    private int panelWidth() {
        return Mth.clamp(this.width - 40, PANEL_MIN_WIDTH, 560);
    }

    private int panelX() {
        return (this.width - panelWidth()) / 2;
    }

    private int panelY() {
        return Math.max(12, (this.height - PANEL_HEIGHT) / 2);
    }

    private int cardsViewX() {
        return panelX() + PANEL_PADDING;
    }

    private int cardsViewY() {
        return panelY() + 43;
    }

    private int cardsViewWidth() {
        return panelWidth() - PANEL_PADDING * 2;
    }

    private int contentWidth() {
        if (this.candidates.isEmpty()) return 0;
        return this.candidates.size() * CARD_WIDTH + (this.candidates.size() - 1) * CARD_GAP;
    }

    private float maxScroll() {
        return Math.max(0, contentWidth() - cardsViewWidth());
    }

    private int candidateX(int index) {
        return this.cardsViewX() + index * (CARD_WIDTH + CARD_GAP) - Math.round(this.scrollX);
    }

    private boolean isScrollbarVisible() {
        return this.maxScroll() > 0.5F;
    }

    private int scrollbarTrackX() {
        return this.cardsViewX();
    }

    private int scrollbarTrackY() {
        return this.cardsViewY() + CARD_HEIGHT + 9;
    }

    private int scrollbarTrackWidth() {
        return this.cardsViewWidth();
    }

    private int scrollbarThumbWidth() {
        int trackW = this.scrollbarTrackWidth();
        int contentW = Math.max(1, this.contentWidth());
        return Mth.clamp((int) (trackW * (trackW / (float) contentW)), 24, trackW);
    }

    private int scrollbarThumbX() {
        int movable = this.scrollbarTrackWidth() - this.scrollbarThumbWidth();
        if (movable <= 0) return scrollbarTrackX();
        return this.scrollbarTrackX() + Math.round((this.scrollX / maxScroll()) * movable);
    }

    private boolean isMouseOverScrollbar(double mouseX, double mouseY) {
        return mouseX >= this.scrollbarTrackX() && mouseX <= this.scrollbarTrackX() + this.scrollbarTrackWidth()
                && mouseY >= this.scrollbarTrackY() - 3 && mouseY <= this.scrollbarTrackY() + SCROLLBAR_HEIGHT + 3;
    }

    private boolean isMouseOverScrollbarThumb(double mouseX, double mouseY) {
        return mouseX >= this.scrollbarThumbX() && mouseX <= this.scrollbarThumbX() + this.scrollbarThumbWidth()
                && mouseY >= this.scrollbarTrackY() - 3 && mouseY <= this.scrollbarTrackY() + SCROLLBAR_HEIGHT + 3;
    }

    private void jumpScrollTo(double mouseX) {
        int trackW = this.scrollbarTrackWidth();
        int thumbW = this.scrollbarThumbWidth();
        int movable = Math.max(1, trackW - thumbW);
        float ratio = (float) ((mouseX - this.scrollbarTrackX() - thumbW / 2.0D) / movable);
        this.scrollX = Mth.clamp(ratio * this.maxScroll(), 0.0F, this.maxScroll());
    }

    private static String ellipsize(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String suffix = "...";
        int suffixWidth = font.width(suffix);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (font.width(builder.toString()) + suffixWidth >= maxWidth) break;
            builder.append(text.charAt(i));
        }

        return builder + suffix;
    }

    private static void renderEntityModel(GuiGraphicsExtractor graphics, LivingEntity entity, int x0, int y0, int x1, int y1) {
        EntityRenderState renderState = extractEntityRenderState(entity);
        if (renderState instanceof LivingEntityRenderState livingState) {
            livingState.bodyRot = ENTITY_PREVIEW_ROT_Y_DEGREES;
            livingState.yRot = ENTITY_PREVIEW_ROT_Y_DEGREES;
            livingState.xRot = ENTITY_PREVIEW_ROT_X_DEGREES;
            livingState.boundingBoxWidth /= livingState.scale;
            livingState.boundingBoxHeight /= livingState.scale;
            livingState.scale = 1.0F;
        }

        float boxWidth = Math.max(0.35F, renderState.boundingBoxWidth);
        float boxHeight = Math.max(0.65F, renderState.boundingBoxHeight);
        float viewWidth = Math.max(1.0F, x1 - x0);
        float viewHeight = Math.max(1.0F, y1 - y0);
        float scale = Math.min(viewWidth / (boxWidth * ENTITY_PREVIEW_WIDTH_PADDING),
                viewHeight / (boxHeight * ENTITY_PREVIEW_HEIGHT_PADDING));
        scale = Mth.clamp(scale, 14.0F, 42.0F);
        Quaternionf rotation = new Quaternionf()
                .rotateZ((float) Math.toRadians(ENTITY_PREVIEW_ROT_Z_DEGREES))
                .rotateX((float) Math.toRadians(ENTITY_PREVIEW_ROT_X_DEGREES))
                .rotateY((float) Math.toRadians(ENTITY_PREVIEW_ROT_Y_DEGREES));
        Vector3f translation = new Vector3f(0.0F, boxHeight * ENTITY_PREVIEW_TRANSLATE_Y_RATIO, 0.0F);
        graphics.entity(renderState, scale, translation, rotation, null, x0, y0, x1, y1);
    }

    private static EntityRenderState extractEntityRenderState(LivingEntity entity) {
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        EntityRenderer<? super LivingEntity, ?> renderer = dispatcher.getRenderer(entity);
        EntityRenderState renderState = renderer.createRenderState(entity, 1.0F);
        renderState.shadowPieces.clear();
        renderState.outlineColor = 0;
        return renderState;
    }

    public record Candidate(int entityId, String name, int distance) {
        static List<Candidate> parse(String encoded) {
            List<Candidate> result = new ArrayList<>();
            if (encoded == null || encoded.isBlank()) return result;
            for (String entry : encoded.split(";")) {
                String[] parts = entry.split("\\|", 3);
                if (parts.length < 3) continue;
                try {
                    result.add(new Candidate(Integer.parseInt(parts[0]), parts[1], Integer.parseInt(parts[2])));
                } catch (NumberFormatException ignored) {}
            }

            return result;
        }
    }

}