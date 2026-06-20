package com.astral_craft.client.gui.character;

import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.CharacterProfileSection;
import com.astral_craft.common.gameplay.character.CharacterSkillDefinition;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

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
        return this.screen.handleArchiveTabClick(layout, mouseX, mouseY);
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
            int y = contentTop - Math.round(ArchiveDetailPage.this.screen.bodyScroll);
            ArchiveDetailPage.this.screen.drawHeader(graphics, Component.translatable(ArchiveDetailPage.this.screen.archiveTab.titleKey()), headerX, headerY, ArchiveDetailPage.this.screen.archiveTab.headerColor(), maxWidth);
            graphics.enableScissor(layout.bodyX + 8, contentTop, layout.bodyX + layout.bodyW - 8, contentBottom);
            return new SectionArea(contentX, y, maxWidth, contentTop, contentBottom);
        }

        protected void end(GuiGraphicsExtractor graphics, CharacterLayout layout, SectionArea area) {
            graphics.disableScissor();
            ArchiveDetailPage.this.screen.renderVerticalScrollbar(graphics, layout.bodyX + layout.bodyW - 5, area.contentTop(), area.contentBottom() - area.contentTop(), ArchiveDetailPage.this.screen.bodyScroll, ArchiveDetailPage.this.maxScroll(layout));
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
            CharacterDefinition definition = ArchiveDetailPage.this.screen.selectedCharacter();
            for (CharacterSkillDefinition skill : definition.skills()) {
                y = ArchiveDetailPage.this.screen.drawHeader(graphics, Component.translatable(skill.nameKey()), area.contentX(), y, 0xFFFFF2A0, area.maxWidth() - 8);
                y = ArchiveDetailPage.this.screen.drawWrapped(graphics, Component.translatable(skill.descriptionKey()), area.contentX() + 8, y + 2, 0xFFE7E7E7, area.maxWidth() - 16);
                if (skill.cooldown() > 0) {
                    y = ArchiveDetailPage.this.screen.drawLine(graphics, Component.translatable("gui.astral_craft.character_settings.cooldown", skill.cooldown()), area.contentX() + 8, y + 2, 0xFFB0B0B0, area.maxWidth() - 16);
                }
                y += 8;
            }

            if (definition.skills().isEmpty()) {
                ArchiveDetailPage.this.screen.drawWrapped(graphics, Component.translatable("gui.astral_craft.character_settings.empty_skills"), area.contentX(), y, 0xFFD0D0D0, area.maxWidth() - 8);
            }

            this.end(graphics, layout, area);
        }

        @Override
        int estimatedHeight(CharacterLayout layout) {
            CharacterDefinition definition = ArchiveDetailPage.this.screen.selectedCharacter();
            int maxWidth = Math.max(40, layout.bodyW - 52);
            int height = 10;
            if (definition.skills().isEmpty()) {
                return ArchiveDetailPage.this.screen.wrappedHeight(Component.translatable("gui.astral_craft.character_settings.empty_skills"), maxWidth) + 12;
            }

            for (CharacterSkillDefinition skill : definition.skills()) {
                height += 16;
                height += ArchiveDetailPage.this.screen.wrappedHeight(Component.translatable(skill.descriptionKey()), maxWidth - 8) + 10;
                if (skill.cooldown() > 0) {
                    height += 14;
                }
            }

            return height;
        }

    }

    protected class LevelSection extends ArchiveSectionPage {

        @Override
        void render(GuiGraphicsExtractor graphics, CharacterLayout layout) {
            SectionArea area = this.begin(graphics, layout);
            int y = area.y();
            CharacterDefinition definition = ArchiveDetailPage.this.screen.selectedCharacter();
            y = ArchiveDetailPage.this.screen.drawLine(graphics, Component.translatable("gui.astral_craft.character_settings.level_value", ArchiveDetailPage.this.screen.level, definition.maxPveLevel()), area.contentX() + 8, y + 2, 0xFFFFFFFF, area.maxWidth() - 16);
            y = ArchiveDetailPage.this.screen.drawLine(graphics, Component.translatable("gui.astral_craft.character_settings.experience_value", ArchiveDetailPage.this.screen.experience), area.contentX() + 8, y + 2, 0xFFBFE6FF, area.maxWidth() - 16);
            y += 5;
            ArchiveDetailPage.this.screen.renderProgressCards(graphics, definition, area.contentX() + 8, y, area.maxWidth() - 16, 1, definition.maxPveLevel(), ArchiveDetailPage.this.screen.level, "gui.astral_craft.character_settings.pve_level_card", 0xFF8CFF20);
            this.end(graphics, layout, area);
        }

        @Override
        int estimatedHeight(CharacterLayout layout) {
            CharacterDefinition definition = ArchiveDetailPage.this.screen.selectedCharacter();
            return 48 + ArchiveDetailPage.this.screen.progressCardsHeight(definition, Math.max(40, layout.bodyW - 68), 1, definition.maxPveLevel(), ArchiveDetailPage.this.screen.level, "gui.astral_craft.character_settings.pve_level_card", 0xFF8CFF20);
        }

    }

    protected class PotentialSection extends ArchiveSectionPage {

        @Override
        void render(GuiGraphicsExtractor graphics, CharacterLayout layout) {
            SectionArea area = this.begin(graphics, layout);
            ArchiveDetailPage.this.screen.drawWrapped(graphics, Component.translatable("gui.astral_craft.character_settings.potential_placeholder"), area.contentX() + 8, area.y() + 4, 0xFFE7E7E7, area.maxWidth() - 16);
            this.end(graphics, layout, area);
        }

        @Override
        int estimatedHeight(CharacterLayout layout) {
            return ArchiveDetailPage.this.screen.wrappedHeight(Component.translatable("gui.astral_craft.character_settings.potential_placeholder"), Math.max(40, layout.bodyW - 60)) + 30;
        }

    }

    protected class ProfileSection extends ArchiveSectionPage {

        @Override
        void render(GuiGraphicsExtractor graphics, CharacterLayout layout) {
            SectionArea area = this.begin(graphics, layout);
            int y = area.y();
            CharacterDefinition definition = ArchiveDetailPage.this.screen.selectedCharacter();
            for (CharacterProfileSection section : definition.profileSections()) {
                y = ArchiveDetailPage.this.screen.drawWrapped(graphics, Component.translatable(section.bodyKey()), area.contentX() + 8, y + 2, 0xFFE7E7E7, area.maxWidth() - 16);
                y += 8;
            }

            if (definition.profileSections().isEmpty()) {
                ArchiveDetailPage.this.screen.drawWrapped(graphics, Component.translatable("gui.astral_craft.character_settings.empty_profile"), area.contentX(), y, 0xFFD0D0D0, area.maxWidth() - 8);
            }

            this.end(graphics, layout, area);
        }

        @Override
        int estimatedHeight(CharacterLayout layout) {
            CharacterDefinition definition = ArchiveDetailPage.this.screen.selectedCharacter();
            int maxWidth = Math.max(40, layout.bodyW - 52);
            int height = 10;
            if (definition.profileSections().isEmpty()) {
                return ArchiveDetailPage.this.screen.wrappedHeight(Component.translatable("gui.astral_craft.character_settings.empty_profile"), maxWidth) + 12;
            }

            for (CharacterProfileSection section : definition.profileSections()) {
                if (ArchiveDetailPage.this.screen.shouldRenderProfileSectionHeader(section)) {
                    height += 16;
                }
                height += ArchiveDetailPage.this.screen.wrappedHeight(Component.translatable(section.bodyKey()), maxWidth - 8) + 12;
            }

            return height;
        }

    }

}