package com.astral_craft.client.gui;

import com.astral_craft.client.gui.components.AstralFancyButton;
import com.astral_craft.client.render.CustomPaintingPreviewRenderer;
import com.astral_craft.common.components.CustomPaintingData;
import com.astral_craft.common.network.c2s.CustomPaintingConfigPayload;
import com.astral_craft.common.network.s2c.OpenCustomPaintingConfigPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

public class CustomPaintingConfigScreen extends Screen {

    private final int entityId;
    private final CustomPaintingData initialData;
    private EditBox resourceBox;
    private EditBox widthBox;
    private EditBox heightBox;
    private boolean submitted;

    public CustomPaintingConfigScreen(int entityId, CustomPaintingData initialData) {
        super(Component.translatable("gui.astral_craft.custom_painting.title"));
        this.entityId = entityId;
        this.initialData = initialData == null ? CustomPaintingData.EMPTY : initialData;
    }

    public static void open(OpenCustomPaintingConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new CustomPaintingConfigScreen(payload.entityId(), payload.data())));
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(430, this.width - 24);
        int x = (this.width - panelWidth) / 2 + 18;
        int y = (this.height - 214) / 2;
        int fieldWidth = panelWidth - 36;
        this.resourceBox = this.addRenderableWidget(new EditBox(this.font, x, y + 58, fieldWidth, 22,
                Component.translatable("gui.astral_craft.custom_painting.resource")));
        this.resourceBox.setMaxLength(256);
        this.resourceBox.setValue(this.initialData.resource());
        this.resourceBox.setHint(Component.translatable("gui.astral_craft.custom_painting.resource_hint"));
        int sizeWidth = Math.min(120, (fieldWidth - 18) / 2);
        this.widthBox = this.addRenderableWidget(new EditBox(this.font, x, y + 108, sizeWidth, 22,
                Component.translatable("gui.astral_craft.custom_painting.width")));
        this.widthBox.setMaxLength(2);
        this.widthBox.setFilter(CustomPaintingConfigScreen::numericOrEmpty);
        this.widthBox.setValue(Integer.toString(this.initialData.width()));
        this.heightBox = this.addRenderableWidget(new EditBox(this.font, x + sizeWidth + 18, y + 108, sizeWidth, 22,
                Component.translatable("gui.astral_craft.custom_painting.height")));
        this.heightBox.setMaxLength(2);
        this.heightBox.setFilter(CustomPaintingConfigScreen::numericOrEmpty);
        this.heightBox.setValue(Integer.toString(this.initialData.height()));
        this.resourceBox.setResponder(value -> this.updatePreview());
        this.widthBox.setResponder(value -> this.updatePreview());
        this.heightBox.setResponder(value -> this.updatePreview());
        CustomPaintingPreviewRenderer.beginEditing(this.entityId, this.currentData());
        this.setInitialFocus(this.resourceBox);
    }

    @Override
    public void removed() {
        CustomPaintingPreviewRenderer.endEditing();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = this.layout();
        graphics.fill(layout.x(), layout.y(), layout.x() + layout.width(), layout.y() + layout.height(), 0xF0131822);
        graphics.fill(layout.x(), layout.y(), layout.x() + layout.width(), layout.y() + 2, 0xFFDAF3FF);
        graphics.centeredText(this.font, this.title, this.width / 2, layout.y() + 16, 0xFFFFFFFF);
        graphics.text(this.font, Component.translatable("gui.astral_craft.custom_painting.resource"), layout.fieldX(), layout.y() + 44, 0xFFD7E4F2);
        graphics.text(this.font, Component.translatable("gui.astral_craft.custom_painting.width"), layout.fieldX(), layout.y() + 94, 0xFFD7E4F2);
        graphics.text(this.font, Component.translatable("gui.astral_craft.custom_painting.height"), layout.fieldX() + layout.sizeFieldWidth() + 18, layout.y() + 94, 0xFFD7E4F2);
        boolean valid = this.validInput();
        AstralFancyButton.renderButton(graphics, this.font, Component.translatable("gui.astral_craft.confirm"),
                layout.confirmX(), layout.buttonY(), layout.buttonWidth(), 32, this.submitted || !valid,
                inside(mouseX, mouseY, layout.confirmX(), layout.buttonY(), layout.buttonWidth(), 32),
                AstralFancyButton.ButtonStyle.button(0xFF4F9D69));
        AstralFancyButton.renderButton(graphics, this.font, Component.translatable("gui.astral_craft.cancel"),
                layout.cancelX(), layout.buttonY(), layout.buttonWidth(), 32, this.submitted,
                inside(mouseX, mouseY, layout.cancelX(), layout.buttonY(), layout.buttonWidth(), 32),
                AstralFancyButton.ButtonStyle.button(0xFF9B5360));
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!this.submitted && event.button() == 0) {
            Layout layout = this.layout();
            if (inside(event.x(), event.y(), layout.confirmX(), layout.buttonY(), layout.buttonWidth(), 32) && this.validInput()) {
                this.submitted = true;
                ClientPacketDistributor.sendToServer(new CustomPaintingConfigPayload(this.entityId, this.currentData()));
                this.onClose();
                return true;
            }
            if (inside(event.x(), event.y(), layout.cancelX(), layout.buttonY(), layout.buttonWidth(), 32)) {
                this.onClose();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void updatePreview() {
        if (this.resourceBox != null && this.widthBox != null && this.heightBox != null) {
            CustomPaintingPreviewRenderer.updateEditing(this.currentData());
        }
    }

    private CustomPaintingData currentData() {
        return new CustomPaintingData(this.resourceBox == null ? this.initialData.resource() : this.resourceBox.getValue().trim(),
                this.widthBox == null ? this.initialData.width() : parseSize(this.widthBox.getValue()),
                this.heightBox == null ? this.initialData.height() : parseSize(this.heightBox.getValue()));
    }

    private boolean validInput() {
        return this.resourceBox != null && CustomPaintingData.validResource(this.resourceBox.getValue());
    }

    private Layout layout() {
        int width = Math.min(430, this.width - 24);
        int height = 214;
        int x = (this.width - width) / 2;
        int y = (this.height - height) / 2;
        int fieldWidth = width - 36;
        int sizeFieldWidth = Math.min(120, (fieldWidth - 18) / 2);
        int buttonWidth = Math.min(150, (width - 50) / 2);
        return new Layout(x, y, width, height, x + 18, sizeFieldWidth, buttonWidth, y + height - 44);
    }

    private static int parseSize(String value) {
        if (value == null || value.isBlank()) return 1;
        try {
            return Math.clamp(Integer.parseInt(value), 1, CustomPaintingData.MAX_SIZE);
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private static boolean numericOrEmpty(String value) {
        if (value.isEmpty()) return true;
        if (value.length() > 2) return false;
        for (int index = 0; index < value.length(); index++) if (!Character.isDigit(value.charAt(index))) return false;
        return true;
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private record Layout(int x, int y, int width, int height, int fieldX, int sizeFieldWidth, int buttonWidth, int buttonY) {
        private int confirmX() { return this.x + 16; }
        private int cancelX() { return this.x + this.width - this.buttonWidth - 16; }
    }
}
