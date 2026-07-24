package com.astral_craft.client.gui.components;

import com.astral_craft.client.gui.components.AstralFancyButton.ButtonStyle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.List;

/** Reusable client-side confirmation dialog. The callback only submits a typed request; the server validates it. */
public class AstralConfirmationScreen extends Screen {

    private final List<Component> lines;
    private final Component confirmText;
    private final Component cancelText;
    private final Runnable confirmAction;
    private boolean submitted;

    public AstralConfirmationScreen(Component title, List<Component> lines, Component confirmText,
                                    Component cancelText, Runnable confirmAction) {
        super(title);
        this.lines = List.copyOf(lines);
        this.confirmText = confirmText;
        this.cancelText = cancelText;
        this.confirmAction = confirmAction;
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
        graphics.fill(layout.x(), layout.y(), layout.x() + layout.width(), layout.y() + 2, 0xFFE7F8FF);
        graphics.centeredText(this.font, this.title, this.width / 2, layout.y() + 18, 0xFFFFFFFF);
        for (int index = 0; index < this.lines.size(); index++) {
            graphics.centeredText(this.font, this.lines.get(index), this.width / 2, layout.messageY() + index * 18,
                    index == 0 ? 0xFFFFD27A : 0xFFD7E4F2);
        }
        AstralFancyButton.renderButton(graphics, this.font, this.confirmText, layout.confirmX(), layout.buttonY(),
                layout.buttonWidth(), layout.buttonHeight(), this.submitted,
                inside(mouseX, mouseY, layout.confirmX(), layout.buttonY(), layout.buttonWidth(), layout.buttonHeight()),
                ButtonStyle.button(0xFF4F9D69));
        AstralFancyButton.renderButton(graphics, this.font, this.cancelText, layout.cancelX(), layout.buttonY(),
                layout.buttonWidth(), layout.buttonHeight(), this.submitted,
                inside(mouseX, mouseY, layout.cancelX(), layout.buttonY(), layout.buttonWidth(), layout.buttonHeight()),
                ButtonStyle.button(0xFF9B5360));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.submitted || event.button() != 0) return super.mouseClicked(event, doubleClick);
        Layout layout = this.layout();
        if (inside(event.x(), event.y(), layout.confirmX(), layout.buttonY(), layout.buttonWidth(), layout.buttonHeight())) {
            this.submitted = true;
            this.confirmAction.run();
            this.onClose();
            return true;
        }
        if (inside(event.x(), event.y(), layout.cancelX(), layout.buttonY(), layout.buttonWidth(), layout.buttonHeight())) {
            this.onClose();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private Layout layout() {
        int width = Math.min(440, this.width - 24);
        int height = 126 + Math.max(1, this.lines.size()) * 18;
        int x = (this.width - width) / 2;
        int y = (this.height - height) / 2;
        int buttonWidth = Math.min(150, (width - 50) / 2);
        return new Layout(x, y, width, height, buttonWidth, 34, y + height - 48);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private record Layout(int x, int y, int width, int height, int buttonWidth, int buttonHeight, int buttonY) {
        private int messageY() { return this.y + 48; }
        private int confirmX() { return this.x + 16; }
        private int cancelX() { return this.x + this.width - this.buttonWidth - 16; }
    }
}
