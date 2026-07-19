package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.entity.projectile.CardProjectileSettings;
import com.astral_craft.common.entity.projectile.SlingshotProjectileEntity;
import com.astral_craft.common.entity.projectile.SnowballAttackProjectileEntity;
import com.astral_craft.common.entity.visual.FallingBrickEntity;
import com.astral_craft.common.entity.visual.LaserStrikeEntity;
import com.astral_craft.common.gameplay.BuffKinds;
import com.astral_craft.common.gameplay.DamagePresentation;
import com.astral_craft.common.stats.AstralPlayerStats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

/** Server-side operations exposed to board-compatible automated effect cards. */
public record BoardBotEffectContext(ServerLevel level, BoardSession session, UUID userSlotId, CardDefinition definition, List<UUID> targetSlotIds) {

    public BoardBotEffectContext {
        targetSlotIds = List.copyOf(targetSlotIds == null ? List.of() : targetSlotIds);
    }

    public BoardBotEffectContext withTargets(List<UUID> targets) {
        return new BoardBotEffectContext(this.level, this.session, this.userSlotId, this.definition, targets);
    }

    public BoardParticipant user() {
        return this.session.participant(this.userSlotId).orElseThrow();
    }

    public Optional<BoardParticipant> target(UUID slotId) {
        return this.session.participant(slotId);
    }

    public Optional<BoardParticipant> firstTarget() {
        return this.targetSlotIds.stream().map(this.session::participant)
                .flatMap(Optional::stream).findFirst();
    }

    public Optional<UUID> randomOpponentSlot(int maximumRange) {
        List<UUID> candidates = this.opponentSlotsInRange(maximumRange);
        return candidates.isEmpty() ? Optional.empty()
                : Optional.of(candidates.get(this.level.getRandom().nextInt(candidates.size())));
    }

    public List<UUID> opponentSlotsInRange(int maximumRange) {
        BoardParticipant source = this.user();
        int range = maximumRange < 0 ? Integer.MAX_VALUE : maximumRange;
        return this.session.participants().stream()
                .filter(target -> !target.slotUuid().equals(source.slotUuid()))
                .filter(target -> target.stats().health() > 0 && !target.knockedDown())
                .filter(target -> BoardRouteService.graphDistance(this.session,
                        source.currentNodeKey(), target.currentNodeKey(), range) >= 0)
                .map(BoardParticipant::slotUuid).toList();
    }

    public void updateUser(UnaryOperator<AstralPlayerStats> operation) {
        BoardParticipant participant = this.user();
        BoardSessionManager.updateParticipant(this.level, this.session,
                participant.withStats(operation.apply(participant.stats())));
    }

    public void reduceUserSkillCooldown(int turns) {
        if (turns <= 0) return;
        BoardParticipant participant = this.user();
        BoardSessionManager.updateParticipant(this.level, this.session, participant.withSkillCooldown(
                Math.max(0, participant.skillCooldownTurns() - turns)));
    }

    public void updateTarget(UUID slotId, UnaryOperator<AstralPlayerStats> operation) {
        this.target(slotId).ifPresent(participant -> BoardSessionManager.updateParticipant(this.level, this.session,
                participant.withStats(operation.apply(participant.stats()))));
    }

    public void damageTarget(UUID slotId, int amount, boolean rewardKnockout) {
        BoardParticipant source = this.user();
        this.target(slotId).ifPresent(target -> this.applyDamage(source, target, amount, rewardKnockout));
    }

    public void damageUser(int amount) {
        BoardParticipant source = this.user();
        this.applyDamage(source, source, amount, false);
    }

