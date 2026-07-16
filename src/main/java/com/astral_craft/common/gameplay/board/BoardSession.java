package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.gameplay.BoardNode;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class BoardSession {

    private final UUID id;
    private final ResourceKey<Level> dimension;
    private final Map<String, BoardNode> nodes;
    private final Map<String, BlockPos> positions;
    private final BoardArea protectedArea;
    private final List<String> startNodes;
    private final LinkedHashMap<UUID, BoardParticipant> participants = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, String> homeNodes = new LinkedHashMap<>();
    private final List<UUID> turnOrder = new ArrayList<>();
    private BoardPhase phase;
    private int turnIndex;
    private int round;
    private boolean turnStarted;
    private long lobbyDeadlineTick;
    private long actionDeadlineTick;
    private int actionDurationTicks;
    private boolean protectionEnabled;
    private boolean keepAfterGame;
    private int arrivalSequence;
    private @Nullable MovementState movement;
    private @Nullable EncounterState encounter;
    private @Nullable DiscardState discard;

    public BoardSession(UUID id, ResourceKey<Level> dimension, ScannedBoard board) {
        this(id, dimension, board.nodes(), board.positions(), board.area(), board.startNodes(),
                BoardPhase.READY, List.of(), Map.of(), List.of(), 0, 0, false, true, true, 0);
    }

    private BoardSession(UUID id, ResourceKey<Level> dimension,
                         Map<String, BoardNode> nodes, Map<String, BlockPos> positions,
                         BoardArea protectedArea, List<String> startNodes,
                         BoardPhase phase, List<BoardParticipant> participants, Map<UUID, String> homeNodes,
                         List<UUID> turnOrder, int turnIndex, int round, boolean turnStarted,
                         boolean protectionEnabled, boolean keepAfterGame, int arrivalSequence) {
        this.id = id;
        this.dimension = dimension;
        CanonicalNodes canonicalNodes = canonicalizeNodes(nodes, positions, startNodes);
        this.nodes = Collections.unmodifiableMap(canonicalNodes.nodes());
        this.positions = Collections.unmodifiableMap(canonicalNodes.positions());
        this.protectedArea = protectedArea;
        this.startNodes = canonicalNodes.startNodes();
        this.phase = phase == null ? BoardPhase.READY : phase;
        for (BoardParticipant participant : participants) {
            this.participants.put(participant.slotUuid(), canonicalNodes.remapParticipant(participant));
        }
        homeNodes.forEach((slotId, nodeId) -> {
            if (slotId != null && nodeId != null && !nodeId.isBlank()) {
                this.homeNodes.put(slotId, canonicalNodes.remapNodeId(nodeId));
            }
        });
        this.turnOrder.addAll(turnOrder);
        this.turnIndex = Math.clamp(turnIndex, 0, Math.max(0, this.turnOrder.size() - 1));
        this.round = Math.max(0, round);
        this.turnStarted = turnStarted;
        this.protectionEnabled = protectionEnabled;
        this.keepAfterGame = keepAfterGame;
        this.arrivalSequence = Math.max(0, arrivalSequence);
    }

    public UUID id() {
        return this.id;
    }

    public ResourceKey<Level> dimension() {
        return this.dimension;
    }

    public Map<String, BoardNode> nodes() {
        return this.nodes;
    }

    public Map<String, BlockPos> positions() {
        return this.positions;
    }

    public BoardArea protectedArea() {
        return this.protectedArea;
    }

    public BlockPos hologramCenter() {
        return this.protectedArea.center().above(3);
    }

    public List<String> startNodes() {
        return this.startNodes;
    }

    public BoardPhase phase() {
        return this.phase;
    }

    public void setPhase(BoardPhase phase) {
        this.phase = phase == null ? BoardPhase.READY : phase;
    }

    public boolean protectionEnabled() {
        return this.protectionEnabled;
    }

    public void setProtectionEnabled(boolean protectionEnabled) {
        this.protectionEnabled = protectionEnabled;
    }

    public boolean keepAfterGame() {
        return this.keepAfterGame;
    }

    public void setKeepAfterGame(boolean keepAfterGame) {
        this.keepAfterGame = keepAfterGame;
    }

    public boolean protects(ResourceKey<Level> dimension, BlockPos pos) {
        return this.protectionEnabled && this.dimension.equals(dimension) && this.protectedArea.contains(pos);
    }

    public List<BoardParticipant> participants() {
        return List.copyOf(this.participants.values());
    }

    public Optional<BoardParticipant> participant(UUID slotId) {
        return Optional.ofNullable(this.participants.get(slotId));
    }

    public Optional<BoardParticipant> participantByController(UUID playerId) {
        return this.participants.values().stream().filter(participant -> participant.controlledBy(playerId)).findFirst();
    }

    public Optional<BoardParticipant> participantByEntity(UUID entityId) {
        return this.participants.values().stream()
                .filter(participant -> participant.entityUuid().filter(entityId::equals).isPresent()).findFirst();
    }

    public void putParticipant(BoardParticipant participant) {
        this.participants.put(participant.slotUuid(), participant);
    }

    public void removeParticipant(UUID slotId) {
        this.participants.remove(slotId);
        this.homeNodes.remove(slotId);
        this.turnOrder.remove(slotId);
        this.normalizeTurnIndex();
    }

    public boolean hasCharacter(Identifier characterId) {
        return this.participants.values().stream().anyMatch(participant -> participant.characterId().equals(characterId));
    }

    public int humanCount() {
        return (int) this.participants.values().stream().filter(participant -> !participant.bot()).count();
    }

    public int participantCount() {
        return this.participants.size();
    }

    public void setHomeNode(UUID slotId, String nodeId) {
        if (slotId == null || nodeId == null || nodeId.isBlank()) return;
        String canonical = canonicalNodeId(nodeId);
        if (this.nodes.containsKey(canonical)) {
            this.homeNodes.put(slotId, canonical);
        }
    }

    public Optional<String> homeNode(UUID slotId) {
        return Optional.ofNullable(this.homeNodes.get(slotId));
    }

    public boolean isHomeNode(BoardParticipant participant) {
        if (participant == null) return false;
        return this.homeNode(participant.slotUuid()).filter(participant.currentNodeKey()::equals).isPresent();
    }

    public void clearParticipants() {
        this.participants.clear();
        this.homeNodes.clear();
        this.turnOrder.clear();
        this.turnIndex = 0;
        this.round = 0;
        this.turnStarted = false;
        this.movement = null;
        this.encounter = null;
        this.discard = null;
    }

    public List<UUID> turnOrder() {
        return List.copyOf(this.turnOrder);
    }

    public void setTurnOrder(List<UUID> order) {
        this.turnOrder.clear();
        for (UUID slotId : order) {
            if (this.participants.containsKey(slotId) && !this.turnOrder.contains(slotId)) {
                this.turnOrder.add(slotId);
            }
        }

        this.normalizeTurnIndex();
    }

    public Optional<BoardParticipant> currentParticipant() {
        if (this.turnOrder.isEmpty()) return Optional.empty();
        this.normalizeTurnIndex();
        return this.participant(this.turnOrder.get(this.turnIndex));
    }

    public int turnIndex() {
        return this.turnIndex;
    }

    public int round() {
        return this.round;
    }

    public boolean turnStarted() {
        return this.turnStarted;
    }

    public void setTurnStarted(boolean turnStarted) {
        this.turnStarted = turnStarted;
    }

    public void advanceTurn() {
        this.turnStarted = false;
        if (this.turnOrder.isEmpty()) return;
        this.turnIndex++;
        if (this.turnIndex >= this.turnOrder.size()) {
            this.turnIndex = 0;
            this.round++;
        }
        this.movement = null;
        this.encounter = null;
        this.discard = null;
    }

    public long lobbyDeadlineTick() {
        return this.lobbyDeadlineTick;
    }

    public void setLobbyDeadlineTick(long tick) {
        this.lobbyDeadlineTick = Math.max(0L, tick);
    }

    public long actionDeadlineTick() {
        return this.actionDeadlineTick;
    }

    public void setActionDeadlineTick(long tick) {
        this.actionDeadlineTick = Math.max(0L, tick);
    }

    public int actionDurationTicks() {
        return this.actionDurationTicks;
    }

    public void setActionDurationTicks(int ticks) {
        this.actionDurationTicks = Math.max(0, ticks);
    }

    public int nextArrivalOrder() {
        return ++this.arrivalSequence;
    }

    public @Nullable MovementState movement() {
        return this.movement;
    }

    public void setMovement(@Nullable MovementState movement) {
        this.movement = movement;
    }

    public @Nullable EncounterState encounter() {
        return this.encounter;
    }

    public void setEncounter(@Nullable EncounterState encounter) {
        this.encounter = encounter;
    }

    public @Nullable DiscardState discard() {
        return this.discard;
    }

    public void setDiscard(@Nullable DiscardState discard) {
        this.discard = discard;
    }

    public Snapshot snapshot() {
        return new Snapshot(this.id.toString(), this.dimension.identifier(), this.nodes, this.positions,
                this.protectedArea, this.startNodes, this.phase, this.participants(),
                this.turnOrder.stream().map(UUID::toString).toList(), this.turnIndex, this.round, this.turnStarted,
                this.protectionEnabled, this.keepAfterGame, this.arrivalSequence, this.snapshotHomeNodes());
    }

    public static BoardSession fromSnapshot(Snapshot snapshot) {
        UUID id;
        try {
            id = UUID.fromString(snapshot.id());
        } catch (Exception exception) {
            id = UUID.randomUUID();
        }
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, snapshot.dimension());
        List<UUID> turnOrder = new ArrayList<>();
        for (String raw : snapshot.turnOrder()) {
            try {
                turnOrder.add(UUID.fromString(raw));
            } catch (Exception ignored) {
            }
        }

        Map<UUID, String> homeNodes = new LinkedHashMap<>();
        snapshot.homeNodes().forEach((rawSlotId, nodeId) -> {
            try {
                homeNodes.put(UUID.fromString(rawSlotId), nodeId);
            } catch (IllegalArgumentException ignored) {}
        });

        BoardSession session = new BoardSession(id, dimension, snapshot.nodes(), snapshot.positions(),
                snapshot.protectedArea(), snapshot.startNodes(), snapshot.phase(), snapshot.participants(), homeNodes,
                turnOrder, snapshot.turnIndex(), snapshot.round(), snapshot.turnStarted(), snapshot.protectionEnabled(),
                snapshot.keepAfterGame(), snapshot.arrivalSequence());
        if (session.phase == BoardPhase.PLAYING) {
            session.actionDeadlineTick = 0L;
        }
        if (session.phase == BoardPhase.CHARACTER_SELECTION) {
            session.lobbyDeadlineTick = 0L;
        }
        return session;
    }

    private Map<String, String> snapshotHomeNodes() {
        Map<String, String> result = new LinkedHashMap<>();
        this.homeNodes.forEach((slotId, nodeId) -> result.put(slotId.toString(), nodeId));
        return Map.copyOf(result);
    }

    private static CanonicalNodes canonicalizeNodes(
            Map<String, BoardNode> nodes, Map<String, BlockPos> positions, List<String> startNodes) {
        Map<String, String> remap = new LinkedHashMap<>();
        positions.forEach((oldId, pos) -> remap.put(oldId, BoardScanner.id(pos)));
        nodes.forEach((oldId, node) -> remap.putIfAbsent(oldId, canonicalNodeId(oldId)));

        Map<String, BlockPos> canonicalPositions = new LinkedHashMap<>();
        positions.forEach((oldId, pos) -> canonicalPositions.put(remap.get(oldId), pos));
        Map<String, BoardNode> canonicalNodes = new LinkedHashMap<>();
        nodes.forEach((oldId, node) -> {
            String nodeId = remap.get(oldId);
            List<String> next = node.next().stream().map(value -> remap.getOrDefault(value, canonicalNodeId(value))).toList();
            canonicalNodes.put(nodeId, new BoardNode(nodeId, node.platformId(), next));
        });
        List<String> canonicalStarts = startNodes.stream()
                .map(value -> remap.getOrDefault(value, canonicalNodeId(value))).toList();
        return new CanonicalNodes(new LinkedHashMap<>(canonicalNodes), new LinkedHashMap<>(canonicalPositions),
                List.copyOf(canonicalStarts), Map.copyOf(remap));
    }

    private static String canonicalNodeId(String raw) {
        return BoardParticipant.nodeIdentifier(raw).toString();
    }

    private record CanonicalNodes(Map<String, BoardNode> nodes, Map<String, BlockPos> positions,
                                  List<String> startNodes, Map<String, String> remap) {
        String remapNodeId(String nodeId) {
            return this.remap.getOrDefault(nodeId, canonicalNodeId(nodeId));
        }

        BoardParticipant remapParticipant(BoardParticipant participant) {
            Identifier current = BoardParticipant.nodeIdentifier(
                    this.remap.getOrDefault(participant.currentNodeKey(), participant.currentNodeKey()));
            Identifier previous = participant.hasPreviousNode()
                    ? BoardParticipant.nodeIdentifier(this.remap.getOrDefault(
                    participant.previousNodeKey(), participant.previousNodeKey()))
                    : BoardParticipant.EMPTY_NODE_ID;
            return participant.withNode(previous, current, participant.arrivalOrder());
        }
    }

    private void normalizeTurnIndex() {
        if (this.turnOrder.isEmpty()) {
            this.turnIndex = 0;
        } else {
            this.turnIndex = Math.floorMod(this.turnIndex, this.turnOrder.size());
        }
    }

    public record Snapshot(
            String id,
            Identifier dimension,
            Map<String, BoardNode> nodes,
            Map<String, BlockPos> positions,
            BoardArea protectedArea,
            List<String> startNodes,
            BoardPhase phase,
            List<BoardParticipant> participants,
            List<String> turnOrder,
            int turnIndex,
            int round,
            boolean turnStarted,
            boolean protectionEnabled,
            boolean keepAfterGame,
            int arrivalSequence,
            Map<String, String> homeNodes) {

        private static final Codec<Map<String, BoardNode>> NODE_MAP_CODEC = Codec.unboundedMap(Codec.STRING, BoardNode.CODEC);
        private static final Codec<Map<String, BlockPos>> POSITION_MAP_CODEC = Codec.unboundedMap(Codec.STRING, BlockPos.CODEC);

        public static final Codec<Snapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(Snapshot::id),
                Identifier.CODEC.fieldOf("dimension").forGetter(Snapshot::dimension),
                NODE_MAP_CODEC.fieldOf("nodes").forGetter(Snapshot::nodes),
                POSITION_MAP_CODEC.fieldOf("positions").forGetter(Snapshot::positions),
                BoardArea.CODEC.fieldOf("protected_area").forGetter(Snapshot::protectedArea),
                Codec.STRING.listOf().fieldOf("start_nodes").forGetter(Snapshot::startNodes),
                BoardPhase.CODEC.optionalFieldOf("phase", BoardPhase.READY).forGetter(Snapshot::phase),
                BoardParticipant.CODEC.listOf().optionalFieldOf("participants", List.of()).forGetter(Snapshot::participants),
                Codec.STRING.listOf().optionalFieldOf("turn_order", List.of()).forGetter(Snapshot::turnOrder),
                Codec.INT.optionalFieldOf("turn_index", 0).forGetter(Snapshot::turnIndex),
                Codec.INT.optionalFieldOf("round", 0).forGetter(Snapshot::round),
                Codec.BOOL.optionalFieldOf("turn_started", false).forGetter(Snapshot::turnStarted),
                Codec.BOOL.optionalFieldOf("protection_enabled", true).forGetter(Snapshot::protectionEnabled),
                Codec.BOOL.optionalFieldOf("keep_after_game", true).forGetter(Snapshot::keepAfterGame),
                Codec.INT.optionalFieldOf("arrival_sequence", 0).forGetter(Snapshot::arrivalSequence),
                Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("home_nodes", Map.of())
                        .forGetter(Snapshot::homeNodes)
        ).apply(instance, Snapshot::new));

        public Snapshot {
            nodes = Map.copyOf(nodes);
            positions = Map.copyOf(positions);
            startNodes = List.copyOf(startNodes);
            participants = List.copyOf(participants);
            turnOrder = List.copyOf(turnOrder);
            homeNodes = Map.copyOf(homeNodes);
        }
    }

    public record MovementState(UUID slotId, int remainingSteps, long nextStepTick,
                                List<String> route, List<String> branchChoices,
                                String activeTargetNodeId, long stepStartedTick, int stepDurationTicks) {
        public MovementState {
            remainingSteps = Math.max(0, remainingSteps);
            route = List.copyOf(route);
            branchChoices = List.copyOf(branchChoices);
            activeTargetNodeId = activeTargetNodeId == null ? "" : activeTargetNodeId;
            stepDurationTicks = Math.max(1, stepDurationTicks);
        }

        public static MovementState begin(UUID slotId, int steps, long tick) {
            return new MovementState(slotId, steps, tick, List.of(), List.of(), "", 0L, 6);
        }

        public MovementState beginStep(String nodeId, long tick, int durationTicks) {
            return new MovementState(this.slotId, this.remainingSteps, this.nextStepTick,
                    this.route, List.of(), nodeId, tick, durationTicks);
        }

        public MovementState completeStep(String nodeId, long nextStepTick) {
            List<String> nextRoute = new ArrayList<>(this.route);
            nextRoute.add(nodeId);
            return new MovementState(this.slotId, Math.max(0, this.remainingSteps - 1),
                    nextStepTick, nextRoute, List.of(), "", 0L, this.stepDurationTicks);
        }

        public MovementState waitingForBranch(List<String> choices, long deadlineTick) {
            return new MovementState(this.slotId, this.remainingSteps, Math.max(0L, deadlineTick),
                    this.route, choices, "", 0L, this.stepDurationTicks);
        }

        public MovementState stop() {
            return new MovementState(this.slotId, 0, this.nextStepTick,
                    this.route, List.of(), "", 0L, this.stepDurationTicks);
        }

        public boolean stepping() {
            return !this.activeTargetNodeId.isBlank();
        }
    }

    public record EncounterState(UUID moverSlotId, UUID targetSlotId, long deadlineTick, int durationTicks) {
        public EncounterState {
            durationTicks = Math.max(1, durationTicks);
        }
    }

    public record DiscardState(UUID slotId, int requiredCount, long deadlineTick, int durationTicks) {
        public DiscardState {
            durationTicks = Math.max(1, durationTicks);
        }
    }

}