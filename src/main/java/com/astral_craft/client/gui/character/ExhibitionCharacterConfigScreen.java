package com.astral_craft.client.gui.character;

import com.astral_craft.client.gameplay.character.ClientCharacterDefinitionCache;
import com.astral_craft.client.gui.AstralStatusIconRenderer;
import com.astral_craft.client.gui.components.AstralFancyButton;
import com.astral_craft.client.model.character.AstralGeoPose;
import com.astral_craft.client.render.character.AstralCharacterRenderState;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.entity.character.ExhibitionCharacterEntity;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.skin.CharacterSkinDefinition;
import com.astral_craft.common.network.c2s.ExhibitionCharacterConfigPayload;
import com.astral_craft.common.network.s2c.OpenExhibitionCharacterConfigPayload;
import com.astral_craft.common.registry.AstralEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

public class ExhibitionCharacterConfigScreen extends Screen {

    private static final int CHARACTER_CARD_W = 64;
    private static final int CHARACTER_CARD_H = 52;
    private static final int SKIN_CARD_W = 78;
    private static final int SKIN_CARD_H = 48;
    private static final int GAP = 6;
    private final int entityId;
    private final List<CharacterDefinition> characters;
    private final Identifier initialCharacterId;
    private final String initialSkinId;
    private final float initialYaw;
    private final float initialScale;
    private final boolean initialShowName;
    private final String initialSpeechText;
    private Identifier selectedCharacterId;
    private String selectedSkinId;
    private float yaw;
    private float scale;
    private boolean showName;
    private String speechText;
    private float characterScroll;
    private float skinScroll;
    private boolean draggingPreview;
    private double lastDragX;
    private boolean submitted;
    private boolean syncingFields;
    private EditBox yawBox;
    private EditBox scaleBox;
    private EditBox speechBox;
    private AstralCharacterEntity previewEntity;
    private ExhibitionCharacterEntity livePreviewEntity;

    public ExhibitionCharacterConfigScreen(OpenExhibitionCharacterConfigPayload payload) {
        super(Component.translatable("gui.astral_craft.exhibition_character.title"));
        this.entityId = payload.entityId();
        this.characters = payload.characters().isEmpty() ? List.of(CharacterDefinition.builtinDefault()) : List.copyOf(payload.characters());
        ClientCharacterDefinitionCache.INSTANCE.replace(this.characters);
        this.initialCharacterId = this.hasCharacter(payload.characterId()) ? payload.characterId() : this.characters.getFirst().id();
        this.initialSkinId = this.validSkin(this.character(this.initialCharacterId), payload.skinId());
        this.initialYaw = Mth.wrapDegrees(payload.yaw());
        this.initialScale = Mth.clamp(payload.scale(), ExhibitionCharacterEntity.MIN_SCALE, ExhibitionCharacterEntity.MAX_SCALE);
        this.initialShowName = payload.showName();
        this.initialSpeechText = payload.speechText();
        this.selectedCharacterId = this.initialCharacterId;
        this.selectedSkinId = this.initialSkinId;
        this.yaw = this.initialYaw;
        this.scale = this.initialScale;
        this.showName = this.initialShowName;
        this.speechText = this.initialSpeechText;
    }

    public static void open(OpenExhibitionCharacterConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new ExhibitionCharacterConfigScreen(payload)));
    }

    @Override
    protected void init() {
        Layout layout = this.layout();
        this.yawBox = this.addRenderableWidget(new EditBox(this.font, layout.formX(), layout.yawBoxY(), layout.numberBoxW(), 20,
                Component.translatable("gui.astral_craft.exhibition_character.yaw")));
        this.yawBox.setMaxLength(10);
        this.yawBox.setFilter(ExhibitionCharacterConfigScreen::signedDecimalOrEmpty);
        this.scaleBox = this.addRenderableWidget(new EditBox(this.font, layout.formX() + layout.numberBoxW() + GAP, layout.yawBoxY(), layout.numberBoxW(), 20,
                Component.translatable("gui.astral_craft.exhibition_character.scale")));
        this.scaleBox.setMaxLength(8);
        this.scaleBox.setFilter(ExhibitionCharacterConfigScreen::decimalOrEmpty);
        this.speechBox = this.addRenderableWidget(new EditBox(this.font, layout.formX(), layout.speechBoxY(), layout.speechBoxW(), 20,
                Component.translatable("gui.astral_craft.exhibition_character.speech")));
        this.speechBox.setMaxLength(ExhibitionCharacterEntity.MAX_SPEECH_LENGTH);
        this.speechBox.setHint(Component.translatable("gui.astral_craft.exhibition_character.speech_hint"));
        this.syncFields();
        this.yawBox.setResponder(this::yawChanged);
        this.scaleBox.setResponder(this::scaleChanged);
        this.speechBox.setResponder(value -> {
            if (this.syncingFields) return;
            this.speechText = value;
            this.applyLivePreview();
        });
        this.applyLivePreview();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.livePreviewEntity == null || this.livePreviewEntity.isRemoved()) this.applyLivePreview();
    }

    @Override
    public void removed() {
        AstralFancyButton.setHandCursor(false);
        if (!this.submitted) this.restoreLivePreview();
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
        graphics.fill(layout.x(), layout.y(), layout.right(), layout.bottom(), 0xF0151723);
        graphics.fill(layout.x(), layout.y(), layout.right(), layout.y() + 3, 0xFFE83CA8);
        graphics.centeredText(this.font, this.title, this.width / 2, layout.y() + 12, 0xFFFFFFFF);
        this.renderPreview(graphics, layout, mouseX, mouseY);
        this.renderCharacters(graphics, layout, mouseX, mouseY);
        this.renderSkins(graphics, layout, mouseX, mouseY);
        this.renderForm(graphics, layout, mouseX, mouseY);
        this.renderActions(graphics, layout, mouseX, mouseY);
        AstralFancyButton.setHandCursor(this.hoveredClickable(layout, mouseX, mouseY));
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.submitted) return true;
        Layout layout = this.layout();
        double mouseX = event.x();
        double mouseY = event.y();
        if (event.button() == 0 && this.isInside(mouseX, mouseY, layout.previewX(), layout.previewY(), layout.previewW(), layout.previewH())) {
            if (doubleClick) {
                this.setYaw(0.0F, true);
                return true;
            }
            this.draggingPreview = true;
            this.lastDragX = mouseX;
            return true;
        }

        if (event.button() == 0 && this.handleCharacterClick(layout, mouseX, mouseY)) return true;
        if (event.button() == 0 && this.handleSkinClick(layout, mouseX, mouseY)) return true;
        if (event.button() == 0 && this.isInside(mouseX, mouseY, layout.nameButtonX(), layout.nameButtonY(), layout.nameButtonW(), 20)) {
            this.showName = !this.showName;
            this.applyLivePreview();
            return true;
        }
        if (event.button() == 0 && this.isInside(mouseX, mouseY, layout.confirmX(), layout.buttonY(), layout.actionButtonW(), 28) && this.validInput()) {
            this.readNumericFields();
            this.submitted = true;
            ClientPacketDistributor.sendToServer(this.payload(false));
            this.onClose();
            return true;
        }
        if (event.button() == 0 && this.isInside(mouseX, mouseY, layout.cancelX(), layout.buttonY(), layout.actionButtonW(), 28)) {
            this.onClose();
            return true;
        }
        if (event.button() == 0 && this.isInside(mouseX, mouseY, layout.removeX(), layout.buttonY(), layout.actionButtonW(), 28)) {
            this.submitted = true;
            ClientPacketDistributor.sendToServer(this.payload(true));
            this.onClose();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        if (this.draggingPreview) {
            float delta = (float) (event.x() - this.lastDragX);
            this.lastDragX = event.x();
            this.setYaw(this.yaw + delta * 0.82F, true);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        if (this.draggingPreview) {
            this.draggingPreview = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        Layout layout = this.layout();
        if (this.isInside(mouseX, mouseY, layout.previewX(), layout.previewY(), layout.previewW(), layout.previewH())) {
            this.setScale(this.scale + (float) deltaY * 0.1F, true);
            return true;
        }
        if (this.isInside(mouseX, mouseY, layout.characterX(), layout.characterY(), layout.characterW(), layout.characterH())) {
            this.characterScroll = Mth.clamp(this.characterScroll - (float) deltaY * 28.0F, 0.0F, this.maxCharacterScroll(layout));
            return true;
        }
        if (this.isInside(mouseX, mouseY, layout.skinX(), layout.skinY(), layout.skinW(), SKIN_CARD_H)) {
            this.skinScroll = Mth.clamp(this.skinScroll - (float) deltaY * 36.0F, 0.0F, this.maxSkinScroll(layout));
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
        if (event.key() == GLFW.GLFW_KEY_R && (this.yawBox == null || !this.yawBox.isFocused()) && (this.scaleBox == null || !this.scaleBox.isFocused()) && (this.speechBox == null || !this.speechBox.isFocused())) {
            this.setYaw(0.0F, true);
            this.setScale(1.0F, true);
            return true;
        }
        return super.keyPressed(event);
    }

    private void renderPreview(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        boolean hovered = this.isInside(mouseX, mouseY, layout.previewX(), layout.previewY(), layout.previewW(), layout.previewH());
        AstralFancyButton.renderOutlinedBox(graphics, layout.previewX(), layout.previewY(), layout.previewW(), layout.previewH(),
                hovered ? 0xB51F2332 : 0xA5161924, hovered ? 0xFFE83CA8 : 0xFF8F95A9, 0xFF101018, 1, 2);
        CharacterDefinition definition = this.selectedCharacter();
        LivingEntity entity = this.configuredPreviewEntity(definition, this.selectedSkinId);
        if (entity != null) {
            int modelTop = layout.previewY() + 28;
            int modelBottom = layout.previewBottom() - 32;
            this.renderEntityModel(graphics, entity, layout.previewX() + 8, modelTop, layout.previewRight() - 8, modelBottom,
                    180.0F - this.yaw, -8.0F, 0.0F, Mth.clamp((float) Math.sqrt(this.scale), 0.5F, 2.0F));
        }
        graphics.centeredText(this.font, Component.translatable(definition.getDescriptionId()), layout.previewX() + layout.previewW() / 2, layout.previewY() + 10, 0xFFFFFFFF);
        graphics.centeredText(this.font, Component.translatable("gui.astral_craft.exhibition_character.drag_hint"), layout.previewX() + layout.previewW() / 2, layout.previewBottom() - 18, 0xFFAEB8CB);
        graphics.centeredText(this.font, Component.translatable("gui.astral_craft.exhibition_character.preview_values", this.format(this.yaw), this.format(this.scale)),
                layout.previewX() + layout.previewW() / 2, layout.previewBottom() - 8, 0xFFCDD4E3);
    }

    private void renderCharacters(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        graphics.text(this.font, Component.translatable("gui.astral_craft.exhibition_character.character"), layout.characterX(), layout.characterTitleY(), 0xFFD7E4F2);
        graphics.enableScissor(layout.characterX(), layout.characterY(), layout.characterRight(), layout.characterBottom());
        int columns = this.characterColumns(layout);
        for (int index = 0; index < this.characters.size(); index++) {
            CharacterDefinition definition = this.characters.get(index);
            int column = index % columns;
            int row = index / columns;
            int x = layout.characterX() + column * (CHARACTER_CARD_W + GAP);
            int y = layout.characterY() + row * (CHARACTER_CARD_H + GAP) - Math.round(this.characterScroll);
            if (y + CHARACTER_CARD_H < layout.characterY() || y > layout.characterBottom()) continue;
            boolean selected = definition.id().equals(this.selectedCharacterId);
            boolean hovered = this.isInside(mouseX, mouseY, x, y, CHARACTER_CARD_W, CHARACTER_CARD_H);
            AstralFancyButton.renderIconFrame(graphics, x, y, CHARACTER_CARD_W, CHARACTER_CARD_H, selected, hovered);
            String skin = selected ? this.selectedSkinId : this.validSkin(definition, "");
            AstralStatusIconRenderer.renderCharacterSkinHead(graphics, definition.id(), skin, x + 20, y + 4, 24, 255, false);
            String name = this.font.plainSubstrByWidth(Component.translatable(definition.getDescriptionId()).getString(), CHARACTER_CARD_W - 6);
            graphics.centeredText(this.font, name, x + CHARACTER_CARD_W / 2, y + 36, 0xFFFFFFFF);
        }
        graphics.disableScissor();
    }

    private void renderSkins(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        graphics.text(this.font, Component.translatable("gui.astral_craft.exhibition_character.skin"), layout.skinX(), layout.skinTitleY(), 0xFFD7E4F2);
        graphics.enableScissor(layout.skinX(), layout.skinY(), layout.skinRight(), layout.skinBottom());
        List<CharacterSkinDefinition> skins = this.selectedCharacter().skins();
        for (int index = 0; index < skins.size(); index++) {
            CharacterSkinDefinition skin = skins.get(index);
            int x = layout.skinX() + index * (SKIN_CARD_W + GAP) - Math.round(this.skinScroll);
            boolean selected = skin.id().equals(this.selectedSkinId);
            boolean hovered = this.isInside(mouseX, mouseY, x, layout.skinY(), SKIN_CARD_W, SKIN_CARD_H);
            AstralFancyButton.renderIconFrame(graphics, x, layout.skinY(), SKIN_CARD_W, SKIN_CARD_H, selected, hovered);
            AstralStatusIconRenderer.renderCharacterSkinHead(graphics, this.selectedCharacterId, skin.id(), x + 6, layout.skinY() + 8, 28, 255, false);
            String name = this.font.plainSubstrByWidth(Component.translatable(skin.nameKey()).getString(), SKIN_CARD_W - 40);
            graphics.text(this.font, name, x + 38, layout.skinY() + 20, 0xFFFFFFFF, false);
        }
        graphics.disableScissor();
    }

    private void renderForm(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        graphics.text(this.font, Component.translatable("gui.astral_craft.exhibition_character.yaw"), layout.formX(), layout.formY(), 0xFFD7E4F2);
        graphics.text(this.font, Component.translatable("gui.astral_craft.exhibition_character.scale"), layout.formX() + layout.numberBoxW() + GAP, layout.formY(), 0xFFD7E4F2);
        graphics.text(this.font, Component.translatable("gui.astral_craft.exhibition_character.speech"), layout.formX(), layout.speechLabelY(), 0xFFD7E4F2);
        boolean nameHover = this.isInside(mouseX, mouseY, layout.nameButtonX(), layout.nameButtonY(), layout.nameButtonW(), 20);
        AstralFancyButton.renderButton(graphics, this.font, Component.translatable(this.showName
                        ? "gui.astral_craft.exhibition_character.name_shown" : "gui.astral_craft.exhibition_character.name_hidden"),
                layout.nameButtonX(), layout.nameButtonY(), layout.nameButtonW(), 20, this.showName, nameHover,
                AstralFancyButton.ButtonStyle.button(0xFF5664B7));
    }

    private void renderActions(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        boolean valid = this.validInput();
        AstralFancyButton.renderButton(graphics, this.font, Component.translatable("gui.astral_craft.confirm"),
                layout.confirmX(), layout.buttonY(), layout.actionButtonW(), 28, !valid || this.submitted,
                valid && this.isInside(mouseX, mouseY, layout.confirmX(), layout.buttonY(), layout.actionButtonW(), 28),
                AstralFancyButton.ButtonStyle.button(0xFF4F9D69));
        AstralFancyButton.renderButton(graphics, this.font, Component.translatable("gui.astral_craft.cancel"),
                layout.cancelX(), layout.buttonY(), layout.actionButtonW(), 28, this.submitted,
                this.isInside(mouseX, mouseY, layout.cancelX(), layout.buttonY(), layout.actionButtonW(), 28),
                AstralFancyButton.ButtonStyle.button(0xFF646477));
        AstralFancyButton.renderButton(graphics, this.font, Component.translatable("gui.astral_craft.exhibition_character.remove"),
                layout.removeX(), layout.buttonY(), layout.actionButtonW(), 28, this.submitted,
                this.isInside(mouseX, mouseY, layout.removeX(), layout.buttonY(), layout.actionButtonW(), 28),
                AstralFancyButton.ButtonStyle.button(0xFF9B5360));
    }

    private boolean handleCharacterClick(Layout layout, double mouseX, double mouseY) {
        if (!this.isInside(mouseX, mouseY, layout.characterX(), layout.characterY(), layout.characterW(), layout.characterH())) return false;
        int columns = this.characterColumns(layout);
        for (int index = 0; index < this.characters.size(); index++) {
            int x = layout.characterX() + index % columns * (CHARACTER_CARD_W + GAP);
            int y = layout.characterY() + index / columns * (CHARACTER_CARD_H + GAP) - Math.round(this.characterScroll);
            if (!this.isInside(mouseX, mouseY, x, y, CHARACTER_CARD_W, CHARACTER_CARD_H)) continue;
            CharacterDefinition definition = this.characters.get(index);
            this.selectedCharacterId = definition.id();
            this.selectedSkinId = this.validSkin(definition, this.selectedSkinId);
            this.skinScroll = 0.0F;
            this.applyLivePreview();
            return true;
        }
        return true;
    }

    private boolean handleSkinClick(Layout layout, double mouseX, double mouseY) {
        if (!this.isInside(mouseX, mouseY, layout.skinX(), layout.skinY(), layout.skinW(), SKIN_CARD_H)) return false;
        List<CharacterSkinDefinition> skins = this.selectedCharacter().skins();
        for (int index = 0; index < skins.size(); index++) {
            int x = layout.skinX() + index * (SKIN_CARD_W + GAP) - Math.round(this.skinScroll);
            if (!this.isInside(mouseX, mouseY, x, layout.skinY(), SKIN_CARD_W, SKIN_CARD_H)) continue;
            this.selectedSkinId = skins.get(index).id();
            this.applyLivePreview();
            return true;
        }
        return true;
    }

    private void applyLivePreview() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        if (!(minecraft.level.getEntity(this.entityId) instanceof ExhibitionCharacterEntity entity)) return;
        this.livePreviewEntity = entity;
        CharacterDefinition definition = this.selectedCharacter();
        entity.setCharacterId(definition.id());
        entity.setSkinId(this.selectedSkinId);
        entity.setExhibitionYaw(this.yaw);
        entity.setDisplayScale(this.scale);
        entity.setCustomName(Component.translatable(definition.getDescriptionId()));
        entity.setCustomNameVisible(this.showName);
        entity.setSpeechText(this.speechText);
    }

    private void restoreLivePreview() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !(minecraft.level.getEntity(this.entityId) instanceof ExhibitionCharacterEntity entity)) return;
        CharacterDefinition definition = this.character(this.initialCharacterId);
        entity.setCharacterId(this.initialCharacterId);
        entity.setSkinId(this.initialSkinId);
        entity.setExhibitionYaw(this.initialYaw);
        entity.setDisplayScale(this.initialScale);
        entity.setCustomName(Component.translatable(definition.getDescriptionId()));
        entity.setCustomNameVisible(this.initialShowName);
        entity.setSpeechText(this.initialSpeechText);
    }

    private ExhibitionCharacterConfigPayload payload(boolean remove) {
        return new ExhibitionCharacterConfigPayload(this.entityId, this.selectedCharacterId, this.selectedSkinId,
                this.yaw, this.scale, this.showName, this.speechText, remove);
    }

    private void yawChanged(String value) {
        if (this.syncingFields) return;
        Float parsed = this.parseFloat(value);
        if (parsed == null) return;
        this.yaw = Mth.wrapDegrees(parsed);
        this.applyLivePreview();
    }

    private void scaleChanged(String value) {
        if (this.syncingFields) return;
        Float parsed = this.parseFloat(value);
        if (parsed == null || parsed < ExhibitionCharacterEntity.MIN_SCALE || parsed > ExhibitionCharacterEntity.MAX_SCALE) return;
        this.scale = parsed;
        this.applyLivePreview();
    }

    private void setYaw(float yaw, boolean syncField) {
        this.yaw = Mth.wrapDegrees(yaw);
        if (syncField && this.yawBox != null) {
            this.syncingFields = true;
            this.yawBox.setValue(this.format(this.yaw));
            this.syncingFields = false;
        }
        this.applyLivePreview();
    }

    private void setScale(float scale, boolean syncField) {
        this.scale = Mth.clamp(scale, ExhibitionCharacterEntity.MIN_SCALE, ExhibitionCharacterEntity.MAX_SCALE);
        if (syncField && this.scaleBox != null) {
            this.syncingFields = true;
            this.scaleBox.setValue(this.format(this.scale));
            this.syncingFields = false;
        }
        this.applyLivePreview();
    }

    private void syncFields() {
        this.syncingFields = true;
        this.yawBox.setValue(this.format(this.yaw));
        this.scaleBox.setValue(this.format(this.scale));
        this.speechBox.setValue(this.speechText);
        this.syncingFields = false;
    }

    private void readNumericFields() {
        Float parsedYaw = this.parseFloat(this.yawBox.getValue());
        Float parsedScale = this.parseFloat(this.scaleBox.getValue());
        if (parsedYaw != null) this.yaw = Mth.wrapDegrees(parsedYaw);
        if (parsedScale != null) this.scale = Mth.clamp(parsedScale, ExhibitionCharacterEntity.MIN_SCALE, ExhibitionCharacterEntity.MAX_SCALE);
        this.speechText = this.speechBox.getValue();
    }

    private boolean validInput() {
        if (this.yawBox == null || this.scaleBox == null || this.speechBox == null) return false;
        Float parsedYaw = this.parseFloat(this.yawBox.getValue());
        Float parsedScale = this.parseFloat(this.scaleBox.getValue());
        return parsedYaw != null && Float.isFinite(parsedYaw) && parsedScale != null && Float.isFinite(parsedScale)
                && parsedScale >= ExhibitionCharacterEntity.MIN_SCALE && parsedScale <= ExhibitionCharacterEntity.MAX_SCALE;
    }

    private CharacterDefinition selectedCharacter() {
        return this.character(this.selectedCharacterId);
    }

    private CharacterDefinition character(Identifier id) {
        for (CharacterDefinition definition : this.characters) if (definition.id().equals(id)) return definition;
        return this.characters.getFirst();
    }

    private boolean hasCharacter(Identifier id) {
        if (id == null) return false;
        return this.characters.stream().anyMatch(definition -> definition.id().equals(id));
    }

    private String validSkin(CharacterDefinition definition, String preferred) {
        if (definition == null || definition.skins().isEmpty()) return "default";
        return definition.skins().stream().filter(skin -> skin.id().equals(preferred)).map(CharacterSkinDefinition::id)
                .findFirst().orElse(definition.skins().getFirst().id());
    }

    private AstralCharacterEntity configuredPreviewEntity(CharacterDefinition definition, String skinId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || definition == null) return null;
        if (this.previewEntity == null) this.previewEntity = new AstralCharacterEntity(AstralEntities.ASTRAL_CHARACTER.get(), minecraft.level);
        this.previewEntity.setCharacterId(definition.id());
        this.previewEntity.setSkinId(this.validSkin(definition, skinId));
        this.previewEntity.setAnimationAction(definition.previewAction());
        this.previewEntity.tickCount = Math.max(0, minecraft.level.getGameTime() > Integer.MAX_VALUE ? 0 : (int) minecraft.level.getGameTime());
        return this.previewEntity;
    }

    private void renderEntityModel(GuiGraphicsExtractor graphics, LivingEntity entity, int x0, int y0, int x1, int y1, float yaw, float pitch, float roll, float scaleMultiplier) {
        EntityRenderState renderState = this.extractEntityRenderState(entity);
        if (renderState instanceof LivingEntityRenderState livingState) {
            livingState.bodyRot = 0.0F;
            livingState.yRot = 0.0F;
            livingState.xRot = 0.0F;
            livingState.scale = 1.0F;
        }
        float boxWidth = Math.max(0.35F, renderState.boundingBoxWidth);
        float boxHeight = Math.max(1.2F, renderState.boundingBoxHeight);
        float viewWidth = Math.max(1.0F, x1 - x0);
        float viewHeight = Math.max(1.0F, y1 - y0);
        float renderScale = Math.min(viewWidth / (boxWidth * 1.35F), viewHeight / (boxHeight * 1.10F)) * scaleMultiplier;
        renderScale = Mth.clamp(renderScale, 8.0F, 130.0F);
        AstralGeoPose pose = renderState instanceof AstralCharacterRenderState astralState ? astralState.rootPose : AstralGeoPose.IDENTITY;
        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.toRadians(180.0F))
                .rotateX((float) Math.toRadians(pitch + pose.rotation().x()))
                .rotateY((float) Math.toRadians(yaw + pose.rotation().y()))
                .rotateZ((float) Math.toRadians(roll + pose.rotation().z()));
        Vector3f translation = new Vector3f(pose.position().x() / 16.0F, boxHeight * 0.48F - pose.position().y() / 16.0F, pose.position().z() / 16.0F);
        graphics.entity(renderState, renderScale, translation, rotation, null, x0, y0, x1, y1);
    }

    private EntityRenderState extractEntityRenderState(LivingEntity entity) {
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        EntityRenderer<? super LivingEntity, ?> renderer = dispatcher.getRenderer(entity);
        EntityRenderState renderState = renderer.createRenderState(entity, 1.0F);
        renderState.shadowPieces.clear();
        renderState.outlineColor = 0;
        return renderState;
    }

    private boolean hoveredClickable(Layout layout, double mouseX, double mouseY) {
        if (this.isInside(mouseX, mouseY, layout.previewX(), layout.previewY(), layout.previewW(), layout.previewH())) return true;
        if (this.isInside(mouseX, mouseY, layout.characterX(), layout.characterY(), layout.characterW(), layout.characterH())) return true;
        if (this.isInside(mouseX, mouseY, layout.skinX(), layout.skinY(), layout.skinW(), SKIN_CARD_H)) return true;
        if (this.isInside(mouseX, mouseY, layout.nameButtonX(), layout.nameButtonY(), layout.nameButtonW(), 20)) return true;
        return this.isInside(mouseX, mouseY, layout.confirmX(), layout.buttonY(), layout.actionButtonW(), 28)
                || this.isInside(mouseX, mouseY, layout.cancelX(), layout.buttonY(), layout.actionButtonW(), 28)
                || this.isInside(mouseX, mouseY, layout.removeX(), layout.buttonY(), layout.actionButtonW(), 28);
    }

    private int characterColumns(Layout layout) {
        return Math.max(1, (layout.characterW() + GAP) / (CHARACTER_CARD_W + GAP));
    }

    private float maxCharacterScroll(Layout layout) {
        int rows = (this.characters.size() + this.characterColumns(layout) - 1) / this.characterColumns(layout);
        return Math.max(0, rows * (CHARACTER_CARD_H + GAP) - GAP - layout.characterH());
    }

    private float maxSkinScroll(Layout layout) {
        int content = this.selectedCharacter().skins().size() * (SKIN_CARD_W + GAP) - GAP;
        return Math.max(0, content - layout.skinW());
    }

    private Float parseFloat(String value) {
        if (value == null || value.isBlank() || "-".equals(value) || ".".equals(value) || "-.".equals(value)) return null;
        try {
            float parsed = Float.parseFloat(value);
            return Float.isFinite(parsed) ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String format(float value) {
        if (Math.abs(value - Math.round(value)) < 0.0001F) return Integer.toString(Math.round(value));
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private Layout layout() {
        int panelW = Math.min(780, Math.max(340, this.width - 20));
        int panelH = Math.min(500, Math.max(300, this.height - 20));
        int x = (this.width - panelW) / 2;
        int y = (this.height - panelH) / 2;
        int leftW = Math.clamp(panelW * 34 / 100, 190, 272);
        int previewX = x + 12;
        int previewY = y + 34;
        int previewW = leftW - 18;
        int previewH = panelH - 48;
        int rightX = x + leftW + 4;
        int rightW = panelW - leftW - 16;
        int characterTitleY = y + 36;
        int characterY = characterTitleY + 13;
        int characterH = Math.clamp(panelH - 248, 52, 116);
        int skinTitleY = characterY + characterH + 8;
        int skinY = skinTitleY + 13;
        int formY = skinY + SKIN_CARD_H + 14;
        int buttonY = y + panelH - 38;
        int actionButtonW = Math.max(40, (rightW - GAP * 2) / 3);
        int numberBoxW = Math.max(64, (rightW - GAP) / 2);
        return new Layout(x, y, panelW, panelH, previewX, previewY, previewW, previewH,
                rightX, rightW, characterTitleY, characterY, characterH, skinTitleY, skinY,
                formY, numberBoxW, buttonY, actionButtonW);
    }

    private static boolean signedDecimalOrEmpty(String value) {
        if (value.isEmpty() || "-".equals(value)) return true;
        int dots = 0;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '-' && index == 0) continue;
            if (character == '.' && dots++ == 0) continue;
            if (!Character.isDigit(character)) return false;
        }
        return true;
    }

    private static boolean decimalOrEmpty(String value) {
        if (value.isEmpty()) return true;
        int dots = 0;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '.' && dots++ == 0) continue;
            if (!Character.isDigit(character)) return false;
        }
        return true;
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private record Layout(int x, int y, int width, int height, int previewX, int previewY, int previewW, int previewH,
                          int formX, int formW, int characterTitleY, int characterY, int characterH, int skinTitleY,
                          int skinY, int formY, int numberBoxW, int buttonY, int actionButtonW) {
        private int right() { return this.x + this.width; }
        private int bottom() { return this.y + this.height; }
        private int previewRight() { return this.previewX + this.previewW; }
        private int previewBottom() { return this.previewY + this.previewH; }
        private int characterX() { return this.formX; }
        private int characterW() { return this.formW; }
        private int characterRight() { return this.characterX() + this.characterW(); }
        private int characterBottom() { return this.characterY + this.characterH; }
        private int skinX() { return this.formX; }
        private int skinW() { return this.formW; }
        private int skinRight() { return this.skinX() + this.skinW(); }
        private int skinBottom() { return this.skinY + SKIN_CARD_H; }
        private int yawBoxY() { return this.formY + 12; }
        private int speechLabelY() { return this.formY + 40; }
        private int speechBoxY() { return this.formY + 52; }
        private int nameButtonW() { return Math.clamp(this.formW / 3, 54, 128); }
        private int speechBoxW() { return Math.max(48, this.formW - this.nameButtonW() - GAP); }
        private int nameButtonX() { return this.formX + this.speechBoxW() + GAP; }
        private int nameButtonY() { return this.speechBoxY(); }
        private int confirmX() { return this.formX; }
        private int cancelX() { return this.confirmX() + this.actionButtonW + GAP; }
        private int removeX() { return this.cancelX() + this.actionButtonW + GAP; }
    }
}
