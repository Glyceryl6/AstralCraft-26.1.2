package com.astral_craft.client.model.character;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public record AstralGeoAnimationSet(Identifier id, Map<String, AstralGeoAnimationClip> clips, JsonObject source) {

    public static final Codec<AstralGeoAnimationSet> CODEC = Codec.PASSTHROUGH.xmap(dynamic -> AstralGeoAnimationSet.read(null, dynamic.convert(JsonOps.INSTANCE).getValue()), value -> new Dynamic<>(JsonOps.INSTANCE, value.source()));

    public static AstralGeoAnimationSet read(Identifier id, JsonElement element) {
        Map<String, AstralGeoAnimationClip> clips = new LinkedHashMap<>();
        JsonObject object = element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
        JsonObject animations = object.has("animations") && object.get("animations").isJsonObject() ? object.getAsJsonObject("animations") : object;
        for (Map.Entry<String, JsonElement> entry : animations.entrySet()) {
            if (entry.getValue().isJsonObject()) {
                String rawName = entry.getKey();
                String shortName = rawName.startsWith("animation.") ? rawName.substring(rawName.lastIndexOf('.') + 1) : rawName;
                AstralGeoAnimationClip clip = AstralGeoAnimationClip.read(rawName, entry.getValue().getAsJsonObject());
                clips.put(rawName, clip);
                clips.putIfAbsent(shortName, clip);
            }
        }
        return new AstralGeoAnimationSet(id, Map.copyOf(clips), object.deepCopy());
    }

    public AstralGeoAnimationClip clip(String action) {
        AstralGeoAnimationClip clip = this.clips.get(action);
        if (clip == null && action != null) {
            clip = this.clips.get("animation." + action);
        }
        return clip == null ? this.clips.get(AstralCharacterAction.IDLE.id()) : clip;
    }

    public List<String> clipNames() {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (String name : this.clips.keySet()) {
            if (name.startsWith("animation.")) {
                names.add(name.substring(name.lastIndexOf('.') + 1));
            } else {
                names.add(name);
            }
        }
        return List.copyOf(names);
    }

}