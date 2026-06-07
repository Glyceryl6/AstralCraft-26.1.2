package com.astral_craft.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.math.Quadrant;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.cuboid.CuboidRotation;
import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;
import net.neoforged.neoforge.client.model.ExtraFaceData;
import org.apache.commons.lang3.mutable.MutableObject;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * Reads the normal Java block/item model "elements" array, but deliberately does not enforce
 * vanilla's from/to -16..32 bounds. The rest of the element syntax is kept vanilla-compatible.
 */
public final class LargeModelParser {

    public static List<CuboidModelElement> readElements(JsonObject root) {
        JsonArray array = GsonHelper.getAsJsonArray(root, "elements");
        List<CuboidModelElement> elements = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                throw new JsonParseException("Model element must be an object");
            }

            elements.add(readElement(element.getAsJsonObject()));
        }

        return elements;
    }

    private static CuboidModelElement readElement(JsonObject object) {
        Vector3f from = readVector3f(object, "from");
        Vector3f to = readVector3f(object, "to");
        CuboidRotation rotation = readRotation(object);
        boolean shade = GsonHelper.getAsBoolean(object, "shade", true);
        int lightEmission = GsonHelper.getAsInt(object, "light_emission", 0);
        if (lightEmission < 0 || lightEmission > 15) {
            throw new JsonParseException("light_emission must be between 0 and 15");
        }

        Map<Direction, CuboidFace> faces = readFaces(object);
        ExtraFaceData faceData = ExtraFaceData.read(object.get("neoforge_data"), ExtraFaceData.DEFAULT);

        // This constructor path does not perform the vanilla -16..32 extent check; that check lives in
        // the vanilla JSON deserializer. We keep all other data in the vanilla CuboidModelElement type,
        // so FaceBakery and NeoForge extra face data continue to work.
        return new CuboidModelElement(from, to, faces, rotation, shade, lightEmission, faceData);
    }

    private static Map<Direction, CuboidFace> readFaces(JsonObject elementObject) {
        JsonObject facesObject = GsonHelper.getAsJsonObject(elementObject, "faces");
        Map<Direction, CuboidFace> faces = new EnumMap<>(Direction.class);
        for (Map.Entry<String, JsonElement> entry : facesObject.entrySet()) {
            Direction side = readDirection(entry.getKey(), "face key");
            if (!entry.getValue().isJsonObject()) {
                throw new JsonParseException("Face '" + entry.getKey() + "' must be an object");
            }

            faces.put(side, readFace(entry.getValue().getAsJsonObject()));
        }

        if (faces.isEmpty()) {
            throw new JsonParseException("Expected at least one face in model element");
        }

        return faces;
    }

    private static CuboidFace readFace(JsonObject object) {
        Direction cullFace = null;
        if (object.has("cullface")) {
            cullFace = readDirection(GsonHelper.getAsString(object, "cullface"), "cullface");
        }

        int tintIndex = GsonHelper.getAsInt(object, "tintindex", CuboidFace.NO_TINT);
        String texture = GsonHelper.getAsString(object, "texture");
        CuboidFace.UVs uvs = object.has("uv") ? readUvs(object) : null;
        Quadrant uvRotation = readQuadrant(GsonHelper.getAsInt(object, "rotation", 0));
        ExtraFaceData faceData = ExtraFaceData.read(object.get("neoforge_data"), null);
        return new CuboidFace(cullFace, tintIndex, texture, uvs, uvRotation, faceData, new MutableObject<>());
    }

    private static CuboidFace.UVs readUvs(JsonObject object) {
        JsonArray uv = GsonHelper.getAsJsonArray(object, "uv");
        if (uv.size() != 4) {
            throw new JsonParseException("Expected 4 uv values");
        }

        return new CuboidFace.UVs(
                GsonHelper.convertToFloat(uv.get(0), "uv[0]"),
                GsonHelper.convertToFloat(uv.get(1), "uv[1]"),
                GsonHelper.convertToFloat(uv.get(2), "uv[2]"),
                GsonHelper.convertToFloat(uv.get(3), "uv[3]"));
    }

    @Nullable
    private static CuboidRotation readRotation(JsonObject elementObject) {
        if (!elementObject.has("rotation")) return null;
        JsonObject rotationObject = GsonHelper.getAsJsonObject(elementObject, "rotation");
        Vector3f origin = readVector3f(rotationObject, "origin");
        Direction.Axis axis = readAxis(GsonHelper.getAsString(rotationObject, "axis"));
        float angle = GsonHelper.getAsFloat(rotationObject, "angle");
        boolean rescale = GsonHelper.getAsBoolean(rotationObject, "rescale", false);
        validateVanillaElementRotation(angle);
        return new CuboidRotation(origin, new CuboidRotation.SingleAxisRotation(axis, angle), rescale);
    }

    private static Vector3f readVector3f(JsonObject object, String key) {
        JsonArray array = GsonHelper.getAsJsonArray(object, key);
        if (array.size() != 3) {
            throw new JsonParseException("Expected 3 values for '" + key + "'");
        }

        return new Vector3f(
                GsonHelper.convertToFloat(array.get(0), key + "[0]"),
                GsonHelper.convertToFloat(array.get(1), key + "[1]"),
                GsonHelper.convertToFloat(array.get(2), key + "[2]"));
    }

    private static Direction readDirection(String name, String fieldName) {
        Direction direction = Direction.byName(name.toLowerCase(Locale.ROOT));
        if (direction == null) {
            throw new JsonParseException("Invalid " + fieldName + ": " + name);
        }

        return direction;
    }

    private static Direction.Axis readAxis(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "x" -> Direction.Axis.X;
            case "y" -> Direction.Axis.Y;
            case "z" -> Direction.Axis.Z;
            default -> throw new JsonParseException("Invalid rotation axis: " + name);
        };
    }

    private static Quadrant readQuadrant(int degrees) {
        return switch (Math.floorMod(degrees, 360)) {
            case 0 -> Quadrant.R0;
            case 90 -> Quadrant.R90;
            case 180 -> Quadrant.R180;
            case 270 -> Quadrant.R270;
            default -> throw new JsonParseException("Invalid face rotation " + degrees + "; expected 0, 90, 180, or 270");
        };
    }

    private static void validateVanillaElementRotation(float angle) {
        if (angle != -45.0F && angle != -22.5F && angle != 0.0F && angle != 22.5F && angle != 45.0F) {
            throw new JsonParseException("Invalid element rotation " + angle + "; expected -45, -22.5, 0, 22.5, or 45");
        }
    }

}