    public int slingshot(UUID targetSlotId, int amount) {
        AstralCharacterEntity source = BoardEntityService.entity(this.level, this.user());
        BoardParticipant targetParticipant = this.target(targetSlotId).orElse(null);
        AstralCharacterEntity target = targetParticipant == null ? null
                : BoardEntityService.entity(this.level, targetParticipant);
        if (source == null || target == null) return 0;
        CardProjectileSettings settings = CardProjectileSettings.slingshot();
        source.playBoardAttackAnimation(12);
        this.level.addFreshEntity(new SlingshotProjectileEntity(this.level, source, target, amount, settings));
        this.level.playSound(null, source.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.9F, 1.8F);
        return settings.durationTicks() + 5;
    }

    public int snowball(UUID targetSlotId, int amount) {
        AstralCharacterEntity source = BoardEntityService.entity(this.level, this.user());
        BoardParticipant targetParticipant = this.target(targetSlotId).orElse(null);
        AstralCharacterEntity target = targetParticipant == null ? null
                : BoardEntityService.entity(this.level, targetParticipant);
        if (source == null || target == null) return 0;
        CardProjectileSettings settings = CardProjectileSettings.snowballAttack();
        source.playBoardAttackAnimation(12);
        this.level.addFreshEntity(new SnowballAttackProjectileEntity(this.level, source, target, amount, settings));
        this.level.playSound(null, source.blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.8F, 1.2F);
        return settings.durationTicks() + 5;
    }

    public int laser(UUID targetSlotId, int amount) {
        AstralCharacterEntity source = BoardEntityService.entity(this.level, this.user());
        BoardParticipant targetParticipant = this.target(targetSlotId).orElse(null);
        AstralCharacterEntity target = targetParticipant == null ? null
                : BoardEntityService.entity(this.level, targetParticipant);
        if (source == null || target == null) return 0;
        source.playBoardAttackAnimation(12);
        this.level.addFreshEntity(new LaserStrikeEntity(this.level, source, target, amount, 0xFF66E8FF, 0.12F));
        this.level.playSound(null, source.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9F, 1.35F);
        return 22;
    }

    public int brick(UUID targetSlotId, int amount) {
        AstralCharacterEntity source = BoardEntityService.entity(this.level, this.user());
        BoardParticipant targetParticipant = this.target(targetSlotId).orElse(null);
        AstralCharacterEntity target = targetParticipant == null ? null
                : BoardEntityService.entity(this.level, targetParticipant);
        if (source == null || target == null) return 0;
        source.playBoardAttackAnimation(12);
        this.level.addFreshEntity(new FallingBrickEntity(this.level, source, target, amount, 10));
        this.level.playSound(null, source.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.55F, 1.55F);
        return 28;
    }

    private void applyDamage(BoardParticipant attacker, BoardParticipant target, int damage, boolean rewardKnockout) {
        int resolvedDamage = Math.max(0, damage + target.stats().incomingDamageBonus()
                + Math.min(1, target.stats().buff(BuffKinds.MARK)));
        if (resolvedDamage >= DamagePresentation.CRITICAL_DAMAGE_THRESHOLD) {
            AstralCharacterEntity targetEntity = BoardEntityService.entity(this.level, target);
            if (targetEntity != null) DamagePresentation.playCriticalImpact(this.level, targetEntity);
        }

        BoardParticipant damaged = target.withStats(target.stats().damage(resolvedDamage));
        if (damaged.stats().health() <= 0) {
            int lostCoins = Math.max(0, (target.stats().starCoins() + 1) / 2);
            damaged = damaged.knockDown();
            BoardWorldObjectService.dropCoins(this.level, this.session, target.currentNodeKey(), lostCoins);
        }

        BoardSessionManager.updateParticipant(this.level, this.session, damaged);
    }

    public void snatch(UUID targetSlotId, int amount) {
        BoardParticipant target = this.target(targetSlotId).orElse(null);
        if (target == null) return;
        int taken = Math.min(amount, target.stats().starCoins());
        BoardSessionManager.updateParticipant(this.level, this.session,
                target.withStats(target.stats().spendCoins(taken)));
        BoardWorldObjectService.awardCoins(this.level, this.session, this.userSlotId, taken);
    }

}