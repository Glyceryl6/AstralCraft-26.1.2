package com.astral_craft.common.gameplay.chip;

public enum ChipPool {
    GENERAL,
    SUPPORT,
    SUSTAIN,
    ATTACK,
    CARDS;

    public record Weights(int support, int sustain, int attack, int cards) {

        public static final int DEFAULT_WEIGHT = 15;
        public static final Weights DEFAULT = new Weights(DEFAULT_WEIGHT, DEFAULT_WEIGHT, DEFAULT_WEIGHT, DEFAULT_WEIGHT);

        public Weights {
            support = Math.max(1, support);
            sustain = Math.max(1, sustain);
            attack = Math.max(1, attack);
            cards = Math.max(1, cards);
        }

        public int weight(ChipPool pool) {
            return switch (pool) {
                case SUPPORT -> this.support;
                case SUSTAIN -> this.sustain;
                case ATTACK -> this.attack;
                case CARDS -> this.cards;
                case GENERAL -> DEFAULT_WEIGHT;
            };
        }
    }

}