package com.astral_craft.common.gameplay;

/** Visual parameters for the Soul Link beam. Color is ARGB. */
public record SoulLinkStyle(float arcHeight, float thickness, int color, boolean rainbow) {

    public static final SoulLinkStyle DEFAULT = new SoulLinkStyle(2.1F, 0.05F, 0xFF8F55FF, false);

    public static SoulLinkStyle rainbow(float arcHeight, float thickness) {
        return new SoulLinkStyle(arcHeight, Math.max(0.20F, thickness), 0xFFFFFFFF, true);
    }

}