package com.astral_craft.client.model.character;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public record AstralGeoModelDefinition(Identifier id, String modelIdentifier, List<String> bones, JsonObject source) {

    public static final Codec<AstralGeoModelDefinition> CODEC = Codec.PASSTHROUGH.xmap(dynamic -> AstralGeoModelDefinition.read(null, dynamic.convert(JsonOps.INSTANCE).getValue()), value -> new Dynamic<>(JsonOps.INSTANCE, value.source()));

    public static AstralGeoModelDefinition read(Identifier id, JsonElement element) {
        JsonObject object = element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
        String identifier = id == null ? "astral_craft:unknown" : id.toString();
        List<String> bones = new ArrayList<>();
        JsonObject geometry = firstGeometry(object);
        if (geometry != null) {
            if (geometry.has("description") && geometry.get("description").isJsonObject() && geometry.getAsJsonObject("description").has("identifier")) {
                identifier = geometry.getAsJsonObject("description").get("identifier").getAsString();
            }

            if (geometry.has("bones") && geometry.get("bones").isJsonArray()) {
                JsonArray array = geometry.getAsJsonArray("bones");
                for (JsonElement bone : array) {
                    if (bone.isJsonObject() && bone.getAsJsonObject().has("name")) {
                        bones.add(bone.getAsJsonObject().get("name").getAsString());
                    }
                }
            }
        }

        return new AstralGeoModelDefinition(id, identifier, List.copyOf(bones), object.deepCopy());
    }

    private static JsonObject firstGeometry(JsonObject object) {
        if (object.has("minecraft:geometry") && object.get("minecraft:geometry").isJsonArray()) {
            JsonArray array = object.getAsJsonArray("minecraft:geometry");
            if (!array.isEmpty() && array.get(0).isJsonObject()) return array.get(0).getAsJsonObject();
        }

        for (String key : object.keySet()) {
            if (key.startsWith("geometry.") && object.get(key).isJsonObject()) return object.getAsJsonObject(key);
        }

        return null;
    }

}