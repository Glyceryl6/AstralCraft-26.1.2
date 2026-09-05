package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.board.BoardEventContext;
import com.astral_craft.common.gameplay.board.BoardEventTask;
import com.astral_craft.common.gameplay.board.BoardMatchmakingService;
import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

/** Applies nested, existing event effects to selected board pawns in a visible sequence. */
public record BoardForEachParticipantEventEffect(Selection selection, List<AstralEventEffect> effects,
                                                  int intervalTicks, int completionDelayTicks)
        implements BoardEventEffect {

    public static final MapCodec<BoardForEachParticipantEventEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Selection.CODEC.optionalFieldOf("selection", Selection.ALL).forGetter(BoardForEachParticipantEventEffect::selection),
            AstralEventEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(BoardForEachParticipantEventEffect::effects),
            Codec.INT.optionalFieldOf("interval_ticks", 4).forGetter(BoardForEachParticipantEventEffect::intervalTicks),
            Codec.INT.optionalFieldOf("completion_delay_ticks", 6).forGetter(BoardForEachParticipantEventEffect::completionDelayTicks)
    ).apply(instance, BoardForEachParticipantEventEffect::new));

    public BoardForEachParticipantEventEffect(List<AstralEventEffect> effects) {
        this(Selection.ALL, effects, 4, 6);
    }

    public BoardForEachParticipantEventEffect(Selection selection, List<AstralEventEffect> effects) {
        this(selection, effects, 4, 6);
    }

    @Override
    public String typeId() {
        return AstralCraft.prefix("board_for_each_participant").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void enqueue(BoardEventContext context, Deque<BoardEventTask> tasks) {
        List<BoardParticipant> participants = this.select(context);
        for (int index = 0; index < participants.size(); index++) {
            BoardParticipant participant = participants.get(index);
            int wait = index + 1 < participants.size() ? Math.max(0, this.intervalTicks)
                    : Math.max(0, this.completionDelayTicks);
            tasks.addLast(BoardEventTask.action(() -> {
                BoardParticipant current = context.session().participant(participant.slotUuid()).orElse(null);
                if (current == null || this.selection == Selection.ACTIVE && current.knockedDown()) return;
                var eventContext = context.astralContext(current);
                for (AstralEventEffect effect : this.effects) if (effect != null) effect.apply(eventContext);
            }, wait));
        }
    }

    private List<BoardParticipant> select(BoardEventContext context) {
        List<BoardParticipant> participants = new ArrayList<>(context.session().partyParticipants());
        participants.removeIf(participant -> BoardMatchmakingService.tutorialProtected(context.session(), participant));
        if (participants.isEmpty() || this.selection == Selection.ALL) return participants;
        if (this.selection == Selection.ACTIVE) return participants.stream().filter(participant -> !participant.knockedDown()).toList();
        Comparator<BoardParticipant> comparator = Comparator.comparingInt(value -> value.stats().starCoins());
        int target = (this.selection == Selection.RICHEST
                ? participants.stream().max(comparator) : participants.stream().min(comparator))
                .map(value -> value.stats().starCoins()).orElse(0);
        List<BoardParticipant> matching = participants.stream()
                .filter(value -> value.stats().starCoins() == target).toList();
        return matching.isEmpty() ? List.of()
                : List.of(matching.get(context.level().getRandom().nextInt(matching.size())));
    }

    public enum Selection implements StringRepresentable {
        ALL("all"), ACTIVE("active"), RICHEST("richest"), POOREST("poorest");

        public static final Codec<Selection> CODEC = StringRepresentable.fromEnum(Selection::values);
        private final String serializedName;

        Selection(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return this.serializedName;
        }

    }

}