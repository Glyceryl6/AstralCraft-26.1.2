package com.astral_craft.common.gameplay.board;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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
    private final Map<UUID, LinkedHashSet<Integer>> lotteryNumbers = new LinkedHashMap<>();
    private final Map<Identifier, Integer> timedEvents = new LinkedHashMap<>();
    private @Nullable UUID timeBombSlot;
    private int lotteryJackpot;

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
        snapshot.lotteryNumbers().forEach((rawSlotId, numbers) -> {
            try {
                UUID slotId = UUID.fromString(rawSlotId);
                LinkedHashSet<Integer> selected = new LinkedHashSet<>();
                for (int number : numbers) if (number >= 1 && number <= 12) selected.add(number);
                if (!selected.isEmpty()) this.lotteryNumbers.put(slotId, selected);
            } catch (IllegalArgumentException ignored) {}
        });

        snapshot.timedEvents().forEach((eventId, turns) -> {
            if (eventId != null && turns != null && turns > 0) this.timedEvents.put(eventId, turns);
        });
        this.lotteryJackpot = Math.max(10, snapshot.lotteryJackpot());
    }

    public static BoardMechanicsState fromSnapshot(Snapshot snapshot) {
        return new BoardMechanicsState(snapshot == null ? Snapshot.EMPTY : snapshot);
    }

    public Snapshot snapshot() {
        Map<String, List<Integer>> lottery = new LinkedHashMap<>();
        this.lotteryNumbers.forEach((slotId, numbers) -> lottery.put(slotId.toString(), List.copyOf(numbers)));
        return new Snapshot(this.characterStartNodes(), this.traps(), this.droppedCoins(), this.timeBombSlot,
                this.soulLinks(), lottery, this.lotteryJackpot, this.timedEvents());
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

    public void addTrap(BoardTrapType type, UUID ownerSlotId, String nodeId) {
        if (type.barricade()) {
            this.traps.removeIf(trap -> trap.nodeId().equals(nodeId) && trap.type().barricade());
        } else if (type.singlePerNode()) {
            this.traps.removeIf(trap -> trap.nodeId().equals(nodeId) && trap.type() == type);
        }

        BoardTrap trap = new BoardTrap(UUID.randomUUID(), type, ownerSlotId, nodeId);
        this.traps.add(trap);
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


    public List<Integer> lotteryNumbers(UUID slotId) {
        if (slotId == null) return List.of();
        return List.copyOf(this.lotteryNumbers.getOrDefault(slotId, new LinkedHashSet<>()));
    }

    public boolean selectLotteryNumber(UUID slotId, int number) {
        if (slotId == null || number < 1 || number > 12) return false;
        return this.lotteryNumbers.computeIfAbsent(slotId, ignored -> new LinkedHashSet<>()).add(number);
    }

    public List<UUID> lotteryWinners(int number) {
        if (number < 1 || number > 12) return List.of();
        return this.lotteryNumbers.entrySet().stream()
                .filter(entry -> entry.getValue().contains(number)).map(Map.Entry::getKey).toList();
    }

    public boolean hasLotteryEntries() {
        return this.lotteryNumbers.values().stream().anyMatch(numbers -> !numbers.isEmpty());
    }

    public void clearLotteryNumbers() {
        this.lotteryNumbers.clear();
    }

    public int lotteryJackpot() {
        return Math.max(10, this.lotteryJackpot);
    }

    public void increaseLotteryJackpot() {
        this.lotteryJackpot = this.lotteryJackpot() + 10;
    }

    public void resetLotteryJackpot() {
        this.lotteryJackpot = 10;
    }

    public Map<Identifier, Integer> timedEvents() {
        return Map.copyOf(this.timedEvents);
    }

    public int timedEventTurns(Identifier eventId) {
        return eventId == null ? 0 : Math.max(0, this.timedEvents.getOrDefault(eventId, 0));
    }

    public void setTimedEvent(Identifier eventId, int turns) {
        if (eventId == null) return;
        if (turns <= 0) this.timedEvents.remove(eventId);
        else this.timedEvents.put(eventId, turns);
    }

    public void tickTimedEvent(Identifier eventId) {
        int remaining = this.timedEventTurns(eventId);
        if (remaining <= 1) {
            this.timedEvents.remove(eventId);
            return;
        }
        this.timedEvents.put(eventId, remaining - 1);
    }

    public Optional<UUID> timeBombSlot() {
        return Optional.ofNullable(this.timeBombSlot);
    }

    public void setTimeBombSlot(@Nullable UUID slotId) {
        this.timeBombSlot = slotId;
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
        this.timeBombSlot = null;
        this.soulLinks.clear();
        this.lotteryNumbers.clear();
        this.timedEvents.clear();
        this.lotteryJackpot = 10;
    }

    public enum MarkerResult {
        ADDED,
        REMOVED,
        FULL,
        INVALID
    }

    public enum BoardTrapType implements StringRepresentable {
        ENTRAPMENT("entrapment", false, false),
        DEMOLITION("demolition", false, false),
        BARRICADE("barricade", true, true),
        ENHANCED_BARRICADE("enhanced_barricade", true, true);

        public static final Codec<BoardTrapType> CODEC = StringRepresentable.fromEnum(BoardTrapType::values);

        private final String serializedName;
        private final boolean barricade;
        private final boolean singlePerNode;

        BoardTrapType(String serializedName, boolean barricade, boolean singlePerNode) {
            this.serializedName = serializedName;
            this.barricade = barricade;
            this.singlePerNode = singlePerNode;
        }

        @Override
        public @NonNull String getSerializedName() {
            return this.serializedName;
        }

        public boolean barricade() {
            return this.barricade;
        }

        public boolean singlePerNode() {
            return this.singlePerNode;
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
                           Map<String, Integer> droppedCoins, @Nullable UUID timeBombSlot,
                           List<BoardSoulLink> soulLinks, Map<String, List<Integer>> lotteryNumbers,
                           int lotteryJackpot, Map<Identifier, Integer> timedEvents) {
        public static final Snapshot EMPTY = new Snapshot(List.of(), List.of(), Map.of(), null,
                List.of(), Map.of(), 10, Map.of());
        public static final Codec<Snapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.listOf().optionalFieldOf("character_start_nodes", List.of())
                        .forGetter(Snapshot::characterStartNodes),
                BoardTrap.CODEC.listOf().optionalFieldOf("traps", List.of()).forGetter(Snapshot::traps),
                Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("dropped_coins", Map.of())
                        .forGetter(Snapshot::droppedCoins),
                UUID_CODEC.optionalFieldOf("time_bomb_slot").forGetter(snapshot -> Optional.ofNullable(snapshot.timeBombSlot())),
                BoardSoulLink.CODEC.listOf().optionalFieldOf("soul_links", List.of()).forGetter(Snapshot::soulLinks),
                Codec.unboundedMap(Codec.STRING, Codec.INT.listOf()).optionalFieldOf("lottery_numbers", Map.of())
                        .forGetter(Snapshot::lotteryNumbers),
                Codec.INT.optionalFieldOf("lottery_jackpot", 10).forGetter(Snapshot::lotteryJackpot),
                Codec.unboundedMap(Identifier.CODEC, Codec.INT)
                        .optionalFieldOf("timed_events", Map.of()).forGetter(Snapshot::timedEvents)
        ).apply(instance, (characterStartNodes, traps, droppedCoins, timeBombSlot, soulLinks, lotteryNumbers,
                           lotteryJackpot, timedEvents) -> new Snapshot(characterStartNodes, traps, droppedCoins,
                timeBombSlot.orElse(null), soulLinks, lotteryNumbers, lotteryJackpot, timedEvents)));

        public Snapshot {
            characterStartNodes = List.copyOf(characterStartNodes);
            traps = List.copyOf(traps);
            droppedCoins = Map.copyOf(droppedCoins);
            soulLinks = List.copyOf(soulLinks);
            Map<String, List<Integer>> copied = new LinkedHashMap<>();
            lotteryNumbers.forEach((slotId, numbers) -> copied.put(slotId, List.copyOf(numbers)));
            lotteryNumbers = Map.copyOf(copied);
            lotteryJackpot = Math.max(10, lotteryJackpot);
            timedEvents = Map.copyOf(timedEvents == null ? Map.of() : timedEvents);
        }
    }

}