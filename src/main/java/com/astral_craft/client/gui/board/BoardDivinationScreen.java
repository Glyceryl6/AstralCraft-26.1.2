package com.astral_craft.client.gui.board;

import com.astral_craft.AstralCraft;
import com.astral_craft.client.gui.HandCardRenderHelper;
import com.astral_craft.client.jpgloader.ScopedJpgTextureCache;
import com.astral_craft.common.gameplay.fortune.BoardFortuneCategory;
import com.astral_craft.common.gameplay.fortune.DivinationTarget;
import com.astral_craft.common.network.c2s.BoardDivinationChoicePayload;
import com.astral_craft.common.network.s2c.OpenBoardDivinationPayload;
import com.astral_craft.common.network.s2c.ResolveBoardDivinationPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.UUID;

public class BoardDivinationScreen extends Screen {

    private static final Identifier EVENT_FRAME = AstralCraft.prefix("textures/item/template_handcard_event.png");
    private static final Identifier EVENT_FALLBACK_ART = AstralCraft.prefix("textures/block/platform_event.png");
    private static final int FLIP_DELAY_TICKS = 20;
    private static final int FLIP_TICKS = 36;
    private static final int CARD_WIDTH = 104;
    private static final int CARD_HEIGHT = 150;
    private final UUID boardId;
    private final List<OpenBoardDivinationPayload.Option> options;
    private final boolean selectable;
    private int timeoutTicks;
    private final int timeoutDurationTicks;
    private int selectedIndex = -1;
    private DivinationTarget target;
    private int revealTicks;
    private boolean submitted;

    public BoardDivinationScreen(OpenBoardDivinationPayload payload) {
        super(Component.translatable("gui.astral_craft.board.divination.title"));
        this.boardId = payload.boardId();
        this.options = List.copyOf(payload.options());
        this.selectable = payload.selectable();
        this.timeoutTicks = payload.timeoutTicks();
        this.timeoutDurationTicks = payload.timeoutDurationTicks();
    }

    private BoardDivinationScreen(ResolveBoardDivinationPayload payload) {
        super(Component.translatable("gui.astral_craft.board.divination.title"));
        this.boardId = payload.boardId();
        this.options = List.of(payload.selectedOption());
        this.selectable = false;
        this.timeoutTicks = 0;
        this.timeoutDurationTicks = 1;
        this.selectedIndex = 0;
        this.target = payload.target();
        this.submitted = true;
    }

