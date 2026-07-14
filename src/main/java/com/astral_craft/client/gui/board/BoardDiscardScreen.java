package com.astral_craft.client.gui.board;

import com.astral_craft.client.gui.HandCardRenderHelper;
import com.astral_craft.client.gui.components.AstralFancyButton;
import com.astral_craft.client.gui.components.AstralFancyButton.ButtonStyle;
import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.network.BoardDiscardPayload;
import com.astral_craft.common.network.OpenBoardDiscardPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class BoardDiscardScreen extends Screen {

    private static final int CARD_W = HandCardRenderHelper.FRAMED_CARD_W;
    private static final int CARD_H = HandCardRenderHelper.FRAMED_CARD_H;
    private static final int GAP = 8;
    private final String boardId;
    private final List<Card> cards;
    private final int required;
    private final Set<Integer> selected = new LinkedHashSet<>();
    private int timeoutTicks;

    public BoardDiscardScreen(OpenBoardDiscardPayload payload) {
        super(Component.translatable("gui.astral_craft.board.discard"));
        this.boardId = payload.boardId();
        this.cards = decode(payload.encodedCards());
        this.required = Math.min(Math.max(0, payload.requiredCount()), this.cards.size());
        this.timeoutTicks = Math.max(0, payload.timeoutTicks());
    }

    public static void open(OpenBoardDiscardPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new BoardDiscardScreen(payload)));
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void tick() {
        super.tick();
        if (this.timeoutTicks > 0 && --this.timeoutTicks == 0) this.onClose();
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xE20A0A13);
        Component hint = Component.translatable("gui.astral_craft.board.discard_count", this.selected.size(), this.required);
        graphics.text(this.font, this.title, 18, 15, 0xFFFFFFFF, false);
        graphics.text(this.font, hint, 18, 30, this.selected.size() == this.required ? 0xFF80FFA8 : 0xFFFFC75C, false);
        int columns = Math.max(1, (this.width - 30) / (CARD_W + GAP));
        int startX = (this.width - columns * (CARD_W + GAP) + GAP) / 2;
        int startY = 52;
        for (int index = 0; index < this.cards.size(); index++) {
            Card card = this.cards.get(index);
            int x = startX + index % columns * (CARD_W + GAP);
            int y = startY + index / columns * (CARD_H + GAP);
            HandCardRenderHelper.renderFramedCard(graphics, this.font, card.definition().type(),
                    card.definition().largeFrontTexture(card.stack()), card.definition().displayName(card.stack()),
                    x, y, mouseX, mouseY, false);
            if (this.selected.contains(index)) {
                graphics.fill(x, y, x + CARD_W, y + CARD_H, 0x4472FF72);
                graphics.fill(x, y, x + CARD_W, y + 2, 0xFF72FF72);
            }
        }
        int buttonW = 120;
        int buttonH = 26;
        int buttonX = this.width - buttonW - 18;
        int buttonY = this.height - buttonH - 16;
        boolean enabled = this.selected.size() == this.required;
        Component confirm = Component.translatable("gui.astral_craft.board.confirm");
        AstralFancyButton.renderButton(graphics, this.font, confirm, buttonX, buttonY, buttonW, buttonH,
                false, enabled && inside(mouseX, mouseY, buttonX, buttonY, buttonW, buttonH),
                ButtonStyle.button(enabled ? 0xFFD64B91 : 0xFF555560));
        Component timer = Component.translatable("gui.astral_craft.board.timeout", (this.timeoutTicks + 19) / 20);
        graphics.text(this.font, timer, 18, this.height - 24, 0xFFBFC8FF, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        int columns = Math.max(1, (this.width - 30) / (CARD_W + GAP));
        int startX = (this.width - columns * (CARD_W + GAP) + GAP) / 2;
        int startY = 52;
        for (int index = 0; index < this.cards.size(); index++) {
            int x = startX + index % columns * (CARD_W + GAP);
            int y = startY + index / columns * (CARD_H + GAP);
            if (inside(event.x(), event.y(), x, y, CARD_W, CARD_H)) {
                if (!this.selected.remove(index) && this.selected.size() < this.required) this.selected.add(index);
                return true;
            }
        }
        int buttonW = 120;
        int buttonH = 26;
        int buttonX = this.width - buttonW - 18;
        int buttonY = this.height - buttonH - 16;
        if (inside(event.x(), event.y(), buttonX, buttonY, buttonW, buttonH)
                && this.selected.size() == this.required) {
            List<Integer> indexes = this.selected.stream()
                    .map(index -> this.cards.get(index).originalIndex())
                    .toList();
            ClientPacketDistributor.sendToServer(new BoardDiscardPayload(this.boardId, indexes));
            this.onClose();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private static List<Card> decode(String encoded) {
        List<Card> result = new ArrayList<>();
        if (encoded == null || encoded.isBlank()) return result;
        String[] ids = encoded.split(";");
        for (int index = 0; index < ids.length; index++) {
            String raw = ids[index];
            try {
                Identifier id = Identifier.parse(raw);
                Item item = BuiltInRegistries.ITEM.getValue(id);
                if (!(item instanceof BaseHandCard card)) continue;
                ItemStack stack = new ItemStack(item);
                result.add(new Card(index, stack, card.definition(stack)));
            } catch (Exception ignored) {}
        }
        return result;
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private record Card(int originalIndex, ItemStack stack, CardDefinition definition) {}
}
