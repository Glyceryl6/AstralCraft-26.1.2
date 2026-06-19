package com.astral_craft.client.model.character;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public record AstralGeoTransform(float x, float y, float z) {

    public static final AstralGeoTransform ZERO = new AstralGeoTransform(0.0F, 0.0F, 0.0F);
    public static final AstralGeoTransform ONE = new AstralGeoTransform(1.0F, 1.0F, 1.0F);

    public static AstralGeoTransform read(JsonElement element, AstralGeoTransform fallback) {
        if (element == null || element.isJsonNull()) return fallback;
        if (element instanceof JsonPrimitive primitive && primitive.isNumber()) {
            float value = primitive.getAsFloat();
            return new AstralGeoTransform(value, value, value);
        }

        if (!element.isJsonArray()) return fallback;
        JsonArray array = element.getAsJsonArray();
        float x = !array.isEmpty() && array.get(0).isJsonPrimitive() ? array.get(0).getAsFloat() : fallback.x();
        float y = array.size() > 1 && array.get(1).isJsonPrimitive() ? array.get(1).getAsFloat() : fallback.y();
        float z = array.size() > 2 && array.get(2).isJsonPrimitive() ? array.get(2).getAsFloat() : fallback.z();
        return new AstralGeoTransform(x, y, z);
    }

    public AstralGeoTransform lerp(AstralGeoTransform other, float t) {
        float value = Math.clamp(t, 0.0F, 1.0F);
        return new AstralGeoTransform(
                this.x + (other.x - this.x) * value,
                this.y + (other.y - this.y) * value,
                this.z + (other.z - this.z) * value);
    }

}