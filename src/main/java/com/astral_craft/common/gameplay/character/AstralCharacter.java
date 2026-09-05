package com.astral_craft.common.gameplay.character;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.board.BoardSession;
import com.astral_craft.common.gameplay.buff.BoardBuff;
import com.astral_craft.common.gameplay.character.skill.*;
import com.astral_craft.common.gameplay.chip.ChipPool;
import com.astral_craft.common.registry.AstralBoardBuffs;
import com.astral_craft.common.stats.AstralPlayerStats;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Registered character type. Static metadata and runtime mechanics live together; skins remain resource-pack driven.
 */
public class AstralCharacter {

    public static final Identifier DEFAULT_CUTIN_ANIMATION = AstralCraft.prefix("skill");

    protected final Properties properties;
    protected final CharacterProgressionDefinition progression;

    public AstralCharacter(Properties properties, CharacterProgressionDefinition progression) {
        this.properties = properties.copy();
        this.progression = progression == null ? CharacterProgressionDefinition.of(1000) : progression.copy();
    }

    public CharacterDefinition definition(Identifier id) {
        String prefix = "character." + id.getNamespace() + "." + id.getPath();
        List<CharacterSkillView> skills = new ArrayList<>();
        skills.add(this.activeSkill().view());
        for (PassiveCharacterSkillDefinition passive : this.passiveSkills()) skills.add(passive.view());
        List<CharacterProfileSection> profiles = this.properties.profileSections.isEmpty()
                ? List.of(new CharacterProfileSection("", prefix + ".profile.basic.body"))
                : List.copyOf(this.properties.profileSections);
        String unlockHint = this.progression.unlockHintKey() == null || this.progression.unlockHintKey().isBlank()
                ? prefix + ".unlock_hint" : this.progression.unlockHintKey();
        return new CharacterDefinition(id, this.properties.modelKey, this.properties.entityTypeKey,
                this.properties.rendererKey, this.properties.animationSetKey, this.properties.previewAction,
                this.properties.baseStats, skills, profiles, List.of(), this.progression.potential(),
                this.progression.implicitBondSkin(), this.progression.unlockedByDefaultValue(), unlockHint,
                this.progression.sortOrder());
    }

    public ActiveCharacterSkillDefinition activeSkill() {
        return this.properties.activeSkill;
    }

    public List<PassiveCharacterSkillDefinition> passiveSkills() {
        return List.copyOf(this.properties.passiveSkills);
    }

    public BoardSkillDefinition boardSkill() {
        return this.properties.boardSkill;
    }

    public ChipPool.Weights chipWeights() {
        return this.properties.chipWeights;
    }

    public int chipWeight(ChipPool pool) {
        return this.properties.chipWeights.weight(pool);
    }

    public boolean botSelectable() {
        return this.properties.botSelectable;
    }

    public AstralPlayerStats initializeBoardStats(AstralPlayerStats stats) {
        AstralPlayerStats result = stats;
        for (IntrinsicBuff value : this.properties.intrinsicBuffs) {
            BoardBuff buff = value.buff().get();
            Identifier id = buff == null ? null : AstralBoardBuffs.REGISTRY.getKey(buff);
            if (id != null) {
                result = result.addBuff(AstralBoardBuffs.instance(id, buff).level(value.level()).intrinsic().build());
            }
        }

        return result;
    }

    public boolean useActiveSkill(CharacterSkillContext context) {
        return grantConfiguredStatusEffect(context) || context != null;
    }

    public boolean useBoardSkill(BoardSkillContext context) {
        return context != null;
    }

    public void onSkillUsed(CharacterSkillContext context) {}

    public void onBoardSkillUsed(BoardSkillContext context) {}

    public void serverTick(CharacterSkillContext context) {}

    public void onBoardEntityTick(AstralCharacterEntity entity) {}

    public void onPlayerTick(ServerPlayer player) {}

    public void onBoardTurnStart(AstralCharacterEntity entity) {}

    public void onBoardTurnEnd(AstralCharacterEntity entity) {}

    public void onBoardEffectCardUsed(ServerLevel level, BoardSession session, BoardParticipant participant, ItemStack card) {}

    public void onBoardMoveStarted(ServerLevel level, BoardSession session, BoardParticipant participant) {}

    public void onBoardMoveFinished(ServerLevel level, BoardSession session, BoardParticipant participant) {}

    public void onBoardBattleStarted(ServerLevel level, BoardSession session, BoardParticipant attacker, BoardParticipant defender) {}

    public void onBoardBattleFinished(ServerLevel level, BoardSession session, BoardParticipant attacker, BoardParticipant defender) {}

