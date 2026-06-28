package com.astral_craft.client.gui.character;

import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.CharacterProfileSection;
import com.astral_craft.common.gameplay.character.CharacterPotentialDefinition;
import com.astral_craft.common.gameplay.character.CharacterProgressEntry;
import com.astral_craft.common.gameplay.character.CharacterSkillDefinition;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.ARGB;

import java.util.EnumMap;
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
        int visible = Math.max(10, layout.bodyH - 50);
        int content = this.currentSection().estimatedHeight(layout);
        return content > visible + 14 ? content - visible : 0.0F;
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
            int contentBottom = layout.bodyY + layout.bodyH - 12;
            int y = contentTop - Math.round(screen.bodyScroll);
            screen.drawHeader(graphics, Component.translatable(screen.archiveTab.titleKey()), headerX, headerY, screen.archiveTab.headerColor(), maxWidth);
            graphics.enableScissor(layout.bodyX + 8, contentTop, layout.bodyX + layout.bodyW - 8, contentBottom);
            return new SectionArea(contentX, y, maxWidth, contentTop, contentBottom);
        }

        protected void end(GuiGraphicsExtractor graphics, CharacterLayout layout, SectionArea area) {
            graphics.disableScissor();
            screen.renderVerticalScrollbar(graphics, layout.bodyX + layout.bodyW - 5, area.contentTop(), area.contentBottom() - area.contentTop(), screen.bodyScroll, maxScroll(layout));
        }

        boolean mouseClicked(CharacterLayout layout, double mouseX, double mouseY) {
            return false;
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
                String prefix = "character.astral_craft.skill." + skill.id();
                int nameColor = skill.id().equals("active") ? ARGB.color(255, 191, 0) : ARGB.color(152, 252, 253);
                y = screen.drawHeader(graphics, Component.translatable(prefix, Component.translatable(skill.nameKey(screen.skillMode))), area.contentX(), y, nameColor, area.maxWidth() - 8);
                y = screen.drawWrapped(graphics, Component.translatable(skill.descriptionKey(screen.skillMode)), area.contentX() + 8, y + 2, 0xFFE7E7E7, area.maxWidth() - 16);
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
                height += screen.wrappedHeight(Component.translatable(skill.descriptionKey(screen.skillMode)), maxWidth - 8) + 10;
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

        @Override
        void render(GuiGraphicsExtractor graphics, CharacterLayout layout) {
            SectionArea area = this.begin(graphics, layout);
            CharacterDefinition definition = screen.selectedCharacter();
            CharacterPotentialDefinition potential = definition.potentialOrDefault();
            CharacterProgressEntry progress = screen.progressEntry(definition.id());
            int y = area.y() + 4;
            y = screen.drawWrapped(graphics, Component.translatable(potential.descriptionKey()), area.contentX() + 8, y, 0xFFE7E7E7, area.maxWidth() - 16);
            y += 8;
            y = screen.drawHeader(graphics, Component.translatable("gui.astral_craft.character_settings.potential_effect"), area.contentX() + 8, y, 0xFFDFA0FF, area.maxWidth() - 16);
            y = screen.drawWrapped(graphics, Component.translatable(potential.effectKey()), area.contentX() + 16, y, 0xFFFFE7FF, area.maxWidth() - 24);
            y += 8;
            if (potential.hasRequirement()) {
                y = screen.drawHeader(graphics, Component.translatable("gui.astral_craft.character_settings.potential_requirement"), area.contentX() + 8, y, 0xFFBFC8FF, area.maxWidth() - 16);
                y = screen.drawLine(graphics, Component.translatable("gui.astral_craft.character_settings.potential_requirement.level", progress.level(), potential.requiredLevel()), area.contentX() + 16, y, progress.level() >= potential.requiredLevel() ? 0xFF92FF22 : 0xFFFF8888, area.maxWidth() - 24);
                y = screen.drawLine(graphics, Component.translatable("gui.astral_craft.character_settings.potential_requirement.friendship", progress.friendship(), potential.requiredFriendship()), area.contentX() + 16, y, progress.friendship() >= potential.requiredFriendship() ? 0xFF92FF22 : 0xFFFF8888, area.maxWidth() - 24);
                if (potential.requiredExperience() > 0) {
                    y = screen.drawLine(graphics, Component.translatable("gui.astral_craft.character_settings.potential_requirement.experience", progress.experience(), potential.requiredExperience()), area.contentX() + 16, y, progress.experience() >= potential.requiredExperience() ? 0xFF92FF22 : 0xFFFF8888, area.maxWidth() - 24);
                }
                y += 8;
            }

            this.renderPotentialButton(graphics, layout, area, definition);
            this.end(graphics, layout, area);
        }

        @Override
        boolean mouseClicked(CharacterLayout layout, double mouseX, double mouseY) {
            CharacterDefinition definition = screen.selectedCharacter();
            if (screen.isPotentialActivated(definition) || !screen.canActivatePotential(definition)) return false;
            int buttonW = Math.clamp(layout.bodyW / 3, 90, 132);
            int buttonH = 22;
            int buttonX = layout.bodyX + layout.bodyW - buttonW - 20;
            int buttonY = layout.bodyY + layout.bodyH - buttonH - 20;
            if (!screen.isInside(mouseX, mouseY, buttonX, buttonY, buttonW, buttonH)) return false;
            screen.activatePotential(definition);
            return true;
        }

        @Override
        int estimatedHeight(CharacterLayout layout) {
            CharacterDefinition definition = screen.selectedCharacter();
            CharacterPotentialDefinition potential = definition.potentialOrDefault();
            int maxWidth = Math.max(40, layout.bodyW - 60);
            int height = 38;
            height += screen.wrappedHeight(Component.translatable(potential.descriptionKey()), maxWidth) + 10;
            height += 18 + screen.wrappedHeight(Component.translatable(potential.effectKey()), maxWidth - 8) + 10;
            if (potential.hasRequirement()) {
                height += 58;
                if (potential.requiredExperience() > 0) height += 12;
            }
            return height;
        }

        protected void renderPotentialButton(GuiGraphicsExtractor graphics, CharacterLayout layout, SectionArea area, CharacterDefinition definition) {
            int buttonW = Math.clamp(layout.bodyW / 3, 90, 132);
            int buttonH = 22;
            int buttonX = layout.bodyX + layout.bodyW - buttonW - 20;
            int buttonY = layout.bodyY + layout.bodyH - buttonH - 20;
            boolean activated = screen.isPotentialActivated(definition);
            boolean canActivate = screen.canActivatePotential(definition);
            boolean hovered = screen.isInside(screen.lastMouseX, screen.lastMouseY, buttonX, buttonY, buttonW, buttonH);
            MutableComponent text = Component.translatable(activated ? "gui.astral_craft.character_settings.potential_active" : "gui.astral_craft.character_settings.potential_activate");
            screen.renderFancyButton(graphics, text, buttonX, buttonY, buttonW, buttonH, false, hovered && canActivate && !activated, activated || !canActivate ? screen.disabledButtonStyle() : screen.pinkButtonStyle());
        }

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