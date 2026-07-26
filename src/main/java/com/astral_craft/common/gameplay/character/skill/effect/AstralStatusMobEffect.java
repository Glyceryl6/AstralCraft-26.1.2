package com.astral_craft.common.gameplay.character.skill.effect;

import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.character.ActiveCharacterState;
import com.astral_craft.common.gameplay.character.CharacterManager;
import com.astral_craft.common.gameplay.character.CharacterProgressManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

import java.util.Optional;

public class AstralStatusMobEffect extends MobEffect {

    protected final Identifier statusId;
    protected final Identifier iconTexture;
    protected final Identifier characterId;

    public AstralStatusMobEffect(MobEffectCategory category, int color, Identifier statusId, Identifier iconTexture, Identifier characterId) {
        super(category, color);
        this.statusId = statusId;
        this.iconTexture = iconTexture;
        this.characterId = characterId;
    }

    public Identifier statusId() {
        return this.statusId;
    }

    public Identifier iconTexture() {
        return this.iconTexture;
    }

    public Optional<Identifier> characterId() {
        return Optional.ofNullable(this.characterId);
    }

    public boolean canApplyTo(LivingEntity entity) {
        if (this.characterId == null) return true;
        if (entity instanceof AstralCharacterEntity character) {
            return this.characterId.equals(character.characterId()) && this.activeCharacterDefinesStatus();
        }
        if (!(entity instanceof ServerPlayer player)) return false;
        ActiveCharacterState state = CharacterProgressManager.activeState(player);
        return state.active() && this.characterId.equals(state.characterId()) && this.activeCharacterDefinesStatus();
    }

    public void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (!this.canApplyTo(event.getEntity())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    public void onEffectAdded(LivingEntity entity, MobEffectInstance instance) {}

    public void onEffectRemoved(LivingEntity entity) {}

    public void onEffectExpired(LivingEntity entity, MobEffectInstance instance) {
        this.onEffectRemoved(entity);
    }

    public void onAttackEntity(AttackEntityEvent event) {}

    public void onLivingChangeTarget(LivingChangeTargetEvent event) {}

    public void onInvulnerabilityCheck(EntityInvulnerabilityCheckEvent event) {}

    protected boolean activeCharacterDefinesStatus() {
        if (this.characterId == null || this.statusId == null || !CharacterManager.INSTANCE.contains(this.characterId)) return false;
        var character = CharacterManager.INSTANCE.character(this.characterId);
        if (character.activeSkill().statusEffectId().filter(this.statusId::equals).isPresent()) return true;
        return character.passiveSkills().stream()
                .anyMatch(skill -> skill.statusEffectId().filter(this.statusId::equals).isPresent());
    }

}