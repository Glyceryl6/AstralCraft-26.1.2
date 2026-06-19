package com.astral_craft.client.model.character;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public record AstralGeoBoneAnimation(String boneName, List<AstralGeoKeyframe> rotations, List<AstralGeoKeyframe> positions, List<AstralGeoKeyframe> scales) {

    public AstralGeoPose sample(float time) {
        return new AstralGeoPose(
                AstralGeoKeyframe.sample(this.rotations, time, AstralGeoTransform.ZERO),
                AstralGeoKeyframe.sample(this.positions, time, AstralGeoTransform.ZERO),
                AstralGeoKeyframe.sample(this.scales, time, AstralGeoTransform.ONE));
    }

    public static AstralGeoBoneAnimation read(String boneName, JsonObject object) {
        return new AstralGeoBoneAnimation(
                boneName,
                readChannel(object.get("rotation"), AstralGeoTransform.ZERO),
                readChannel(object.get("position"), AstralGeoTransform.ZERO),
                readChannel(object.get("scale"), AstralGeoTransform.ONE));
    }

    private static List<AstralGeoKeyframe> readChannel(JsonElement element, AstralGeoTransform fallback) {
        List<AstralGeoKeyframe> frames = new ArrayList<>();
        if (element == null || element.isJsonNull()) return frames;
        if (element.isJsonArray() || element.isJsonPrimitive()) {
            frames.add(new AstralGeoKeyframe(0.0F, AstralGeoTransform.read(element, fallback), "linear"));
            return frames;
        }

        if (!element.isJsonObject()) return frames;
        JsonObject object = element.getAsJsonObject();
        if (object.has("vector")) {
            frames.add(new AstralGeoKeyframe(0.0F, AstralGeoTransform.read(object.get("vector"), fallback), interpolation(object)));
            return frames;
        }

        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            try {
                float time = Float.parseFloat(entry.getKey());
                JsonElement value = entry.getValue();
                String interpolation = "linear";
                AstralGeoTransform transform;
                if (value != null && value.isJsonObject()) {
                    JsonObject frameObject = value.getAsJsonObject();
                    transform = AstralGeoTransform.read(frameObject.has("post") ? frameObject.get("post") : frameObject.get("vector"), fallback);
                    interpolation = interpolation(frameObject);
                } else {
                    transform = AstralGeoTransform.read(value, fallback);
                }
                frames.add(new AstralGeoKeyframe(time, transform, interpolation));
            } catch (NumberFormatException ignored) {}
        }

        frames.sort(Comparator.comparingDouble(AstralGeoKeyframe::time));
        return frames;
    }

    private static String interpolation(JsonObject object) {
        return object.has("lerp_mode") ? object.get("lerp_mode").getAsString() : object.has("interpolation") ? object.get("interpolation").getAsString() : "linear";
    }

}