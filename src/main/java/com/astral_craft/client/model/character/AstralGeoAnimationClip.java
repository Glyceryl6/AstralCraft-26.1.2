package com.astral_craft.client.model.character;

import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

public record AstralGeoAnimationClip(String name, float lengthSeconds, boolean loop, Map<String, AstralGeoBoneAnimation> bones) {

    public AstralGeoPose sample(String boneName, float timeSeconds) {
        AstralGeoBoneAnimation animation = this.bones.get(boneName);
        if (animation == null) return AstralGeoPose.IDENTITY;
        float safeTime = timeSeconds;
        if (this.loop && this.lengthSeconds > 0.0F) {
            safeTime = safeTime % this.lengthSeconds;
        }
        return animation.sample(safeTime);
    }

    public static AstralGeoAnimationClip read(String name, JsonObject object) {
        float length = object.has("animation_length") ? object.get("animation_length").getAsFloat() : 1.0F;
        boolean loop = object.has("loop") && (object.get("loop").isJsonPrimitive() && (object.get("loop").getAsJsonPrimitive().isBoolean() ? object.get("loop").getAsBoolean() : !"false".equalsIgnoreCase(object.get("loop").getAsString())));
        Map<String, AstralGeoBoneAnimation> bones = new LinkedHashMap<>();
        if (object.has("bones") && object.get("bones").isJsonObject()) {
            for (Map.Entry<String, com.google.gson.JsonElement> entry : object.getAsJsonObject("bones").entrySet()) {
                if (entry.getValue().isJsonObject()) {
                    bones.put(entry.getKey(), AstralGeoBoneAnimation.read(entry.getKey(), entry.getValue().getAsJsonObject()));
                }
            }
        }
        return new AstralGeoAnimationClip(name, length, loop, Map.copyOf(bones));
    }

}
