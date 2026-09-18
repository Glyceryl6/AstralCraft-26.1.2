package com.astral_craft;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AstralVersion {

    private static final String RESOURCE = "/META-INF/astral_craft.versions.properties";
    private static final Properties PROPERTIES = loadProperties();

    public static String modVersion() {
        return require("mod_version");
    }

    public static String minecraftVersion() {
        return require("minecraft_version");
    }

    public static String neoForgeVersion() {
        return require("neo_version");
    }

    public static String networkProtocol() {
        return require("network_protocol_version");
    }

    private static String require(String key) {
        String value = PROPERTIES.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing AstralCraft version property: " + key);
        return value;
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = AstralVersion.class.getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IllegalStateException("Missing AstralCraft version metadata: " + RESOURCE);
            properties.load(input);
            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read AstralCraft version metadata: " + RESOURCE, exception);
        }
    }

}