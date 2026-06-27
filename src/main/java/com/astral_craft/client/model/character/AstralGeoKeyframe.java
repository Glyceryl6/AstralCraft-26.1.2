package com.astral_craft.client.model.character;

import java.util.List;

public record AstralGeoKeyframe(float time, AstralGeoTransform value, String interpolation) {

    public static AstralGeoTransform sample(List<AstralGeoKeyframe> frames, float time, AstralGeoTransform fallback) {
        if (frames == null || frames.isEmpty()) return fallback;
        if (frames.size() == 1 || time <= frames.getFirst().time()) return frames.getFirst().value();
        for (int i = 1; i < frames.size(); i++) {
            AstralGeoKeyframe previous = frames.get(i - 1);
            AstralGeoKeyframe next = frames.get(i);
            if (time <= next.time()) {
                float length = Math.max(0.0001F, next.time() - previous.time());
                return previous.value().lerp(next.value(), (time - previous.time()) / length);
            }
        }

        return frames.getLast().value();
    }

}