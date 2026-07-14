package com.astral_craft.client.gui.board;

import com.astral_craft.common.network.BoardEncounterChoicePayload;
import com.astral_craft.common.network.OpenBoardEncounterPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

public class BoardEncounterScreen extends Screen {

    private final String boardId;
    private final int targetEntityId;
    private final String controllerName;
    private int timeoutTicks;

    public BoardEncounterScreen(OpenBoardEncounterPayload payload) {
        super(Component.translatable("gui.astral_craft.board.encounter"));
        this.boardId = payload.boardId();
        this.targetEntityId = payload.targetEntityId();
        this.controllerName = payload.controllerName();
        this.timeoutTicks = Math.max(1, payload.timeoutTicks());
    }

    public static void open(OpenBoardEncounterPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new BoardEncounterScreen(payload)));
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void tick() {
        super.tick();
        if (--this.timeoutTicks <= 0) {
            ClientPacketDistributor.sendToServer(new BoardEncounterChoicePayload(this.boardId, false));
            this.onClose();
        }
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int panelW = Math.min(420, this.width - 32);
        int panelH = Math.min(250, this.height - 32);
        int x = (this.width - panelW) / 2;
        int y = (this.height - panelH) / 2;
        graphics.fill(x, y, x + panelW, y + panelH, 0xEB11111C);
        graphics.fill(x, y, x + panelW, y + 2, 0xB0FFFFFF);
        graphics.text(this.font, this.title, x + 14, y + 12, 0xFFFFFFFF, false);
        graphics.text(this.font, Component.translatable("gui.astral_craft.board.encounter_target", this.controllerName),
                x + 14, y + 30, 0xFFFFC75C, false);
        LivingEntity target = null;
        if (Minecraft.getInstance().level != null) {
            Entity entity = Minecraft.getInstance().level.getEntity(this.targetEntityId);
            if (entity instanceof LivingEntity living) target = living;
        }
        BoardScreenEntityRenderer.render(graphics, target, x + 12, y + 48, x + panelW / 2, y + panelH - 18, 205.0F);
        int buttonW = 116;
        int buttonH = 34;
        int buttonX = x + panelW - buttonW - 22;
        int challengeY = y + 88;
        int passY = challengeY + 48;
        graphics.fill(buttonX, challengeY, buttonX + buttonW, challengeY + buttonH, 0xFFD64B61);
        graphics.fill(buttonX, passY, buttonX + buttonW, passY + buttonH, 0xFF486A9C);
        Component challenge = Component.translatable("gui.astral_craft.board.challenge");
        Component pass = Component.translatable("gui.astral_craft.board.pass");
        graphics.text(this.font, challenge, buttonX + (buttonW - this.font.width(challenge)) / 2, challengeY + 12, 0xFFFFFFFF, false);
        graphics.text(this.font, pass, buttonX + (buttonW - this.font.width(pass)) / 2, passY + 12, 0xFFFFFFFF, false);
        Component timer = Component.translatable("gui.astral_craft.board.timeout", (this.timeoutTicks + 19) / 20);
        graphics.text(this.font, timer, buttonX, passY + buttonH + 12, 0xFFBFC8FF, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        int panelW = Math.min(420, this.width - 32);
        int panelH = Math.min(250, this.height - 32);
        int x = (this.width - panelW) / 2;
        int y = (this.height - panelH) / 2;
        int buttonW = 116;
        int buttonH = 34;
        int buttonX = x + panelW - buttonW - 22;
        int challengeY = y + 88;
        int passY = challengeY + 48;
        if (inside(event.x(), event.y(), buttonX, challengeY, buttonW, buttonH)) {
            choose(true); return true;
        }
        if (inside(event.x(), event.y(), buttonX, passY, buttonW, buttonH)) {
            choose(false); return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void choose(boolean challenge) {
        ClientPacketDistributor.sendToServer(new BoardEncounterChoicePayload(this.boardId, challenge));
        this.onClose();
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
