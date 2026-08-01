package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.board.BoardEventTargets;
import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record BoardHandEventEffect(Action action, Optional<Holder<Item>> item, int count) implements AstralEventEffect {

    public static final MapCodec<BoardHandEventEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Action.CODEC.fieldOf("action").forGetter(BoardHandEventEffect::action),
            Item.CODEC.optionalFieldOf("item").forGetter(BoardHandEventEffect::item),
            Codec.INT.optionalFieldOf("count", 1).forGetter(BoardHandEventEffect::count)
    ).apply(instance, BoardHandEventEffect::new));

    public BoardHandEventEffect(Action action, int count) {
        this(action, Optional.empty(), count);
    }

    public BoardHandEventEffect(Holder<Item> item, int count) {
        this(Action.GIVE_FIXED, Optional.of(item), count);
    }

    @Override
    public String typeId() {
        return AstralCraft.prefix("board_hand").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void apply(AstralEventContext context) {
        BoardEventTargets.resolve(context).ifPresent(target -> {
            BoardParticipant participant = target.participant();
            int safeCount = Math.clamp(this.count, 0, 64);
            if (safeCount <= 0) return;
            List<Identifier> hand = new ArrayList<>(participant.hand());
            switch (this.action) {
                case DISCARD_RANDOM -> {
                    for (int index = 0; index < safeCount && !hand.isEmpty(); index++) {
                        hand.remove(target.level().getRandom().nextInt(hand.size()));
                    }
                }

                case GIVE_RANDOM -> {
                    for (int index = 0; index < safeCount; index++) {
                        BoardSessionManager.randomPvpCardId(target.level()).ifPresent(hand::add);
                    }
                }

                case GIVE_FIXED -> this.item.map(Holder::value).map(BuiltInRegistries.ITEM::getKey).ifPresent(cardId -> {
                    for (int index = 0; index < safeCount; index++) hand.add(cardId);
                });
            }

            BoardSessionManager.updateParticipant(target.level(), target.session(), participant.withHand(hand));
        });
    }

    public enum Action implements StringRepresentable {

        GIVE_FIXED("give_fixed"),
        GIVE_RANDOM("give_random"),
        DISCARD_RANDOM("discard_random");

        public static final Codec<Action> CODEC = StringRepresentable.fromEnum(Action::values);
        private final String name;

        Action(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

    }

}