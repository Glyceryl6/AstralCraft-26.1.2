package com.astral_craft.common.gameplay.board;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.stats.AstralPlayerStats;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** Persisted participant data with typed resource and UUID identifiers. */
public record BoardParticipant(
        UUID slotId,
        Optional<UUID> controllerId,
        boolean bot,
        boolean disconnectedHuman,
        Identifier characterId,
        Identifier skinId,
        Identifier currentNodeId,
        Identifier previousNodeId,
        Optional<UUID> entityId,
        AstralPlayerStats stats,
        List<Identifier> hand,
        Map<Identifier, Integer> roundStatusEffects,
        int skillCooldownTurns,
        int knockedDownTurns,
        int cardPlaysUsed,
        int maxHandSize,
        int arrivalOrder,
        int decisionTimeoutStrikes) {

    public static final Identifier EMPTY_NODE_ID = AstralCraft.prefix("board_node/none");
    private static final Codec<Map<Identifier, Integer>> ROUND_STATUS_EFFECTS_CODEC =
            Codec.unboundedMap(Identifier.CODEC, Codec.INT);
    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(BoardParticipant::parseUuid, UUID::toString);
    private static final Codec<Optional<UUID>> OPTIONAL_UUID_CODEC = Codec.STRING.xmap(
            BoardParticipant::parseOptionalUuid,
            value -> value.map(UUID::toString).orElse(""));
    private static final Codec<Identifier> LEGACY_IDENTIFIER_CODEC = Codec.STRING.xmap(
            BoardParticipant::parseIdentifier,
            Identifier::toString);
    private static final MapCodec<ControlFlags> CONTROL_FLAGS_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("bot", false).forGetter(ControlFlags::bot),
            Codec.BOOL.optionalFieldOf("disconnected_human", false).forGetter(ControlFlags::disconnectedHuman),
            Codec.INT.optionalFieldOf("decision_timeout_strikes", 0).forGetter(ControlFlags::decisionTimeoutStrikes)
    ).apply(instance, ControlFlags::new));

    public static final Codec<BoardParticipant> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUID_CODEC.fieldOf("slot_id").forGetter(BoardParticipant::slotId),
            OPTIONAL_UUID_CODEC.optionalFieldOf("controller_id", Optional.empty()).forGetter(BoardParticipant::controllerId),
            CONTROL_FLAGS_CODEC.forGetter(participant -> new ControlFlags(
                    participant.bot(), participant.disconnectedHuman(), participant.decisionTimeoutStrikes())),
            Identifier.CODEC.fieldOf("character_id").forGetter(BoardParticipant::characterId),
            LEGACY_IDENTIFIER_CODEC.optionalFieldOf("skin_id", Identifier.withDefaultNamespace("default"))
                    .forGetter(BoardParticipant::skinId),
            LEGACY_IDENTIFIER_CODEC.optionalFieldOf("current_node", EMPTY_NODE_ID)
                    .forGetter(BoardParticipant::currentNodeId),
            LEGACY_IDENTIFIER_CODEC.optionalFieldOf("previous_node", EMPTY_NODE_ID)
                    .forGetter(BoardParticipant::previousNodeId),
            OPTIONAL_UUID_CODEC.optionalFieldOf("entity_id", Optional.empty()).forGetter(BoardParticipant::entityId),
            AstralPlayerStats.CODEC.optionalFieldOf("stats", AstralPlayerStats.DEFAULT).forGetter(BoardParticipant::stats),
            Identifier.CODEC.listOf().optionalFieldOf("hand", List.of()).forGetter(BoardParticipant::hand),
            ROUND_STATUS_EFFECTS_CODEC.optionalFieldOf("round_status_effects", Map.of())
                    .forGetter(BoardParticipant::roundStatusEffects),
            Codec.INT.optionalFieldOf("skill_cooldown_turns", 0).forGetter(BoardParticipant::skillCooldownTurns),
            Codec.INT.optionalFieldOf("knocked_down_turns", 0).forGetter(BoardParticipant::knockedDownTurns),
            Codec.INT.optionalFieldOf("card_plays_used", 0).forGetter(BoardParticipant::cardPlaysUsed),
            Codec.INT.optionalFieldOf("max_hand_size", 7).forGetter(BoardParticipant::maxHandSize),
            Codec.INT.optionalFieldOf("arrival_order", 0).forGetter(BoardParticipant::arrivalOrder)
    ).apply(instance, BoardParticipant::fromCodecParts));

    public BoardParticipant(
            UUID slotId, Optional<UUID> controllerId, boolean bot,
            Identifier characterId, Identifier skinId, Identifier currentNodeId, Identifier previousNodeId,
            Optional<UUID> entityId, AstralPlayerStats stats, List<Identifier> hand,
            Map<Identifier, Integer> roundStatusEffects, int skillCooldownTurns, int knockedDownTurns,
            int cardPlaysUsed, int maxHandSize, int arrivalOrder) {
        this(slotId, controllerId, bot, false, characterId, skinId, currentNodeId, previousNodeId, entityId,
                stats, hand, roundStatusEffects, skillCooldownTurns, knockedDownTurns, cardPlaysUsed,
                maxHandSize, arrivalOrder, 0);
    }

    public BoardParticipant(
            UUID slotId, Optional<UUID> controllerId, boolean bot, boolean disconnectedHuman,
            Identifier characterId, Identifier skinId, Identifier currentNodeId, Identifier previousNodeId,
            Optional<UUID> entityId, AstralPlayerStats stats, List<Identifier> hand,
            Map<Identifier, Integer> roundStatusEffects, int skillCooldownTurns, int knockedDownTurns,
            int cardPlaysUsed, int maxHandSize, int arrivalOrder) {
        this(slotId, controllerId, bot, disconnectedHuman, characterId, skinId, currentNodeId, previousNodeId,
                entityId, stats, hand, roundStatusEffects, skillCooldownTurns, knockedDownTurns, cardPlaysUsed,
                maxHandSize, arrivalOrder, 0);
    }

    public BoardParticipant {
        slotId = slotId == null ? UUID.randomUUID() : slotId;
        characterId = characterId == null ? AstralCraft.prefix("mimi") : characterId;
        skinId = skinId == null ? skinIdentifier(characterId, "default") : skinId;
        currentNodeId = currentNodeId == null ? EMPTY_NODE_ID : currentNodeId;
        previousNodeId = previousNodeId == null ? EMPTY_NODE_ID : previousNodeId;
        stats = stats == null ? AstralPlayerStats.DEFAULT : stats;
        hand = List.copyOf(hand == null ? List.of() : hand);
        roundStatusEffects = copyRoundStatusEffects(roundStatusEffects);
        skillCooldownTurns = Math.max(0, skillCooldownTurns);
        knockedDownTurns = Math.max(0, knockedDownTurns);
        cardPlaysUsed = Math.max(0, cardPlaysUsed);
        maxHandSize = Math.clamp(maxHandSize, 1, 64);
        arrivalOrder = Math.max(0, arrivalOrder);
        decisionTimeoutStrikes = Math.clamp(decisionTimeoutStrikes, 0, 8);
    }

    public UUID slotUuid() {
        return this.slotId;
    }

    public Optional<UUID> controllerUuid() {
        return this.controllerId;
    }

    public Optional<UUID> entityUuid() {
        return this.entityId;
    }

    public String skinName() {
        return this.skinId.getPath();
    }

    public String currentNodeKey() {
        return this.currentNodeId.toString();
    }

    public String previousNodeKey() {
        return this.previousNodeId.toString();
    }

    public boolean hasPreviousNode() {
        return !this.previousNodeId.equals(EMPTY_NODE_ID);
    }

    public boolean controlledBy(UUID playerId) {
        return this.controllerId.filter(playerId::equals).isPresent();
    }

    public BoardParticipant withNode(String previousNodeId, String currentNodeId, int arrivalOrder) {
        return this.withNode(nodeIdentifier(previousNodeId), nodeIdentifier(currentNodeId), arrivalOrder);
    }

    public BoardParticipant withNode(Identifier previousNodeId, Identifier currentNodeId, int arrivalOrder) {
        return new BoardParticipant(this.slotId, this.controllerId, this.bot, this.disconnectedHuman, this.characterId, this.skinId,
                currentNodeId, previousNodeId, this.entityId, this.stats, this.hand, this.roundStatusEffects,
                this.skillCooldownTurns, this.knockedDownTurns, this.cardPlaysUsed, this.maxHandSize, arrivalOrder, this.decisionTimeoutStrikes);
    }

    public BoardParticipant withEntity(@Nullable UUID entityUuid) {
        return new BoardParticipant(this.slotId, this.controllerId, this.bot, this.disconnectedHuman, this.characterId, this.skinId,
                this.currentNodeId, this.previousNodeId, Optional.ofNullable(entityUuid), this.stats, this.hand,
                this.roundStatusEffects, this.skillCooldownTurns, this.knockedDownTurns, this.cardPlaysUsed,
                this.maxHandSize, this.arrivalOrder, this.decisionTimeoutStrikes);
    }

    public BoardParticipant withStats(AstralPlayerStats stats) {
        return new BoardParticipant(this.slotId, this.controllerId, this.bot, this.disconnectedHuman, this.characterId, this.skinId,
                this.currentNodeId, this.previousNodeId, this.entityId, stats, this.hand,
                this.roundStatusEffects, this.skillCooldownTurns, this.knockedDownTurns, this.cardPlaysUsed,
                this.maxHandSize, this.arrivalOrder, this.decisionTimeoutStrikes);
    }

    public BoardParticipant withHand(List<Identifier> hand) {
        return new BoardParticipant(this.slotId, this.controllerId, this.bot, this.disconnectedHuman, this.characterId, this.skinId,
                this.currentNodeId, this.previousNodeId, this.entityId, this.stats, hand,
                this.roundStatusEffects, this.skillCooldownTurns, this.knockedDownTurns, this.cardPlaysUsed,
                this.maxHandSize, this.arrivalOrder, this.decisionTimeoutStrikes);
    }

    public BoardParticipant withRoundStatusEffect(Identifier statusId, int turns) {
        if (statusId == null || turns <= 0) return this;
        Map<Identifier, Integer> next = new LinkedHashMap<>(this.roundStatusEffects);
        next.merge(statusId, turns, Math::max);
        return new BoardParticipant(this.slotId, this.controllerId, this.bot, this.disconnectedHuman, this.characterId, this.skinId,
                this.currentNodeId, this.previousNodeId, this.entityId, this.stats, this.hand, next,
                this.skillCooldownTurns, this.knockedDownTurns, this.cardPlaysUsed,
                this.maxHandSize, this.arrivalOrder, this.decisionTimeoutStrikes);
    }

    public boolean hasRoundStatusEffect(Identifier statusId) {
        return statusId != null && this.roundStatusEffects.getOrDefault(statusId, 0) > 0;
    }

    public BoardParticipant withoutRoundStatusEffect(Identifier statusId) {
        if (statusId == null || !this.roundStatusEffects.containsKey(statusId)) return this;
        Map<Identifier, Integer> next = new LinkedHashMap<>(this.roundStatusEffects);
        next.remove(statusId);
        return new BoardParticipant(this.slotId, this.controllerId, this.bot, this.disconnectedHuman,
                this.characterId, this.skinId, this.currentNodeId, this.previousNodeId, this.entityId,
                this.stats, this.hand, next, this.skillCooldownTurns, this.knockedDownTurns,
                this.cardPlaysUsed, this.maxHandSize, this.arrivalOrder, this.decisionTimeoutStrikes);
    }

    public BoardParticipant removeCard(int index) {
        if (index < 0 || index >= this.hand.size()) return this;
        List<Identifier> next = new ArrayList<>(this.hand);
        next.remove(index);
        return this.withHand(next);
    }

    public BoardParticipant addCard(Identifier cardId) {
        List<Identifier> next = new ArrayList<>(this.hand);
        next.add(cardId);
        return this.withHand(next);
    }

    public BoardParticipant beginTurn() {
        AstralPlayerStats nextStats = this.stats.beginTurn();
        int nextKnockdown = Math.max(0, this.knockedDownTurns - 1);
        boolean recoveryTurn = this.knockedDownTurns == 1;
        if (recoveryTurn) {
            nextStats = nextStats.withHealth(Math.max(1, nextStats.maxHealth()))
                    .clearNextMoveDiceEffects();
        } else if (this.knockedDownTurns > 1 || this.stats.health() <= 0) {
            nextStats = nextStats.withHealth(0).clearNextMoveDiceEffects();
        }
        return new BoardParticipant(this.slotId, this.controllerId, this.bot, this.disconnectedHuman, this.characterId, this.skinId,
                this.currentNodeId, this.previousNodeId, this.entityId, nextStats, this.hand,
                tickRoundStatusEffects(this.roundStatusEffects), Math.max(0, this.skillCooldownTurns - 1),
                nextKnockdown, 0, this.maxHandSize, this.arrivalOrder, this.decisionTimeoutStrikes);
    }

    public boolean knockedDown() {
        return this.knockedDownTurns > 0 || this.stats.health() <= 0;
    }

    public BoardParticipant repairKnockdownState() {
        if (this.stats.health() > 0 || this.knockedDownTurns > 0) return this;
        return new BoardParticipant(this.slotId, this.controllerId, this.bot, this.disconnectedHuman, this.characterId, this.skinId,
                this.currentNodeId, this.previousNodeId, this.entityId, this.stats.withHealth(0), this.hand,
                this.roundStatusEffects, this.skillCooldownTurns, 2, this.cardPlaysUsed,
                this.maxHandSize, this.arrivalOrder, this.decisionTimeoutStrikes);
    }

    public BoardParticipant asBot() {
        return new BoardParticipant(this.slotId, Optional.empty(), true, true, this.characterId, this.skinId,
                this.currentNodeId, this.previousNodeId, this.entityId, this.stats, this.hand,
                this.roundStatusEffects, this.skillCooldownTurns, this.knockedDownTurns, this.cardPlaysUsed,
                this.maxHandSize, this.arrivalOrder, this.decisionTimeoutStrikes);
    }

    public BoardParticipant endTurn() {
        return this.withStats(this.stats.endTurn());
    }

    public BoardParticipant useCardPlay() {
        return new BoardParticipant(this.slotId, this.controllerId, this.bot, this.disconnectedHuman, this.characterId, this.skinId,
                this.currentNodeId, this.previousNodeId, this.entityId,
                this.stats.useCardPlay(), this.hand, this.roundStatusEffects, this.skillCooldownTurns,
                this.knockedDownTurns, this.cardPlaysUsed + 1, this.maxHandSize, this.arrivalOrder, this.decisionTimeoutStrikes);
    }

    public BoardParticipant withSkillCooldown(int turns) {
        return new BoardParticipant(this.slotId, this.controllerId, this.bot, this.disconnectedHuman, this.characterId, this.skinId,
                this.currentNodeId, this.previousNodeId, this.entityId, this.stats, this.hand,
                this.roundStatusEffects, Math.max(0, turns), this.knockedDownTurns, this.cardPlaysUsed,
                this.maxHandSize, this.arrivalOrder, this.decisionTimeoutStrikes);
    }

    public BoardParticipant knockDown() {
        int lost = Math.max(0, (this.stats.starCoins() + 1) / 2);
        AstralPlayerStats next = this.stats.spendCoins(lost).withHealth(0);
        return new BoardParticipant(this.slotId, this.controllerId, this.bot, this.disconnectedHuman, this.characterId, this.skinId,
                this.currentNodeId, this.previousNodeId, this.entityId, next, this.hand,
                this.roundStatusEffects, this.skillCooldownTurns, 2, this.cardPlaysUsed,
                this.maxHandSize, this.arrivalOrder, this.decisionTimeoutStrikes);
    }

    public int decisionDurationTicks(int baseTicks) {
        int safeBase = Math.max(20, baseTicks);
        int reduced = safeBase / Math.max(1, this.decisionTimeoutStrikes + 1);
        return Math.max(20 * 5, reduced);
    }

    public BoardParticipant recordManualDecision() {
        if (this.decisionTimeoutStrikes == 0) return this;
        return new BoardParticipant(this.slotId, this.controllerId, this.bot, this.disconnectedHuman,
                this.characterId, this.skinId, this.currentNodeId, this.previousNodeId, this.entityId,
                this.stats, this.hand, this.roundStatusEffects, this.skillCooldownTurns,
                this.knockedDownTurns, this.cardPlaysUsed, this.maxHandSize, this.arrivalOrder, 0);
    }

    public BoardParticipant recordTimedOutDecision() {
        if (this.bot) return this;
        return new BoardParticipant(this.slotId, this.controllerId, this.bot, this.disconnectedHuman,
                this.characterId, this.skinId, this.currentNodeId, this.previousNodeId, this.entityId,
                this.stats, this.hand, this.roundStatusEffects, this.skillCooldownTurns,
                this.knockedDownTurns, this.cardPlaysUsed, this.maxHandSize, this.arrivalOrder,
                Math.min(8, this.decisionTimeoutStrikes + 1));
    }

    public static Identifier skinIdentifier(Identifier characterId, String skinId) {
        String path = skinId == null || skinId.isBlank() ? "default" : skinId;
        return Identifier.fromNamespaceAndPath(characterId.getNamespace(), path);
    }

    public static Identifier nodeIdentifier(String raw) {
        if (raw == null || raw.isBlank()) return EMPTY_NODE_ID;
        try {
            return Identifier.parse(raw);
        } catch (IllegalArgumentException ignored) {
            String[] values = raw.split(",", 3);
            if (values.length == 3) {
                try {
                    return Identifier.parse(BoardScanner.id(new BlockPos(
                            Integer.parseInt(values[0]), Integer.parseInt(values[1]), Integer.parseInt(values[2]))));
                } catch (IllegalArgumentException ignoredPosition) {}
            }
            return EMPTY_NODE_ID;
        }
    }

    private static Identifier parseIdentifier(String raw) {
        if (raw == null || raw.isBlank()) return EMPTY_NODE_ID;
        try {
            return Identifier.parse(raw);
        } catch (IllegalArgumentException exception) {
            return nodeIdentifier(raw);
        }
    }

    private static Map<Identifier, Integer> copyRoundStatusEffects(Map<Identifier, Integer> effects) {
        if (effects == null || effects.isEmpty()) return Map.of();
        Map<Identifier, Integer> result = new LinkedHashMap<>();
        effects.forEach((id, turns) -> {
            if (id != null && turns != null && turns > 0) result.put(id, turns);
        });
        return Map.copyOf(result);
    }

    private static Map<Identifier, Integer> tickRoundStatusEffects(Map<Identifier, Integer> effects) {
        if (effects.isEmpty()) return Map.of();
        Map<Identifier, Integer> next = new LinkedHashMap<>();
        effects.forEach((id, turns) -> {
            if (turns > 1) next.put(id, turns - 1);
        });
        return Map.copyOf(next);
    }

    private static UUID parseUuid(String raw) {
        if (raw != null) {
            try {
                return UUID.fromString(raw);
            } catch (IllegalArgumentException ignored) {}
        }
        String fallback = raw == null ? "" : raw;
        return UUID.nameUUIDFromBytes(("astral_craft:board_participant:" + fallback)
                .getBytes(StandardCharsets.UTF_8));
    }

    private static Optional<UUID> parseOptionalUuid(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static BoardParticipant fromCodecParts(
            UUID slotId, Optional<UUID> controllerId, ControlFlags controlFlags,
            Identifier characterId, Identifier skinId, Identifier currentNodeId, Identifier previousNodeId,
            Optional<UUID> entityId, AstralPlayerStats stats, List<Identifier> hand,
            Map<Identifier, Integer> roundStatusEffects, int skillCooldownTurns, int knockedDownTurns,
            int cardPlaysUsed, int maxHandSize, int arrivalOrder) {
        return new BoardParticipant(slotId, controllerId, controlFlags.bot(), controlFlags.disconnectedHuman(),
                characterId, skinId, currentNodeId, previousNodeId, entityId, stats, hand,
                roundStatusEffects, skillCooldownTurns, knockedDownTurns, cardPlaysUsed,
                maxHandSize, arrivalOrder, controlFlags.decisionTimeoutStrikes());
    }

    private record ControlFlags(boolean bot, boolean disconnectedHuman, int decisionTimeoutStrikes) {}

}