    public static void open(OpenBoardDivinationPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new BoardDivinationScreen(payload)));
    }

    public static void resolve(ResolveBoardDivinationPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof BoardDivinationScreen screen && screen.boardId.equals(payload.boardId())) {
                screen.selectedIndex = Math.clamp(payload.selectedIndex(), 0, screen.options.size() - 1);
                screen.target = payload.target();
                screen.revealTicks = 0;
                screen.submitted = true;
                return;
            }
            minecraft.setScreen(new BoardDivinationScreen(payload));
        });
    }

    public static void closePresentation(UUID boardId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof BoardDivinationScreen screen && screen.boardId.equals(boardId)) {
            minecraft.setScreen(null);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.selectedIndex >= 0) {
            this.revealTicks++;
        } else if (this.timeoutTicks > 0) {
            this.timeoutTicks--;
        }
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xB8100B1C);
        graphics.centeredText(this.font, this.title, this.width / 2, Math.max(12, this.height / 2 - 116), 0xFFFFFFFF);
        int gap = 40;
        int total = CARD_WIDTH * this.options.size() + gap * Math.max(0, this.options.size() - 1);
        int firstX = (this.width - total) / 2;
        int y = (this.height - CARD_HEIGHT) / 2 - 4;
        for (int index = 0; index < this.options.size(); index++) {
            int x = firstX + index * (CARD_WIDTH + gap);
            float alpha = this.selectedIndex >= 0 && index != this.selectedIndex
                    ? 1.0F - Mth.clamp(this.revealTicks / 18.0F, 0.0F, 1.0F) : 1.0F;
            boolean hovered = this.selectedIndex < 0 && this.selectable
                    && inside(mouseX, mouseY, x, y, CARD_WIDTH, CARD_HEIGHT);
            if (index == this.selectedIndex) {
                this.renderFlippingCard(graphics, this.options.get(index), x, y, alpha);
            } else {
                this.renderFront(graphics, this.options.get(index), x, y, alpha, hovered);
            }
        }

        if (this.selectedIndex >= 0) {
            this.renderSelectedDescription(graphics, y);
        } else {
            Component instruction = Component.translatable(this.selectable
                    ? "gui.astral_craft.board.divination.choose" : "gui.astral_craft.board.divination.wait");
            graphics.centeredText(this.font, instruction, this.width / 2, y + CARD_HEIGHT + 18, 0xFFD7E4FF);
            if (this.timeoutDurationTicks != Integer.MAX_VALUE) {
                int barWidth = Math.min(260, this.width - 40);
                int barX = (this.width - barWidth) / 2;
                int barY = y + CARD_HEIGHT + 36;
                float progress = Mth.clamp((float) this.timeoutTicks / Math.max(1, this.timeoutDurationTicks), 0.0F, 1.0F);
                graphics.fill(barX, barY, barX + barWidth, barY + 5, 0xAA11131D);
                graphics.fill(barX, barY, barX + Math.round(barWidth * progress), barY + 5, 0xFFE06BC2);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!this.selectable || this.submitted || event.button() != 0) return super.mouseClicked(event, doubleClick);
        int gap = 40;
        int total = CARD_WIDTH * this.options.size() + gap * Math.max(0, this.options.size() - 1);
        int firstX = (this.width - total) / 2;
        int y = (this.height - CARD_HEIGHT) / 2 - 4;
        for (int index = 0; index < this.options.size(); index++) {
            int x = firstX + index * (CARD_WIDTH + gap);
            if (!inside(event.x(), event.y(), x, y, CARD_WIDTH, CARD_HEIGHT)) continue;
            this.submitted = true;
            ClientPacketDistributor.sendToServer(new BoardDivinationChoicePayload(this.boardId, index));
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    private void renderFlippingCard(GuiGraphicsExtractor graphics, OpenBoardDivinationPayload.Option option, int x, int y, float alpha) {
        float progress = Mth.clamp((this.revealTicks - FLIP_DELAY_TICKS) / (float) FLIP_TICKS, 0.0F, 1.0F);
        float widthScale = Math.abs(Mth.cos(progress * Mth.PI));
        int centerX = x + CARD_WIDTH / 2;
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, 0.0F);
        graphics.pose().scale(Math.max(0.02F, widthScale), 1.0F);
        graphics.pose().translate(-centerX, 0.0F);
        if (progress < 0.5F) {
            this.renderFront(graphics, option, x, y, alpha, false);
        } else {
            this.renderBack(graphics, x, y, alpha);
        }

        graphics.pose().popMatrix();
    }

    private void renderFront(GuiGraphicsExtractor graphics, OpenBoardDivinationPayload.Option option,
                             int x, int y, float alpha, boolean hovered) {
        int argb = alphaColor(alpha);
        graphics.blit(RenderPipelines.GUI_TEXTURED, this.frameTexture(option.category()), x, y, 0.0F, 0.0F,
                CARD_WIDTH, CARD_HEIGHT, 44, 64, 44, 64, argb);
        Identifier optionTexture = ScopedJpgTextureCache.isSupportedTexture(option.texture())
                ? ScopedJpgTextureCache.resolve(option.texture()) : EVENT_FALLBACK_ART;
        int optionTextureSize = optionTexture.equals(EVENT_FALLBACK_ART) ? 32 : 256;
        graphics.blit(RenderPipelines.GUI_TEXTURED, optionTexture, x + 13, y + 16, 0.0F, 0.0F,
                CARD_WIDTH - 26, CARD_WIDTH - 26, optionTextureSize, optionTextureSize, optionTextureSize, optionTextureSize, argb);
        Component title = HandCardRenderHelper.ellipsize(this.font, Component.translatable(option.nameKey()), CARD_WIDTH - 14);
        graphics.text(this.font, title, x + CARD_WIDTH / 2 - this.font.width(title) / 2,
                y + CARD_HEIGHT - 28, withAlpha(0xFFFFFF, alpha), true);
        if (hovered) graphics.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, 0x28FFFFFF);
    }

    private void renderBack(GuiGraphicsExtractor graphics, int x, int y, float alpha) {
        Identifier requestedTexture = this.target == null
                ? AstralCraft.prefix("textures/gui/cards/divination/unknown.png") : this.target.texture();
        Identifier texture = ScopedJpgTextureCache.isSupportedTexture(requestedTexture)
                ? ScopedJpgTextureCache.resolve(requestedTexture) : EVENT_FRAME;
        int textureWidth = texture.equals(EVENT_FRAME) ? 44 : 256;
        int textureHeight = texture.equals(EVENT_FRAME) ? 64 : 360;
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F,
                CARD_WIDTH, CARD_HEIGHT, textureWidth, textureHeight, textureWidth, textureHeight, alphaColor(alpha));
        if (this.target == null) return;
        Component targetText = Component.translatable(this.target.translationKey());
        List<FormattedCharSequence> lines = this.font.split(targetText, CARD_WIDTH - 18);
        int textY = y + CARD_HEIGHT - 42 - Math.max(0, lines.size() - 1) * 5;
        for (FormattedCharSequence line : lines) {
            graphics.text(this.font, line, x + CARD_WIDTH / 2 - this.font.width(line) / 2,
                    textY, withAlpha(0xFFFFFF, alpha), true);
            textY += 10;
        }
    }

    private void renderSelectedDescription(GuiGraphicsExtractor graphics, int cardY) {
        if (this.selectedIndex < 0 || this.selectedIndex >= this.options.size()) return;
        Component description = Component.translatable(this.options.get(this.selectedIndex).descriptionKey());
        int maxWidth = Math.clamp(this.width - 40, 160, 360);
        List<FormattedCharSequence> lines = this.font.split(description, maxWidth);
        int y = cardY + CARD_HEIGHT + 18;
        for (FormattedCharSequence line : lines) {
            graphics.text(this.font, line, this.width / 2 - this.font.width(line) / 2, y, 0xFFE6E0F2, false);
            y += 11;
        }
    }

    private Identifier frameTexture(BoardFortuneCategory category) {
        BoardFortuneCategory safeCategory = category == null ? BoardFortuneCategory.NEUTRAL : category;
        return ScopedJpgTextureCache.resolveOrFallback(safeCategory.cardFrameTexture(), EVENT_FRAME);
    }

    private static int alphaColor(float alpha) {
        return (Math.clamp(Math.round(alpha * 255.0F), 0, 255) << 24) | 0xFFFFFF;
    }

    private static int withAlpha(int rgb, float alpha) {
        return (Math.clamp(Math.round(alpha * 255.0F), 0, 255) << 24) | rgb;
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

}