package com.astral_craft.client.model.character;

public record AstralGeoPose(AstralGeoTransform rotation, AstralGeoTransform position, AstralGeoTransform scale) {

    public static final AstralGeoPose IDENTITY = new AstralGeoPose(AstralGeoTransform.ZERO, AstralGeoTransform.ZERO, AstralGeoTransform.ONE);

}
