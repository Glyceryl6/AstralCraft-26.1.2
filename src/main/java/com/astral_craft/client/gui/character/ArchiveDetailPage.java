package com.astral_craft.client.gui.character;

import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.CharacterProfileSection;
import com.astral_craft.common.gameplay.character.CharacterPotentialDefinition;
import com.astral_craft.common.gameplay.character.CharacterPotentialMaterialRequirement;
import com.astral_craft.common.gameplay.character.CharacterProgressEntry;
import com.astral_craft.common.gameplay.character.skill.CharacterSkillDefinition;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ArchiveDetailPage implements CharacterDetailPage {

    protected final CharacterSettingsScreen screen;
    protected final Map<CharacterSettingsScreen.ArchiveTab, ArchiveSectionPage> sectionPages = new EnumMap<>(CharacterSettingsScreen.ArchiveTab.class);

    public ArchiveDetailPage(CharacterSettingsScreen screen) {
        this.screen = screen;
        this.sectionPages.put(CharacterSettingsScreen.ArchiveTab.SKILLS, new SkillsSection());
        this.sectionPages.put(CharacterSettingsScreen.ArchiveTab.LEVEL, new LevelSection());
        this.sectionPages.put(CharacterSettingsScreen.ArchiveTab.POTENTIAL, new PotentialSection());
        this.sectionPages.put(CharacterSettingsScreen.ArchiveTab.PROFILE, new ProfileSection());
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, CharacterLayout layout, int mouseX, int mouseY) {
        this.screen.renderArchiveTabs(graphics, layout, mouseX, mouseY);
        this.screen.renderBodyContainer(graphics, layout);
        this.currentSection().render(graphics, layout);
    }

    @Override
    public boolean mouseClicked(CharacterLayout layout, double mouseX, double mouseY) {
        return this.screen.handleArchiveTabClick(layout, mouseX, mouseY) || this.currentSection().mouseClicked(layout, mouseX, mouseY);
    }

    @Override
    public float maxScroll(CharacterLayout layout) {
        int visible = this.currentSection().visibleHeight(layout);
        int content = this.currentSection().estimatedHeight(layout);
        return content > visible + 14 ? content - visible : 0.0F;
    }

    @Override
    public int bodyScrollBarX(CharacterLayout layout) {
        return this.currentSection().bodyScrollBarX(layout);
    }

    @Override
    public int bodyScrollBarY(CharacterLayout layout) {
        return this.currentSection().bodyScrollBarY(layout);
    }

    @Override
    public int bodyScrollBarHeight(CharacterLayout layout) {
        return this.currentSection().bodyScrollBarHeight(layout);
    }

    protected ArchiveSectionPage currentSection() {
        return this.sectionPages.getOrDefault(this.screen.archiveTab, this.sectionPages.get(CharacterSettingsScreen.ArchiveTab.SKILLS));
    }

    protected abstract class ArchiveSectionPage {

        protected SectionArea begin(GuiGraphicsExtractor graphics, CharacterLayout layout) {
            int headerX = layout.bodyX + 14;
            int headerY = layout.bodyY + 14;
            int maxWidth = layout.bodyW - 34;
            int contentX = layout.bodyX + 18;
            int contentTop = layout.bodyY + 38;
            int contentBottom = layout.bodyY + layout.bodyH - 12 - this.bottomInset();
            int y = contentTop - Math.round(screen.bodyScroll);
            screen.drawHeader(graphics, Component.translatable(screen.archiveTab.titleKey()), headerX, headerY, screen.archiveTab.headerColor(), maxWidth);
            graphics.enableScissor(layout.bodyX + 8, contentTop, layout.bodyX + layout.bodyW - 8, contentBottom);
            return new SectionArea(contentX, y, maxWidth, contentTop, contentBottom);
        }

        protected void end(GuiGraphicsExtractor graphics, CharacterLayout layout, SectionArea area) {
            graphics.disableScissor();
            screen.renderVerticalScrollbar(graphics, this.bodyScrollBarX(layout), this.bodyScrollBarY(layout), this.bodyScrollBarHeight(layout), screen.bodyScroll, maxScroll(layout));
        }

        boolean mouseClicked(CharacterLayout layout, double mouseX, double mouseY) {
            return false;
        }

        protected int bottomInset() {
            return 0;
        }

        protected int visibleHeight(CharacterLayout layout) {
            return Math.max(10, layout.bodyH - 50 - this.bottomInset());
        }

        protected int bodyScrollBarX(CharacterLayout layout) {
            return layout.bodyX + layout.bodyW - 5;
        }

        protected int bodyScrollBarY(CharacterLayout layout) {
            return layout.bodyY + 38;
        }

        protected int bodyScrollBarHeight(CharacterLayout layout) {
            return this.visibleHeight(layout);
        }

        abstract void render(GuiGraphicsExtractor graphics, CharacterLayout layout);

        abstract int estimatedHeight(CharacterLayout layout);

    }

    protected record SectionArea(int contentX, int y, int maxWidth, int contentTop, int contentBottom) {}

    protected class SkillsSection extends ArchiveSectionPage {

        @Override
        void render(GuiGraphicsExtractor graphics, CharacterLayout layout) {
            SectionArea area = this.begin(graphics, layout);
            int y = area.y();
            CharacterDefinition definition = screen.selectedCharacter();
            if (this.shouldShowModeSwitch(definition)) {
                this.renderModeSwitch(graphics, layout, area.contentX(), y, area.maxWidth() - 8);
                y += 31;
            }

            for (CharacterSkillDefinition skill : definition.skills()) {
                String prefix = "character.astral_craft.skill." + skill.serializedId();
                int nameColor = skill.id().isActive() ? ARGB.color(255, 191, 0) : ARGB.color(152, 252, 253);
                y = screen.drawHeader(graphics, Component.translatable(prefix, Component.translatable(definition.skillNameKey(skill, screen.skillMode))), area.contentX(), y, nameColor, area.maxWidth() - 8);
                y = screen.drawWrapped(graphics, Component.translatable(definition.skillDescriptionKey(skill, screen.skillMode)), area.contentX() + 8, y + 2, 0xFFE7E7E7, area.maxWidth() - 16);
                int cooldown = skill.cooldown(screen.skillMode);
                if (cooldown > 0) {
                    y = screen.drawLine(graphics, Component.translatable("gui.astral_craft.character_settings.cooldown", cooldown), area.contentX() + 8, y + 2, 0xFFB0B0B0, area.maxWidth() - 16);
                }

                y += 8;
            }

            if (definition.skills().isEmpty()) {
                screen.drawWrapped(graphics, Component.translatable("gui.astral_craft.character_settings.empty_skills"), area.contentX(), y, 0xFFD0D0D0, area.maxWidth() - 8);
            }

            this.end(graphics, layout, area);
        }

        @Override
        int estimatedHeight(CharacterLayout layout) {
            CharacterDefinition definition = screen.selectedCharacter();
            int maxWidth = Math.max(40, layout.bodyW - 52);
            int height = 10;
            if (definition.skills().isEmpty()) {
                return screen.wrappedHeight(Component.translatable("gui.astral_craft.character_settings.empty_skills"), maxWidth) + 12;
            }

            if (this.shouldShowModeSwitch(definition)) {
                height += 31;
            }

            for (CharacterSkillDefinition skill : definition.skills()) {
                height += 16;
                height += screen.wrappedHeight(Component.translatable(definition.skillDescriptionKey(skill, screen.skillMode)), maxWidth - 8) + 10;
                if (skill.cooldown(screen.skillMode) > 0) {
                    height += 14;
                }
            }

            return height;
        }

        @Override
        boolean mouseClicked(CharacterLayout layout, double mouseX, double mouseY) {
            CharacterDefinition definition = screen.selectedCharacter();
            if (!this.shouldShowModeSwitch(definition)) return false;
            int x = layout.bodyX + 26;
            int y = layout.bodyY + 38 - Math.round(screen.bodyScroll);
            int w = Math.clamp(layout.bodyW - 64, 128, 174);
            if (!screen.isInside(mouseX, mouseY, x, y, w, 22)) return false;
            screen.skillMode = mouseX < x + w / 2.0D ? CharacterSkillDefinition.SkillMode.PVP : CharacterSkillDefinition.SkillMode.PVE;
            screen.bodyScroll = 0.0F;
            return true;
        }

        private boolean shouldShowModeSwitch(CharacterDefinition definition) {
            return definition.skills().stream().anyMatch(CharacterSkillDefinition::hasModeSpecificText);
        }

        private void renderModeSwitch(GuiGraphicsExtractor graphics, CharacterLayout layout, int x, int y, int maxWidth) {
            int w = Math.clamp(maxWidth, 128, 174);
            int h = 22;
            boolean pvp = screen.skillMode == CharacterSkillDefinition.SkillMode.PVP;
            graphics.fill(x, y, x + w, y + h, 0xAA101018);
            graphics.fill(x, y, x + w, y + 1, 0x80FFFFFF);
            int knobX = pvp ? x + 2 : x + w / 2;
            graphics.fill(knobX, y + 2, knobX + w / 2 - 2, y + h - 2, pvp ? 0xCC9B64FF : 0xCCFF5FB8);
            MutableComponent pvpText = Component.translatable("gui.astral_craft.character_settings.skill_mode.pvp");
            MutableComponent pveText = Component.translatable("gui.astral_craft.character_settings.skill_mode.pve");
            screen.drawCenteredText(graphics, pvpText.withStyle(ChatFormatting.BOLD), x, y + 7, w / 2, pvp ? 0xFFFFFFFF : 0xFFC8C8D8);
            screen.drawCenteredText(graphics, pveText.withStyle(ChatFormatting.BOLD), x + w / 2, y + 7, w / 2, pvp ? 0xFFC8C8D8 : 0xFFFFFFFF);
        }

    }

    protected class LevelSection extends ArchiveSectionPage {

        @Override
        void render(GuiGraphicsExtractor graphics, CharacterLayout layout) {
            SectionArea area = this.begin(graphics, layout);
            int y = area.y();
            CharacterDefinition definition = screen.selectedCharacter();
            y = screen.drawLine(graphics, Component.translatable("gui.astral_craft.character_settings.level_value", screen.level, definition.maxPveLevel()), area.contentX() + 8, y + 2, 0xFFFFFFFF, area.maxWidth() - 16);
            y = screen.drawLine(graphics, Component.translatable("gui.astral_craft.character_settings.experience_value", screen.experience), area.contentX() + 8, y + 2, 0xFFBFE6FF, area.maxWidth() - 16);
            y += 5;
            screen.renderProgressCards(graphics, definition, area.contentX() + 8, y, area.maxWidth() - 16, 1, definition.maxPveLevel(), screen.level, "gui.astral_craft.character_settings.pve_level_card", 0xFF8CFF20);
            this.end(graphics, layout, area);
        }

        @Override
        int estimatedHeight(CharacterLayout layout) {
            CharacterDefinition definition = screen.selectedCharacter();
            return 48 + screen.progressCardsHeight(definition, Math.max(40, layout.bodyW - 68), 1, definition.maxPveLevel(), screen.level, "gui.astral_craft.character_settings.pve_level_card", 0xFF8CFF20);
        }

    }

    protected class PotentialSection extends ArchiveSectionPage {

        private ItemStack hoveredMaterialStack = ItemStack.EMPTY;

        @Override
        protected int visibleHeight(CharacterLayout layout) {
            return Math.max(10, layout.bodyH - 50);
        }

        @Override
        protected int bodyScrollBarX(CharacterLayout layout) {
            return this.textRight(layout) - 5;
        }

        @Override
        void render(GuiGraphicsExtractor graphics, CharacterLayout layout) {
            this.hoveredMaterialStack = ItemStack.EMPTY;
            CharacterDefinition definition = screen.selectedCharacter();
            CharacterPotentialDefinition potential = definition.potentialOrDefault();
            CharacterProgressEntry progress = screen.progressEntry(definition.id());
            int headerX = layout.bodyX + 14;
            int headerY = layout.bodyY + 14;
            int contentX = layout.bodyX + 18;
            int contentTop = layout.bodyY + 38;
            int contentBottom = layout.bodyY + layout.bodyH - 12;
            int textRight = this.textRight(layout);
            int textWidth = this.textWidth(layout);
            int y = contentTop - Math.round(screen.bodyScroll) + 4;
            screen.drawHeader(graphics, Component.translatable(screen.archiveTab.titleKey()), headerX, headerY, screen.archiveTab.headerColor(), textRight - headerX - 4);
            graphics.enableScissor(layout.bodyX + 8, contentTop, textRight, contentBottom);
            y = screen.drawWrapped(graphics, Component.translatable(definition.potentialEffectKey()), contentX + 8, y, 0xFFFFE7FF, textWidth - 24);
            y += 8;
            if (potential.hasRequirement()) {
                y = screen.drawHeader(graphics, Component.translatable("gui.astral_craft.character_settings.potential_requirement"), contentX + 8, y, 0xFFBFC8FF, textWidth - 16);
                y = screen.drawWrapped(graphics, Component.translatable("gui.astral_craft.character_settings.potential_requirement.pve_max", progress.level(), definition.maxPveLevel()), contentX + 16, y, progress.level() >= definition.maxPveLevel() ? 0xFF92FF22 : 0xFFFF8888, textWidth - 24);
                y = screen.drawWrapped(graphics, Component.translatable("gui.astral_craft.character_settings.potential_requirement.friendship", progress.friendship(), potential.requiredFriendship()), contentX + 16, y + 2, progress.friendship() >= potential.requiredFriendship() ? 0xFF92FF22 : 0xFFFF8888, textWidth - 24);
                if (potential.requiredExperience() > 0) {
                    y = screen.drawWrapped(graphics, Component.translatable("gui.astral_craft.character_settings.potential_requirement.experience", progress.experience(), potential.requiredExperience()), contentX + 16, y + 2, progress.experience() >= potential.requiredExperience() ? 0xFF92FF22 : 0xFFFF8888, textWidth - 24);
                }
                y += 8;
            }

            this.renderMaterialRequirements(graphics, potential, contentX, y, textWidth - 16, contentTop, contentBottom);
            graphics.disableScissor();
            screen.renderVerticalScrollbar(graphics, this.bodyScrollBarX(layout), this.bodyScrollBarY(layout), this.bodyScrollBarHeight(layout), screen.bodyScroll, maxScroll(layout));
            this.renderPotentialButtonContainer(graphics, layout, definition);
            this.renderHoveredMaterialTooltip(graphics);
        }

        @Override
        boolean mouseClicked(CharacterLayout layout, double mouseX, double mouseY) {
            CharacterDefinition definition = screen.selectedCharacter();
            if (screen.isPotentialActivated(definition) || !screen.canActivatePotential(definition)) return false;
            ButtonArea button = this.buttonArea(layout);
            if (!screen.isInside(mouseX, mouseY, button.x(), button.y(), button.w(), button.h())) return false;
            screen.activatePotential(definition);
            return true;
        }

        @Override
        int estimatedHeight(CharacterLayout layout) {
            CharacterDefinition definition = screen.selectedCharacter();
            CharacterPotentialDefinition potential = definition.potentialOrDefault();
            int maxWidth = Math.max(40, this.textWidth(layout) - 16);
            int height = 0;
            height += screen.wrappedHeight(Component.translatable(definition.potentialDescriptionKey()), maxWidth) + 10;
            height += 18 + screen.wrappedHeight(Component.translatable(definition.potentialEffectKey()), maxWidth - 8) + 10;
            if (potential.hasRequirement()) {
                CharacterProgressEntry progress = screen.progressEntry(definition.id());
                height += 18;
                height += screen.wrappedHeight(Component.translatable("gui.astral_craft.character_settings.potential_requirement.pve_max", progress.level(), definition.maxPveLevel()), maxWidth - 8) + 2;
                height += screen.wrappedHeight(Component.translatable("gui.astral_craft.character_settings.potential_requirement.friendship", progress.friendship(), potential.requiredFriendship()), maxWidth - 8) + 2;
                if (potential.requiredExperience() > 0) {
                    height += screen.wrappedHeight(Component.translatable("gui.astral_craft.character_settings.potential_requirement.experience", progress.experience(), potential.requiredExperience()), maxWidth - 8) + 2;
                }
                height += 8;
            }

            if (!potential.materialRequirements().isEmpty()) {
                int columns = Math.clamp((maxWidth - 8) / 72, 1, 3);
                height += Math.max(1, Math.ceilDiv(potential.materialRequirements().size(), columns));
            }

            return height;
        }

        protected void renderMaterialRequirements(GuiGraphicsExtractor graphics, CharacterPotentialDefinition potential, int x, int y, int maxWidth, int contentTop, int contentBottom) {
            if (potential.materialRequirements().isEmpty()) return;
            y = screen.drawHeader(graphics, Component.translatable("gui.astral_craft.character_settings.potential_requirement.materials"), x + 8, y, 0xFFBFC8FF, maxWidth);
            int iconSize = 18;
            int gap = 10;
            int columns = Math.clamp((maxWidth - 8) / 72, 1, 3);
            int cellW = Math.min(36, (maxWidth - gap * (columns - 1)) / columns);
            int currentX = x + 8;
            int currentY = y;
            for (CharacterPotentialMaterialRequirement requirement : potential.materialRequirements()) {
                ItemStack stack = requirement.displayStack();
                if (!stack.isEmpty()) {
                    graphics.item(stack, currentX, currentY);
                    graphics.itemDecorations(Minecraft.getInstance().font, stack, currentX, currentY);
                    if (currentY + 16 >= contentTop && currentY <= contentBottom && screen.isInside(screen.lastMouseX, screen.lastMouseY, currentX, currentY, 16, 16)) {
                        this.hoveredMaterialStack = stack.copy();
                    }
                }

                int owned = requirement.count(Minecraft.getInstance().player);
                int color = owned >= requirement.count() ? 0xFF92FF22 : 0xFFFF6666;
                Component ownedText = Component.literal(String.valueOf(owned));
                Component slashText = Component.literal("/" + requirement.count());
                int textX = currentX + iconSize + 4;
                int textY = currentY + 5;
                graphics.text(Minecraft.getInstance().font, ownedText, textX, textY, color, true);
                graphics.text(Minecraft.getInstance().font, slashText, textX + Minecraft.getInstance().font.width(ownedText), textY, 0xFFE8E8E8, true);
                currentX += cellW + gap;
            }
        }

        protected void renderPotentialButtonContainer(GuiGraphicsExtractor graphics, CharacterLayout layout, CharacterDefinition definition) {
            int paneX = this.buttonPaneX(layout);
            int paneY = layout.bodyY + 38;
            int paneW = layout.bodyX + layout.bodyW - 8 - paneX;
            int paneH = Math.max(24, layout.bodyH - 50);
            graphics.fill(paneX, paneY, paneX + paneW, paneY + paneH, 0x9915151C);
            graphics.fill(paneX, paneY, paneX + 1, paneY + paneH, 0x40FFFFFF);
            ButtonArea button = this.buttonArea(layout);
            boolean activated = screen.isPotentialActivated(definition);
            boolean canActivate = screen.canActivatePotential(definition);
            boolean hovered = screen.isInside(screen.lastMouseX, screen.lastMouseY, button.x(), button.y(), button.w(), button.h());
            MutableComponent text = Component.translatable(activated ? "gui.astral_craft.character_settings.potential_active" : "gui.astral_craft.character_settings.potential_activate");
            screen.renderFancyButton(graphics, text, button.x(), button.y(), button.w(), button.h(), false, hovered && canActivate && !activated, activated || !canActivate ? screen.disabledButtonStyle() : screen.pinkButtonStyle());
        }

        protected void renderHoveredMaterialTooltip(GuiGraphicsExtractor graphics) {
            if (this.hoveredMaterialStack.isEmpty()) return;
            Minecraft minecraft = Minecraft.getInstance();
            List<Component> components = new ArrayList<>();
            components.add(this.hoveredMaterialStack.getHoverName());
            Identifier itemId = BuiltInRegistries.ITEM.getKey(this.hoveredMaterialStack.getItem());
            components.add(Component.literal(itemId.toString()).withStyle(ChatFormatting.DARK_GRAY));
            List<FormattedCharSequence> lines = new ArrayList<>();
            int maxLineWidth = 174;
            for (Component component : components) {
                lines.addAll(minecraft.font.split(component, maxLineWidth));
            }

            int tooltipW = 0;
            for (FormattedCharSequence line : lines) {
                tooltipW = Math.max(tooltipW, minecraft.font.width(line));
            }

            tooltipW = Math.clamp(tooltipW + 12, 36, maxLineWidth + 12);
            int lineH = 10;
            int tooltipH = lines.size() * lineH + 10;
            int screenW = minecraft.getWindow().getGuiScaledWidth();
            int screenH = minecraft.getWindow().getGuiScaledHeight();
            int tooltipX = Math.min(screen.lastMouseX + 12, screenW - tooltipW - 6);
            int tooltipY = Math.min(screen.lastMouseY + 12, screenH - tooltipH - 6);
            tooltipX = Math.max(6, tooltipX);
            tooltipY = Math.max(6, tooltipY);
            graphics.fill(tooltipX, tooltipY, tooltipX + tooltipW, tooltipY + tooltipH, 0xF0101018);
            graphics.fill(tooltipX, tooltipY, tooltipX + tooltipW, tooltipY + 1, 0xB0FFFFFF);
            int y = tooltipY + 6;
            for (FormattedCharSequence line : lines) {
                graphics.text(minecraft.font, line, tooltipX + 6, y, 0xFFFFFFFF, false);
                y += lineH;
            }
        }

        protected int buttonPaneWidth(CharacterLayout layout) {
            return Math.clamp(layout.bodyW / 4, 90, 118);
        }

        protected int buttonPaneX(CharacterLayout layout) {
            return this.textRight(layout) + 8;
        }

        protected int textRight(CharacterLayout layout) {
            return layout.bodyX + layout.bodyW - this.buttonPaneWidth(layout) - 14;
        }

        protected int textWidth(CharacterLayout layout) {
            return Math.max(52, this.textRight(layout) - (layout.bodyX + 18) - 8);
        }

        protected ButtonArea buttonArea(CharacterLayout layout) {
            int paneX = this.buttonPaneX(layout);
            int paneRight = layout.bodyX + layout.bodyW - 8;
            int buttonW = Math.clamp((paneRight - paneX) - 18, 58, 82);
            int buttonH = 22;
            int buttonX = paneX + Math.max(0, (paneRight - paneX - buttonW) / 2);
            int buttonY = layout.bodyY + layout.bodyH - buttonH - 20;
            return new ButtonArea(buttonX, buttonY, buttonW, buttonH);
        }

        protected record ButtonArea(int x, int y, int w, int h) {}

    }

    protected class ProfileSection extends ArchiveSectionPage {

        @Override
        void render(GuiGraphicsExtractor graphics, CharacterLayout layout) {
            SectionArea area = this.begin(graphics, layout);
            int y = area.y();
            CharacterDefinition definition = screen.selectedCharacter();
            for (CharacterProfileSection section : definition.profileSections()) {
                y = screen.drawWrapped(graphics, Component.translatable(section.bodyKey()), area.contentX() + 8, y + 2, 0xFFE7E7E7, area.maxWidth() - 16);
                y += 8;
            }

            if (definition.profileSections().isEmpty()) {
                screen.drawWrapped(graphics, Component.translatable("gui.astral_craft.character_settings.empty_profile"), area.contentX(), y, 0xFFD0D0D0, area.maxWidth() - 8);
            }

            this.end(graphics, layout, area);
        }

        @Override
        int estimatedHeight(CharacterLayout layout) {
            CharacterDefinition definition = screen.selectedCharacter();
            int maxWidth = Math.max(40, layout.bodyW - 52);
            int height = 10;
            if (definition.profileSections().isEmpty()) {
                return screen.wrappedHeight(Component.translatable("gui.astral_craft.character_settings.empty_profile"), maxWidth) + 12;
            }

            for (CharacterProfileSection section : definition.profileSections()) {
                if (screen.shouldRenderProfileSectionHeader(section)) {
                    height += 16;
                }

                height += screen.wrappedHeight(Component.translatable(section.bodyKey()), maxWidth - 8) + 12;
            }

            return height;
        }

    }

}