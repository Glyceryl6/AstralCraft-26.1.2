package com.astral_craft.common.gameplay.board;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.*;

/**
 * Persistent board-only mechanics that are independent from the physical board graph.
 * The visual entities for these records are deliberately disposable and are rebuilt by
 * {@link BoardWorldObjectService} when they are missing.
 */
public class BoardMechanicsState {

    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
    private final List<String> characterStartNodes = new ArrayList<>();
    private final List<BoardTrap> traps = new ArrayList<>();
    private final Map<String, Integer> droppedCoins = new LinkedHashMap<>();
    private final List<BoardSoulLink> soulLinks = new ArrayList<>();
    private Optional<UUID> timeBombSlot;

    private BoardMechanicsState(Snapshot snapshot) {
        this.characterStartNodes.addAll(snapshot.characterStartNodes());
        this.traps.addAll(snapshot.traps());
        snapshot.droppedCoins().forEach((nodeId, amount) -> {
            if (nodeId != null && !nodeId.isBlank() && amount != null && amount > 0) {
                this.droppedCoins.put(nodeId, amount);
            }
        });

        this.timeBombSlot = snapshot.timeBombSlot();
        this.soulLinks.addAll(snapshot.soulLinks());
    }

    public static BoardMechanicsState fromSnapshot(Snapshot snapshot) {
        return new BoardMechanicsState(snapshot == null ? Snapshot.EMPTY : snapshot);
    }

    public Snapshot snapshot() {
        return new Snapshot(this.characterStartNodes(), this.traps(), this.droppedCoins(), this.timeBombSlot, this.soulLinks());
    }

    public List<String> characterStartNodes() {
        return List.copyOf(this.characterStartNodes);
    }

    public boolean hasCompleteCharacterStarts() {
        return this.characterStartNodes.size() == BoardSessionManager.REQUIRED_PLAYERS;
    }

    public void retainCharacterStarts(List<String> validStartNodes) {
        Set<String> valid = Set.copyOf(validStartNodes);
        LinkedHashSet<String> retained = new LinkedHashSet<>();
        for (String nodeId : this.characterStartNodes) {
            if (valid.contains(nodeId) && retained.size() < BoardSessionManager.REQUIRED_PLAYERS) {
                retained.add(nodeId);
            }
        }

        this.characterStartNodes.clear();
        this.characterStartNodes.addAll(retained);
    }

    public MarkerResult toggleCharacterStart(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) return MarkerResult.INVALID;
        int existing = this.characterStartNodes.indexOf(nodeId);
        if (existing >= 0) {
            this.characterStartNodes.remove(existing);
            return MarkerResult.REMOVED;
        }