    public Identifier fallbackAnimation() {
        return this.properties.fallbackAnimation;
    }

    public static boolean grantConfiguredStatusEffect(CharacterSkillContext context) {
        if (context == null || context.skill() == null) return false;
        return context.skill().statusEffectId().filter(statusId -> AstralCharacterSkillEffects.add(context.actor(), statusId,
                AstralCharacterSkillService.durationTicks(context.skill()), 0)).isPresent();
    }

    public static class Properties {

        protected Identifier modelKey = AstralCraft.prefix("humanoid");
        protected Identifier entityTypeKey = AstralCraft.prefix("astral_character");
        protected Identifier rendererKey = AstralCraft.prefix("player");
        protected Identifier animationSetKey = AstralCraft.prefix("humanoid");
        protected String previewAction = "idle";
        protected CharacterStatsDefinition baseStats = CharacterStatsDefinition.defaultStats();
        protected ActiveCharacterSkillDefinition activeSkill = ActiveCharacterSkillDefinition.cooldown(3);
        protected BoardSkillDefinition boardSkill = BoardSkillDefinition.cooldown(3);
        protected ChipPool.Weights chipWeights = ChipPool.Weights.DEFAULT;
        protected boolean botSelectable = true;
        protected final List<PassiveCharacterSkillDefinition> passiveSkills = new ArrayList<>(List.of(PassiveCharacterSkillDefinition.of("passive")));
        protected final List<CharacterProfileSection> profileSections = new ArrayList<>();
        protected final List<IntrinsicBuff> intrinsicBuffs = new ArrayList<>();
        protected Identifier fallbackAnimation = DEFAULT_CUTIN_ANIMATION;

        public Properties model(Identifier value) {
            this.modelKey = value;
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

        public Properties baseStats(int attack, int defense, int health, int initialStarCoins) {
            this.baseStats = new CharacterStatsDefinition(attack, defense, health, initialStarCoins);
            return this;
        }

        public Properties activeSkill(ActiveCharacterSkillDefinition value) {
            if (value != null) this.activeSkill = value;
            return this;
        }

        public Properties boardSkill(BoardSkillDefinition value) {
            if (value != null) this.boardSkill = value;
            return this;
        }

        public Properties cooldown(int value) {
            this.activeSkill = ActiveCharacterSkillDefinition.cooldown(value);
            this.boardSkill = BoardSkillDefinition.cooldown(value);
            return this;
        }

        public Properties cooldown(int pvp, int pve) {
            this.activeSkill = ActiveCharacterSkillDefinition.cooldown(pvp, pve);
            this.boardSkill = BoardSkillDefinition.cooldown(pvp, pve);
            return this;
        }

        public Properties chipWeights(int support, int sustain, int attack, int cards) {
            this.chipWeights = new ChipPool.Weights(support, sustain, attack, cards);
            return this;
        }

        public Properties botSelectable(boolean value) {
            this.botSelectable = value;
            return this;
        }

        public Properties passiveSkill(PassiveCharacterSkillDefinition value) {
            if (value != null) {
                if (this.passiveSkills.size() == 1 && "passive".equals(this.passiveSkills.getFirst().id()))
                    this.passiveSkills.clear();
                this.passiveSkills.add(value);
            }
            return this;
        }

        public Properties profile(CharacterProfileSection value) {
            if (value != null) this.profileSections.add(value);
            return this;
        }

        public Properties intrinsicBuff(Supplier<? extends BoardBuff> buff, int level) {
            if (buff != null && level > 0) this.intrinsicBuffs.add(new IntrinsicBuff(buff, level));
            return this;
        }

        public Properties fallbackAnimation(Identifier value) {
            if (value != null) this.fallbackAnimation = value;
            return this;
        }

        protected Properties copy() {
            Properties result = new Properties();
            result.modelKey = this.modelKey;
            result.entityTypeKey = this.entityTypeKey;
            result.rendererKey = this.rendererKey;
            result.animationSetKey = this.animationSetKey;
            result.previewAction = this.previewAction;
            result.baseStats = this.baseStats;
            result.activeSkill = this.activeSkill;
            result.boardSkill = this.boardSkill;
            result.chipWeights = this.chipWeights;
            result.botSelectable = this.botSelectable;
            result.passiveSkills.clear();
            result.passiveSkills.addAll(this.passiveSkills);
            result.profileSections.addAll(this.profileSections);
            result.intrinsicBuffs.addAll(this.intrinsicBuffs);
            result.fallbackAnimation = this.fallbackAnimation;
            return result;
        }
    }

    protected record IntrinsicBuff(Supplier<? extends BoardBuff> buff, int level) { }

}