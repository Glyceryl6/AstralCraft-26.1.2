package com.astral_craft.common.gameplay;

/** Visual parameters for the Soul Link beam. Color is ARGB. */
public record SoulLinkStyle(float arcHeight, float thickness, int color, boolean rainbow) {

    public static final SoulLinkStyle DEFAULT = new SoulLinkStyle(5.0F, 0.03F, 0xFF8F55FF, false);

    public static SoulLinkStyle rainbow(float arcHeight, float thickness) {
        return new SoulLinkStyle(arcHeight, thickness, 0xFFFFFFFF, true);
    }

}