package com.astral_craft.common.entity.projectile;

/** Runtime-tunable projectile parameters. They are also saved on the projectile entity as NBT. */
public record CardProjectileSettings(float speed, float gravity, float homing, float arcBoost, int durationTicks) {

    public static CardProjectileSettings of(float speed, float gravity, float homing, float arcBoost, int durationTicks) {
        return new CardProjectileSettings(speed, gravity, homing, arcBoost, durationTicks);
    }

    public static CardProjectileSettings firecrackers() {
        return of(0.92F, 0.025F, 0.15F, 0.42F, 28);
    }

    public static CardProjectileSettings slingshot() {
        return of(1.15F, 0.018F, 0.10F, 0.08F, 18);
    }

    public static CardProjectileSettings snowballAttack() {
        return of(1.05F, 0.026F, 0.12F, 0.12F, 18);
    }

}