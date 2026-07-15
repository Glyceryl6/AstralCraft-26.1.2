package com.astral_craft.client.gui.battle;

import com.astral_craft.client.util.ClientAnimationClock;
import com.astral_craft.common.network.s2c.OpenBattleScenePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-only battle staging screen.
 *
 * <p>This screen intentionally does not pause the game and tries to pass movement keys through. It is still a
 * Screen, so mouse input can select/drag cards. If you want perfect FPS-style mouse look during the battle card
 * phase, replace this with a HUD overlay plus explicit mouse event capture; Minecraft normally releases the mouse
 * cursor while a Screen is open.</p>
 */
public class BattleSceneScreen extends Screen {

    private static final int PANEL_HEIGHT = 172;
    private static final int CARD_WIDTH = 64;
    private static final int CARD_HEIGHT = 80;
    private static final int CARD_GAP = 8;

    private static final float LEFT_MODEL_YAW = 130.0F;
    private static final float RIGHT_MODEL_YAW = -130.0F;
    private static final float MODEL_PITCH = 18.0F;
    private static final float MODEL_ROLL = 180.0F;

    private final OpenBattleScenePayload payload;
    private final List<String> cards;
    private Button confirmButton;

    public BattleSceneScreen(OpenBattleScenePayload payload) {
        super(Component.translatable("gui.astral_craft.battle.title"));
        this.payload = payload;
        this.cards = parseCards(payload.availableCards());
    }

    public static void open(OpenBattleScenePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new BattleSceneScreen(payload)));
    }

    @Override
    protected void init() {
        int y = this.height - 28;
        this.confirmButton = Button.builder(Component.translatable("gui.astral_craft.battle.ready"), _ -> this.onClose())
                .bounds(this.width - 112, y, 96, 20).build();
        this.addRenderableWidget(this.confirmButton);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Keep the world visible; the battle scene is an overlay-style interactive screen.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int panelY = this.height - PANEL_HEIGHT;
        graphics.fill(0, panelY, this.width, this.height, 0xB0101018);
        graphics.fill(0, panelY, this.width, panelY + 1, 0x80FFFFFF);
        graphics.text(this.font, this.title, this.width / 2 - this.font.width(this.title) / 2, panelY + 8, 0xFFFFFFFF, true);
        renderBattleModel(graphics, this.payload.attackerId(), this.width / 2 - 120, panelY + 20, this.width / 2 - 18, panelY + 122, LEFT_MODEL_YAW);
        renderBattleModel(graphics, this.payload.defenderId(), this.width / 2 + 18, panelY + 20, this.width / 2 + 120, panelY + 122, RIGHT_MODEL_YAW);
        graphics.text(this.font, Component.translatable("gui.astral_craft.battle.attacker"), this.width / 2 - 112, panelY + 125, 0xFFFFB36B, true);
        graphics.text(this.font, Component.translatable("gui.astral_craft.battle.defender"), this.width / 2 + 54, panelY + 125, 0xFF8FD5FF, true);
        Component cost = Component.translatable("gui.astral_craft.battle.cost", this.payload.availableCost());
        graphics.text(this.font, cost, 16, panelY + 12, 0xFFFFE08A, true);
        this.renderCardStrip(graphics, this.font, panelY + 46, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.isMovementKey(event)) {
            return false;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (this.isMovementKey(event)) {
            return false;
        }

        return super.keyReleased(event);
    }

    private boolean isMovementKey(KeyEvent event) {
        Minecraft mc = Minecraft.getInstance();
        return mc.options.keyUp.matches(event)
                || mc.options.keyDown.matches(event)
                || mc.options.keyLeft.matches(event)
                || mc.options.keyRight.matches(event)
                || mc.options.keyJump.matches(event)
                || mc.options.keyShift.matches(event)
                || mc.options.keySprint.matches(event);
    }

    private void renderCardStrip(GuiGraphicsExtractor graphics, Font font, int y, int mouseX, int mouseY) {
        int x = 16;
        for (String card : this.cards) {
            boolean hover = mouseX >= x && mouseX <= x + CARD_WIDTH && mouseY >= y && mouseY <= y + CARD_HEIGHT;
            graphics.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, hover ? 0xCC3B3348 : 0xAA242430);
            graphics.fill(x, y, x + CARD_WIDTH, y + 1, 0x99FFFFFF);
            graphics.fill(x, y + CARD_HEIGHT - 1, x + CARD_WIDTH, y + CARD_HEIGHT, 0x99000000);
            String title = ellipsize(font, card, CARD_WIDTH - 8);
            graphics.text(font, title, x + 4, y + CARD_HEIGHT - 16, 0xFFFFFFFF, false);
            x += CARD_WIDTH + CARD_GAP;
        }
    }

    private static void renderBattleModel(GuiGraphicsExtractor graphics, int entityId, int x0, int y0, int x1, int y1, float yaw) {
        Entity entity = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getEntity(entityId);
        if (!(entity instanceof LivingEntity living)) {
            graphics.fill(x0, y0, x1, y1, 0x66000000);
            return;
        }

        EntityRenderState renderState = extractEntityRenderState(living);
        if (renderState instanceof LivingEntityRenderState livingState) {
            livingState.bodyRot = yaw;
            livingState.yRot = yaw;
            livingState.xRot = MODEL_PITCH;
            livingState.walkAnimationSpeed = 0.25F;
            livingState.walkAnimationPos += ClientAnimationClock.phaseTicks(20) * 0.05F;
            livingState.boundingBoxWidth /= livingState.scale;
            livingState.boundingBoxHeight /= livingState.scale;
            livingState.scale = 1.0F;
        }

        float boxWidth = Math.max(0.35F, renderState.boundingBoxWidth);
        float boxHeight = Math.max(0.65F, renderState.boundingBoxHeight);
        float scale = Math.min((x1 - x0) / (boxWidth * 1.6F), (y1 - y0) / (boxHeight * 1.18F));
        scale = Mth.clamp(scale, 18.0F, 68.0F);
        Quaternionf rotation = new Quaternionf()
                .rotateZ((float) Math.toRadians(MODEL_ROLL))
                .rotateX((float) Math.toRadians(MODEL_PITCH))
                .rotateY((float) Math.toRadians(yaw));
        Vector3f translation = new Vector3f(0.0F, boxHeight * 0.48F, 0.0F);
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

    private static List<String> parseCards(@Nullable String encoded) {
        List<String> result = new ArrayList<>();
        if (encoded != null && !encoded.isBlank()) {
            for (String entry : encoded.split(";")) {
                if (!entry.isBlank()) result.add(entry.trim());
            }
        }

        return result;
    }

    private static String ellipsize(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String suffix = "...";
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (font.width(builder.toString()) + font.width(suffix) >= maxWidth) break;
            builder.append(text.charAt(i));
        }
        return builder + suffix;
    }

}