        if (this.characterStartNodes.size() >= BoardSessionManager.REQUIRED_PLAYERS) return MarkerResult.FULL;
        this.characterStartNodes.add(nodeId);
        return MarkerResult.ADDED;
    }

    public Optional<String> undoCharacterStart() {
        if (this.characterStartNodes.isEmpty()) return Optional.empty();
        return Optional.of(this.characterStartNodes.removeLast());
    }

    public void clearCharacterStarts() {
        this.characterStartNodes.clear();
    }

    public List<BoardTrap> traps() {
        return List.copyOf(this.traps);
    }

    public List<BoardTrap> trapsAt(String nodeId) {
        return this.traps.stream().filter(trap -> trap.nodeId().equals(nodeId)).toList();
    }

    public BoardTrap addTrap(BoardTrapType type, UUID ownerSlotId, String nodeId) {
        if (type.barricade()) {
            this.traps.removeIf(trap -> trap.nodeId().equals(nodeId) && trap.type().barricade());
        } else if (type.singlePerNode()) {
            this.traps.removeIf(trap -> trap.nodeId().equals(nodeId) && trap.type() == type);
        }

        BoardTrap trap = new BoardTrap(UUID.randomUUID(), type, ownerSlotId, nodeId);
        this.traps.add(trap);
        return trap;
    }

    public void removeTrap(UUID trapId) {
        this.traps.removeIf(trap -> trap.id().equals(trapId));
    }

    public int removeBarricades(String nodeId) {
        int before = this.traps.size();
        this.traps.removeIf(trap -> trap.nodeId().equals(nodeId) && trap.type().barricade());
        return before - this.traps.size();
    }

    public void clearTraps() {
        this.traps.clear();
    }

    public Map<String, Integer> droppedCoins() {
        return Map.copyOf(this.droppedCoins);
    }

    public int droppedCoinsAt(String nodeId) {
        return Math.max(0, this.droppedCoins.getOrDefault(nodeId, 0));
    }

    public void addDroppedCoins(String nodeId, int amount) {
        if (nodeId == null || nodeId.isBlank() || amount <= 0) return;
        this.droppedCoins.merge(nodeId, amount, Integer::sum);
    }

    public int removeDroppedCoins(String nodeId) {
        Integer removed = this.droppedCoins.remove(nodeId);
        return removed == null ? 0 : Math.max(0, removed);
    }

    public Optional<UUID> timeBombSlot() {
        return this.timeBombSlot;
    }

    public void setTimeBombSlot(Optional<UUID> slotId) {
        this.timeBombSlot = slotId == null ? Optional.empty() : slotId;
    }

    public List<BoardSoulLink> soulLinks() {
        return List.copyOf(this.soulLinks);
    }

    public boolean addSoulLink(UUID firstSlotId, UUID secondSlotId, int rounds) {
        if (firstSlotId == null || secondSlotId == null || firstSlotId.equals(secondSlotId) || rounds <= 0) return false;
        boolean occupied = this.soulLinks.stream().anyMatch(link -> link.contains(firstSlotId) || link.contains(secondSlotId));
        if (occupied) return false;
        this.soulLinks.add(new BoardSoulLink(UUID.randomUUID(), firstSlotId, secondSlotId, rounds));
        return true;
    }

    public void setSoulLinks(List<BoardSoulLink> links) {
        this.soulLinks.clear();
        if (links != null) this.soulLinks.addAll(links);
    }

    public Optional<BoardSoulLink> soulLinkFor(UUID slotId) {
        if (slotId == null) return Optional.empty();
        return this.soulLinks.stream().filter(link -> link.contains(slotId)).findFirst();
    }

    public List<BoardSoulLink> tickSoulLinks() {
        List<BoardSoulLink> expired = new ArrayList<>();
        List<BoardSoulLink> active = new ArrayList<>();
        for (BoardSoulLink link : this.soulLinks) {
            BoardSoulLink next = link.tickRound();
            if (next.remainingRounds() <= 0) expired.add(link);
            else active.add(next);
        }
        this.soulLinks.clear();
        this.soulLinks.addAll(active);
        return List.copyOf(expired);
    }

    public void removeSoulLink(UUID linkId) {
        if (linkId != null) this.soulLinks.removeIf(link -> link.id().equals(linkId));
    }

    public void clearRuntimeGameState() {
        this.traps.clear();
        this.droppedCoins.clear();
        this.timeBombSlot = Optional.empty();
        this.soulLinks.clear();
    }

    public enum MarkerResult {
        ADDED,
        REMOVED,
        FULL,
        INVALID
    }

    public enum BoardTrapType {
        ENTRAPMENT("entrapment", false, false),
        DEMOLITION("demolition", false, false),
        BARRICADE("barricade", true, true),
        ENHANCED_BARRICADE("enhanced_barricade", true, true);

        public static final Codec<BoardTrapType> CODEC = Codec.STRING.xmap(BoardTrapType::byName, BoardTrapType::serializedName);

        private final String serializedName;
        private final boolean barricade;
        private final boolean singlePerNode;

        BoardTrapType(String serializedName, boolean barricade, boolean singlePerNode) {
            this.serializedName = serializedName;
            this.barricade = barricade;
            this.singlePerNode = singlePerNode;
        }

        public String serializedName() {
            return this.serializedName;
        }

        public boolean barricade() {
            return this.barricade;
        }

        public boolean singlePerNode() {
            return this.singlePerNode;
        }

        private static BoardTrapType byName(String value) {
            for (BoardTrapType type : values()) {
                if (type.serializedName.equals(value)) return type;
            }
            return ENTRAPMENT;
        }
    }

    public record BoardTrap(UUID id, BoardTrapType type, UUID ownerSlotId, String nodeId) {
        public static final Codec<BoardTrap> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUID_CODEC.fieldOf("id").forGetter(BoardTrap::id),
                BoardTrapType.CODEC.fieldOf("type").forGetter(BoardTrap::type),
                UUID_CODEC.fieldOf("owner_slot_id").forGetter(BoardTrap::ownerSlotId),
                Codec.STRING.fieldOf("node_id").forGetter(BoardTrap::nodeId)
        ).apply(instance, BoardTrap::new));
    }

    public record BoardSoulLink(UUID id, UUID firstSlotId, UUID secondSlotId, int remainingRounds) {
        public static final Codec<BoardSoulLink> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUID_CODEC.fieldOf("id").forGetter(BoardSoulLink::id),
                UUID_CODEC.fieldOf("first_slot_id").forGetter(BoardSoulLink::firstSlotId),
                UUID_CODEC.fieldOf("second_slot_id").forGetter(BoardSoulLink::secondSlotId),
                Codec.INT.fieldOf("remaining_rounds").forGetter(BoardSoulLink::remainingRounds)
        ).apply(instance, BoardSoulLink::new));

        public BoardSoulLink {
            remainingRounds = Math.max(0, remainingRounds);
        }

        public boolean contains(UUID slotId) {
            return this.firstSlotId.equals(slotId) || this.secondSlotId.equals(slotId);
        }

        public Optional<UUID> other(UUID slotId) {
            if (this.firstSlotId.equals(slotId)) return Optional.of(this.secondSlotId);
            if (this.secondSlotId.equals(slotId)) return Optional.of(this.firstSlotId);
            return Optional.empty();
        }

        public BoardSoulLink tickRound() {
            return new BoardSoulLink(this.id, this.firstSlotId, this.secondSlotId,
                    Math.max(0, this.remainingRounds - 1));
        }
    }

    public record Snapshot(List<String> characterStartNodes, List<BoardTrap> traps,
                           Map<String, Integer> droppedCoins, Optional<UUID> timeBombSlot,
                           List<BoardSoulLink> soulLinks) {
        public static final Snapshot EMPTY = new Snapshot(List.of(), List.of(), Map.of(), Optional.empty(), List.of());
        public static final Codec<Snapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.listOf().optionalFieldOf("character_start_nodes", List.of())
                        .forGetter(Snapshot::characterStartNodes),
                BoardTrap.CODEC.listOf().optionalFieldOf("traps", List.of()).forGetter(Snapshot::traps),
                Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("dropped_coins", Map.of())
                        .forGetter(Snapshot::droppedCoins),
                UUID_CODEC.optionalFieldOf("time_bomb_slot").forGetter(Snapshot::timeBombSlot),
                BoardSoulLink.CODEC.listOf().optionalFieldOf("soul_links", List.of()).forGetter(Snapshot::soulLinks)
        ).apply(instance, Snapshot::new));

        public Snapshot {
            characterStartNodes = List.copyOf(characterStartNodes);
            traps = List.copyOf(traps);
            droppedCoins = Map.copyOf(droppedCoins);
            timeBombSlot = timeBombSlot == null ? Optional.empty() : timeBombSlot;
            soulLinks = List.copyOf(soulLinks);
        }
    }

}