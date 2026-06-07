package com.astral_craft.client.model;

import com.astral_craft.AstralCraft;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.model.StandardModelParameters;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;

public final class LargeCuboidModelLoader implements UnbakedModelLoader<LargeCuboidUnbakedModel> {

    public static final Identifier ID = AstralCraft.prefix("large_cuboid");
    public static final LargeCuboidModelLoader INSTANCE = new LargeCuboidModelLoader();

    @Override
    public LargeCuboidUnbakedModel read(JsonObject json, JsonDeserializationContext context) throws JsonParseException {
        StandardModelParameters parameters = StandardModelParameters.parse(json, context);
        LargeCuboidGeometry geometry = null;
        if (json.has("elements")) {
            geometry = new LargeCuboidGeometry(LargeModelParser.readElements(json));
        }

        // When elements is absent, geometry() returns null so ResolvedModel climbs to the parent model,
        // matching vanilla parent inheritance rules for geometry.
        return new LargeCuboidUnbakedModel(parameters, geometry);
    }

}