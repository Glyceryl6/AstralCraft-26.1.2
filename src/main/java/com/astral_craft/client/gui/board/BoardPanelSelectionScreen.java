package com.astral_craft.client.gui.board;

import com.astral_craft.client.gui.AstralStatusIconRenderer;
import com.astral_craft.client.gui.components.AstralFancyButton;
import com.astral_craft.common.network.BoardPanelEdgeView;
import com.astral_craft.common.network.BoardPanelNodeView;
import com.astral_craft.common.network.c2s.BoardPanelSelectionPayload;
import com.astral_craft.common.network.s2c.OpenBoardPanelSelectionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class BoardPanelSelectionScreen extends Screen {

    private final UUID boardId;
    private final ItemStack cardStack;
    private final int handIndex;
    private final List<BoardPanelNodeView> nodes;
    private final List<BoardPanelEdgeView> edges;
    private final Map<Identifier, BoardPanelNodeView> nodesById;
    private boolean submitted;

    public BoardPanelSelectionScreen(OpenBoardPanelSelectionPayload payload) {
        super(Component.translatable("gui.astral_craft.board.panel_selection"));
        this.boardId = payload.boardId();
        this.cardStack = payload.cardStack();
        this.handIndex = payload.handIndex();
        this.nodes = payload.nodes();
        this.edges = payload.edges();
        this.nodesById = new HashMap<>();
        this.nodes.forEach(node -> this.nodesById.put(node.nodeId(), node));
    }

    public static void open(OpenBoardPanelSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new BoardPanelSelectionScreen(payload)));
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
        graphics.fill(layout.panelX(), layout.panelY(), layout.panelX() + layout.panelWidth(),
                layout.panelY() + layout.panelHeight(), 0xEE10111B);
        graphics.fill(layout.panelX(), layout.panelY(), layout.panelX() + layout.panelWidth(),
                layout.panelY() + 2, 0xFFD46AF0);
        Component cardName = this.cardStack.getHoverName();
        graphics.text(this.font, cardName, this.width / 2 - this.font.width(cardName) / 2,
                layout.panelY() + 10, 0xFFFFFFFF, true);
        Component hint = Component.translatable("gui.astral_craft.board.panel_selection.hint");
        graphics.text(this.font, hint, this.width / 2 - this.font.width(hint) / 2,
                layout.panelY() + 24, 0xFFBFC5D8, false);
        Transform transform = this.transform(layout);
        for (BoardPanelEdgeView edge : this.edges) {
            BoardPanelNodeView first = this.nodesById.get(edge.firstNodeId());
            BoardPanelNodeView second = this.nodesById.get(edge.secondNodeId());
            if (first == null || second == null) continue;
            Point a = transform.point(first);
            Point b = transform.point(second);
            drawLine(graphics, a.x(), a.y(), b.x(), b.y(), 0xFF656B7D);
        }

        for (BoardPanelNodeView node : this.nodes) {
            Point point = transform.point(node);
            boolean hovered = inside(mouseX, mouseY, point.x() - 7, point.y() - 7, 14, 14);
            int fill = node.valid() ? (hovered ? 0xFFFFD85D : 0xFF7FD77B) : 0xFF434754;
            graphics.fill(point.x() - 5, point.y() - 5, point.x() + 6, point.y() + 6, 0xFF0B0C12);
            graphics.fill(point.x() - 4, point.y() - 4, point.x() + 5, point.y() + 5, fill);
            int offset = -(node.occupants().size() - 1) * 4;
            for (int index = 0; index < node.occupants().size(); index++) {
                var occupant = node.occupants().get(index);
                AstralStatusIconRenderer.renderCharacterSkinHead(graphics, occupant.characterId(),
                        occupant.skinId().getPath(), point.x() + offset + index * 8 - 5,
                        point.y() - 18, 11, 255);
            }
        }

        AstralFancyButton.renderButton(graphics, this.font,
                Component.translatable("gui.cancel"), layout.cancelX(), layout.cancelY(),
                layout.cancelWidth(), 28, false,
                inside(mouseX, mouseY, layout.cancelX(), layout.cancelY(), layout.cancelWidth(), 28),
                AstralFancyButton.ButtonStyle.button(0xFF6B7080));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.submitted || event.button() != 0) return super.mouseClicked(event, doubleClick);
        Layout layout = this.layout();
        if (inside(event.x(), event.y(), layout.cancelX(), layout.cancelY(), layout.cancelWidth(), 28)) {
            this.submit(null);
            return true;
        }

        Transform transform = this.transform(layout);
        for (BoardPanelNodeView node : this.nodes) {
            if (!node.valid()) continue;
            Point point = transform.point(node);
            if (inside(event.x(), event.y(), point.x() - 8, point.y() - 8, 16, 16)) {
                this.submit(node.nodeId());
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        if (!this.submitted) this.submit(null);
        super.onClose();
    }

    private void submit(@Nullable Identifier nodeId) {
        if (this.submitted) return;
        this.submitted = true;
        ClientPacketDistributor.sendToServer(new BoardPanelSelectionPayload(this.boardId,
                this.cardStack, this.handIndex, nodeId));
        Minecraft.getInstance().setScreen(null);
    }

    private Transform transform(Layout layout) {
        int minX = this.nodes.stream().mapToInt(node -> node.position().getX()).min().orElse(0);
        int maxX = this.nodes.stream().mapToInt(node -> node.position().getX()).max().orElse(minX + 1);
        int minZ = this.nodes.stream().mapToInt(node -> node.position().getZ()).min().orElse(0);
        int maxZ = this.nodes.stream().mapToInt(node -> node.position().getZ()).max().orElse(minZ + 1);
        double scaleX = (layout.mapWidth() - 28.0D) / Math.max(1, maxX - minX);
        double scaleZ = (layout.mapHeight() - 28.0D) / Math.max(1, maxZ - minZ);
        double scale = Math.min(scaleX, scaleZ);
        return new Transform(layout.mapX() + 14, layout.mapY() + 14, minX, minZ, scale);
    }

    private Layout layout() {
        int panelWidth = Math.min(720, this.width - 24);
        int panelHeight = Math.min(520, this.height - 24);
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;
        return new Layout(panelX, panelY, panelWidth, panelHeight,
                panelX + 18, panelY + 42, panelWidth - 36, panelHeight - 94,
                panelX + panelWidth / 2 - 70, panelY + panelHeight - 40, 140);
    }

    private static void drawLine(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps <= 0) return;
        for (int step = 0; step <= steps; step++) {
            int x = x1 + Math.round(dx * (step / (float) steps));
            int y = y1 + Math.round(dy * (step / (float) steps));
            graphics.fill(x, y, x + 2, y + 2, color);
        }
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private record Point(int x, int y) {}

    private record Transform(int originX, int originY, int minX, int minZ, double scale) {
        Point point(BoardPanelNodeView node) {
            return new Point(this.originX + (int) Math.round((node.position().getX() - this.minX) * this.scale),
                    this.originY + (int) Math.round((node.position().getZ() - this.minZ) * this.scale));
        }
    }

    private record Layout(int panelX, int panelY, int panelWidth, int panelHeight,
                          int mapX, int mapY, int mapWidth, int mapHeight,
                          int cancelX, int cancelY, int cancelWidth) {}

}