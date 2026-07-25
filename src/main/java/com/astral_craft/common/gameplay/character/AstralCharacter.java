package com.astral_craft.common.gameplay.character;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.gameplay.buff.BoardBuff;
import com.astral_craft.common.gameplay.character.skill.AstralCharacterSkillEffects;
import com.astral_craft.common.gameplay.character.skill.AstralCharacterSkillService;
import com.astral_craft.common.gameplay.character.skill.CharacterSkillContext;
import com.astral_craft.common.gameplay.character.skill.CharacterSkillDefinition;
import com.astral_craft.common.gameplay.character.skill.CharacterSkillType;
import com.astral_craft.common.registry.AstralBoardBuffs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Registered character type. Metadata is fixed during mod initialization while skins remain
 * resource-pack driven through {@link com.astral_craft.common.gameplay.character.skin.CharacterSkinManager}.
 */
public class AstralCharacter {

    public static final Identifier DEFAULT_CUTIN_ANIMATION = AstralCraft.prefix("skill");

    protected final Properties properties;

    public AstralCharacter(Properties properties) {
        this.properties = properties.copy();
    }

    public CharacterDefinition definition(Identifier id) {
        String prefix = "character." + id.getNamespace() + "." + id.getPath();
        List<CharacterSkillDefinition> skills = this.properties.skills.isEmpty() ? this.defaultSkills(id) : List.copyOf(this.properties.skills);
        return new CharacterDefinition(id, this.properties.nameKey == null ? prefix + ".name" : this.properties.nameKey,
                this.properties.titleKey == null ? prefix + ".title" : this.properties.titleKey,
                this.properties.modelKey, this.properties.previewTexture == null
                ? Identifier.fromNamespaceAndPath(id.getNamespace(), "entity/character/skin_" + id.getPath() + "_default")
                : this.properties.previewTexture,
                this.properties.entityTypeKey, this.properties.rendererKey, this.properties.animationSetKey,
                this.properties.previewAction, this.properties.maxPveLevel, this.properties.maxFriendshipLevel,
                this.properties.baseStats, skills, this.properties.profileSections.isEmpty()
                ? List.of(new CharacterProfileSection("", prefix + ".profile.basic.body"))
                : List.copyOf(this.properties.profileSections),
                List.of(), this.properties.potential.enabled(), this.properties.potential,
                this.properties.implicitDefaultSkin, this.properties.implicitBondSkin,
                this.properties.unlockedByDefault, this.properties.unlockHintKey == null
                ? prefix + ".unlock_hint" : this.properties.unlockHintKey, this.properties.sortOrder);
    }

    protected List<CharacterSkillDefinition> defaultSkills(Identifier id) {
        Identifier handler = this.properties.skillHandler == null ? id : this.properties.skillHandler;
        if (this.properties.sameSkillCooldown) {
            return List.of(new CharacterSkillDefinition(CharacterSkillType.ACTIVE, this.properties.cooldown, 0,
                            handler, this.properties.fallbackAnimation, false, false, -1, -1, null),
                    new CharacterSkillDefinition(CharacterSkillType.PASSIVE, 0, 0, handler,
                            this.properties.fallbackAnimation, false, false, -1, -1, null));
        }

        return List.of(new CharacterSkillDefinition(CharacterSkillType.ACTIVE, 0, 0, handler,
                        this.properties.fallbackAnimation, true, true, this.properties.pvpCooldown,
                        this.properties.pveCooldown, null),
                new CharacterSkillDefinition(CharacterSkillType.PASSIVE, 0, 0, handler,
                        this.properties.fallbackAnimation, true, true, -1, -1, null));
    }

    public boolean hasActiveSkill() {
        return false;
    }

    public boolean useActiveSkill(CharacterSkillContext context) {
        return false;
    }

    public void serverTick(CharacterSkillContext context) {}

    public void onBoardEntityTick(AstralCharacterEntity entity) {}

    public void onPlayerTick(ServerPlayer player) {}

    public void onBoardTurnStart(AstralCharacterEntity entity) {}

    public void onBoardTurnEnd(AstralCharacterEntity entity) {}

    public Identifier fallbackAnimation() {
        return this.properties.fallbackAnimation;
    }

    public static boolean grantConfiguredStatusEffect(CharacterSkillContext context) {
        if (context == null || context.skill() == null) return false;
        return context.skill().statusEffectId().filter(statusId -> {
            if (context.actor() instanceof AstralCharacterEntity character && character.isBoardPawn()) {
                BoardBuff buff = AstralBoardBuffs.REGISTRY.getValue(statusId);
                int duration = AstralCharacterSkillService.durationRounds(context.skill());
                return buff == null ? BoardSessionManager.addRoundStatusEffect(character, statusId, duration)
                        : BoardSessionManager.addBoardBuff(character, buff, duration, 0);
            }

            return AstralCharacterSkillEffects.add(context.actor(), statusId,
                    AstralCharacterSkillService.durationTicks(context.skill()), 0);
        }).isPresent();
    }

    public static class Properties {

