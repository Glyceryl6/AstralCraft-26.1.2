package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.gameplay.BoardNode;
import com.astral_craft.common.gameplay.board.BoardEventContext;
import com.astral_craft.common.gameplay.board.BoardEventTask;
import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.board.BoardRouteService;
import com.astral_craft.common.gameplay.board.BoardSession;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.StringRepresentable;

import java.util.*;

public record BoardTeleportParticipantsEventEffect(Mode mode) implements BoardEventEffect {

    public static final MapCodec<BoardTeleportParticipantsEventEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Mode.CODEC.fieldOf("mode").forGetter(BoardTeleportParticipantsEventEffect::mode)
    ).apply(instance, BoardTeleportParticipantsEventEffect::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("board_teleport_participants").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void enqueue(BoardEventContext context, Deque<BoardEventTask> tasks) {
        tasks.addLast(BoardEventTask.action(() -> {
            List<BoardParticipant> participants = context.session().participants();
            List<String> destinations = switch (this.mode) {
                case ROTATE_CURRENT -> rotateCurrent(context, participants);
                case CONNECTED_RANDOM -> connectedNodes(context);
                case HOSPITAL -> hospitalNodes(context, participants.size());
            };
            if (destinations.size() < participants.size()) return;
            Map<String, Direction> originalDirections = new LinkedHashMap<>();
            for (BoardParticipant participant : participants) {
                originalDirections.putIfAbsent(participant.currentNodeKey(),
                        BoardRouteService.travelDirection(context.session(), participant));
            }
            for (int index = 0; index < participants.size(); index++) {
                BoardParticipant participant = participants.get(index);
                String destination = destinations.get(index);
                Direction direction = this.mode == Mode.ROTATE_CURRENT
                        ? originalDirections.getOrDefault(destination, BoardRouteService.travelDirection(context.session(), participant))
                        : BoardRouteService.travelDirection(context.session(), participant);
                BoardSessionManager.relocateParticipant(context.level(), context.session(), participant, destination, direction);
            }
        }, 12));
    }

    private static List<String> rotateCurrent(BoardEventContext context, List<BoardParticipant> participants) {
        if (participants.size() < 2) return List.of();
        List<String> nodes = new ArrayList<>(participants.stream().map(BoardParticipant::currentNodeKey).toList());
        Collections.rotate(nodes, 1 + context.level().getRandom().nextInt(nodes.size() - 1));
        return nodes;
    }

    private static List<String> connectedNodes(BoardEventContext context) {
        List<String> starts = new ArrayList<>(context.session().nodes().keySet());
        Collections.shuffle(starts, new Random(context.level().getRandom().nextLong()));
        for (String start : starts) {
            Deque<List<String>> queue = new ArrayDeque<>();
            queue.add(List.of(start));
            while (!queue.isEmpty()) {
                List<String> path = queue.removeFirst();
                if (path.size() >= context.session().participants().size()) return path;
                for (String neighbor : neighbors(context.session(), path.getLast())) {
                    if (path.contains(neighbor)) continue;
                    List<String> next = new ArrayList<>(path);
                    next.add(neighbor);
                    queue.addLast(next);
                }
            }
        }

        return List.of();
    }

    private static List<String> hospitalNodes(BoardEventContext context, int count) {
        List<String> hospitals = context.session().nodes().values().stream()
                .filter(node -> BuiltInRegistries.BLOCK.getValue(node.platformId()) instanceof BasePlatform platform
                        && platform.protectsBoardParticipant())
                .map(BoardNode::id).toList();
        if (hospitals.isEmpty()) return List.of();
        List<String> result = new ArrayList<>();
        int start = context.level().getRandom().nextInt(hospitals.size());
        for (int index = 0; index < count; index++) result.add(hospitals.get((start + index) % hospitals.size()));
        return result;
    }

    private static List<String> neighbors(BoardSession session, String nodeId) {
        Set<String> result = new LinkedHashSet<>();
        BoardNode node = session.nodes().get(nodeId);
        if (node != null) result.addAll(node.next());
        session.nodes().values().stream().filter(candidate -> candidate.next().contains(nodeId))
                .map(BoardNode::id).forEach(result::add);
        return List.copyOf(result);
    }

    public enum Mode implements StringRepresentable {

        ROTATE_CURRENT("rotate_current"),
        CONNECTED_RANDOM("connected_random"),
        HOSPITAL("hospital");

        public static final Codec<Mode> CODEC = StringRepresentable.fromEnum(Mode::values);
        private final String serializedName;

        Mode(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return this.serializedName;
        }

    }

}