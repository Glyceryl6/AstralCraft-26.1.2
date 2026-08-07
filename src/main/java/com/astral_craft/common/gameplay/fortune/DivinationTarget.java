package com.astral_craft.common.gameplay.fortune;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.board.BoardParticipant;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.function.ToIntFunction;

public enum DivinationTarget {

    HIGHEST_STARS("highest_stars", true, participant -> participant.stats().stars()),
    LOWEST_STARS("lowest_stars", false, participant -> participant.stats().stars()),
    HIGHEST_HEALTH("highest_health", true, participant -> participant.stats().health()),
    LOWEST_HEALTH("lowest_health", false, participant -> participant.stats().health()),
    MOST_COINS("most_coins", true, participant -> participant.stats().starCoins()),
    LEAST_COINS("least_coins", false, participant -> participant.stats().starCoins()),
    MOST_CARDS("most_cards", true, participant -> participant.hand().size()),
    LEAST_CARDS("least_cards", false, participant -> participant.hand().size());

    private final String path;
    private final boolean maximum;
    private final ToIntFunction<BoardParticipant> metric;

    DivinationTarget(String path, boolean maximum, ToIntFunction<BoardParticipant> metric) {
        this.path = path;
        this.maximum = maximum;
        this.metric = metric;
    }

    public String translationKey() {
        return "gui.astral_craft.board.divination.target." + this.path;
    }

    public Identifier texture() {
        return AstralCraft.prefix("textures/gui/cards/divination/" + this.path + ".png");
    }

    public List<BoardParticipant> select(List<BoardParticipant> participants) {
        List<BoardParticipant> eligible = participants.stream().filter(participant -> !participant.knockedDown()).toList();
        if (eligible.isEmpty()) return List.of();
        int targetValue = this.maximum
                ? eligible.stream().mapToInt(this.metric).max().orElse(0)
                : eligible.stream().mapToInt(this.metric).min().orElse(0);
        return eligible.stream().filter(participant -> this.metric.applyAsInt(participant) == targetValue).toList();
    }
}