        protected String nameKey;
        protected String titleKey;
        protected Identifier modelKey = AstralCraft.prefix("humanoid");
        protected Identifier previewTexture;
        protected Identifier entityTypeKey = AstralCraft.prefix("astral_character");
        protected Identifier rendererKey = AstralCraft.prefix("player");
        protected Identifier animationSetKey = AstralCraft.prefix("humanoid");
        protected String previewAction = "idle";
        protected int maxPveLevel = 6;
        protected int maxFriendshipLevel = 6;
        protected CharacterStatsDefinition baseStats = CharacterStatsDefinition.defaultStats();
        protected final List<CharacterSkillDefinition> skills = new ArrayList<>();
        protected final List<CharacterProfileSection> profileSections = new ArrayList<>();
        protected CharacterPotentialDefinition potential = CharacterPotentialDefinition.NONE;
        protected boolean implicitDefaultSkin = true;
        protected boolean implicitBondSkin = true;
        protected boolean unlockedByDefault;
        protected String unlockHintKey;
        protected int sortOrder = 1000;
        protected boolean sameSkillCooldown;
        protected int cooldown;
        protected int pvpCooldown = -1;
        protected int pveCooldown = -1;
        protected Identifier skillHandler;
        protected Identifier fallbackAnimation = DEFAULT_CUTIN_ANIMATION;

        public Properties nameKey(String value) {
            this.nameKey = value;
            return this;
        }

        public Properties titleKey(String value) {
            this.titleKey = value;
            return this;
        }

        public Properties model(Identifier value) {
            this.modelKey = value;
            return this;
        }

        public Properties previewTexture(Identifier value) {
            this.previewTexture = value;
            return this;
        }

        public Properties entityType(Identifier value) {
            this.entityTypeKey = value;
            return this;
        }

        public Properties renderer(Identifier value) {
            this.rendererKey = value;
            return this;
        }

        public Properties animationSet(Identifier value) {
            this.animationSetKey = value;
            return this;
        }

        public Properties previewAction(String value) {
            this.previewAction = value;
            return this;
        }

        public Properties maxPveLevel(int value) {
            this.maxPveLevel = Math.max(1, value);
            return this;
        }

        public Properties maxFriendshipLevel(int value) {
            this.maxFriendshipLevel = Math.max(1, value);
            return this;
        }

        public Properties baseStats(int attack, int defense, int health, int initialStarCoins) {
            this.baseStats = new CharacterStatsDefinition(attack, defense, health, initialStarCoins);
            return this;
        }

        public Properties skill(CharacterSkillDefinition value) {
            if (value != null) this.skills.add(value);
            return this;
        }

        public Properties profile(CharacterProfileSection value) {
            if (value != null) this.profileSections.add(value);
            return this;
        }

        public Properties potential(CharacterPotentialDefinition value) {
            this.potential = value == null ? CharacterPotentialDefinition.NONE : value;
            return this;
        }

        public Properties implicitDefaultSkin(boolean value) {
            this.implicitDefaultSkin = value;
            return this;
        }

        public Properties implicitBondSkin(boolean value) {
            this.implicitBondSkin = value;
            return this;
        }

        public Properties unlockedByDefault(boolean value) {
            this.unlockedByDefault = value;
            return this;
        }

        public Properties unlockHintKey(String value) {
            this.unlockHintKey = value;
            return this;
        }

        public Properties sortOrder(int value) {
            this.sortOrder = value;
            return this;
        }

        public Properties cooldown(int value) {
            this.sameSkillCooldown = true;
            this.cooldown = Math.max(0, value);
            return this;
        }

        public Properties cooldown(int pvp, int pve) {
            this.sameSkillCooldown = false;
            this.pvpCooldown = pvp;
            this.pveCooldown = pve;
            return this;
        }

        public Properties skillHandler(Identifier value) {
            this.skillHandler = value;
            return this;
        }

        public Properties fallbackAnimation(Identifier value) {
            this.fallbackAnimation = value;
            return this;
        }

        protected Properties copy() {
            Properties result = new Properties();
            result.nameKey = this.nameKey;
            result.titleKey = this.titleKey;
            result.modelKey = this.modelKey;
            result.previewTexture = this.previewTexture;
            result.entityTypeKey = this.entityTypeKey;
            result.rendererKey = this.rendererKey;
            result.animationSetKey = this.animationSetKey;
            result.previewAction = this.previewAction;
            result.maxPveLevel = this.maxPveLevel;
            result.maxFriendshipLevel = this.maxFriendshipLevel;
            result.baseStats = this.baseStats;
            result.skills.addAll(this.skills);
            result.profileSections.addAll(this.profileSections);
            result.potential = this.potential;
            result.implicitDefaultSkin = this.implicitDefaultSkin;
            result.implicitBondSkin = this.implicitBondSkin;
            result.unlockedByDefault = this.unlockedByDefault;
            result.unlockHintKey = this.unlockHintKey;
            result.sortOrder = this.sortOrder;
            result.sameSkillCooldown = this.sameSkillCooldown;
            result.cooldown = this.cooldown;
            result.pvpCooldown = this.pvpCooldown;
            result.pveCooldown = this.pveCooldown;
            result.skillHandler = this.skillHandler;
            result.fallbackAnimation = this.fallbackAnimation;
            return result;
        }

    }

}