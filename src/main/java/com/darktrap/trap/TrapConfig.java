package com.darktrap.trap;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Holds every configurable value of a single trap type, loaded from
 * plugins/DarkTrap/Traps/<id>/config.yml
 * <p>
 * New trap types can be added purely by creating a new folder + config.yml,
 * no Java code changes required.
 */
public final class TrapConfig {

    private final String id;
    private String displayName;

    private Material itemMaterial;
    private String itemDisplayName;
    private List<String> itemLore;

    private int durationSeconds;
    private long activationDelayTicks;
    private long cooldownSeconds;

    private int radius;
    private int height;

    private Material wallMaterial;
    private Material floorMaterial;
    private Material roofMaterial;

    private final List<EffectData> effects = new ArrayList<>();
    private final Map<String, String> messageOverrides = new HashMap<>();

    private TrapConfig(String id) {
        this.id = id;
    }

    public static TrapConfig load(File folder, Logger logger) {
        File file = new File(folder, "config.yml");
        if (!file.exists()) {
            logger.warning("[DarkTrap] Skipped '" + folder.getName() + "' - missing config.yml");
            return null;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        TrapConfig config = new TrapConfig(folder.getName().toLowerCase());

        config.displayName = yaml.getString("name", "&f" + config.id);

        config.itemMaterial = matchMaterial(yaml.getString("item.material", "STONE"), Material.STONE, logger, folder.getName());
        config.itemDisplayName = yaml.getString("item.name", config.displayName);
        config.itemLore = yaml.getStringList("item.lore");

        config.durationSeconds = Math.max(1, yaml.getInt("duration", 10));
        config.activationDelayTicks = Math.max(0, yaml.getLong("activation-delay", 0));
        config.cooldownSeconds = Math.max(0, yaml.getLong("cooldown", 10));

        config.radius = Math.max(1, yaml.getInt("size.radius", 2));
        config.height = Math.max(1, yaml.getInt("size.height", 3));

        config.wallMaterial = matchMaterial(yaml.getString("blocks.wall", "STONE"), Material.STONE, logger, folder.getName());
        config.floorMaterial = matchMaterial(yaml.getString("blocks.floor", "STONE"), Material.STONE, logger, folder.getName());
        config.roofMaterial = matchMaterial(yaml.getString("blocks.roof", "STONE"), Material.STONE, logger, folder.getName());

        List<Map<?, ?>> effectList = yaml.getMapList("effects");
        for (Map<?, ?> raw : effectList) {
            Object typeObj = raw.get("type");
            if (typeObj == null) {
                continue;
            }
            String typeName = String.valueOf(typeObj);
            int amplifier = raw.get("amplifier") instanceof Number n ? n.intValue() : 0;
            int duration = raw.get("duration") instanceof Number n ? n.intValue() : 40;
            EffectData effectData = EffectData.of(typeName, amplifier, duration);
            if (effectData != null) {
                config.effects.add(effectData);
            } else {
                logger.warning("[DarkTrap] Unknown potion effect type '" + typeName + "' in trap '" + folder.getName() + "'");
            }
        }

        if (yaml.isConfigurationSection("messages")) {
            for (String key : yaml.getConfigurationSection("messages").getKeys(false)) {
                config.messageOverrides.put(key, yaml.getString("messages." + key));
            }
        }

        return config;
    }

    /**
     * Writes a brand new trap config template to disk for the "/darktrap create" command.
     */
    public static void writeTemplate(File folder, String rawName, Plugin plugin) throws java.io.IOException {
        if (!folder.exists() && !folder.mkdirs()) {
            throw new java.io.IOException("Could not create directory: " + folder.getAbsolutePath());
        }
        File file = new File(folder, "config.yml");
        YamlConfiguration yaml = new YamlConfiguration();

        yaml.set("name", "&f" + rawName);
        yaml.set("item.material", "STONE");
        yaml.set("item.name", "&f" + rawName);
        yaml.set("item.lore", List.of("&7ПКМ чтобы установить"));
        yaml.set("duration", 10);
        yaml.set("activation-delay", 0);
        yaml.set("cooldown", 15);
        yaml.set("size.radius", 2);
        yaml.set("size.height", 3);
        yaml.set("blocks.wall", "STONE");
        yaml.set("blocks.floor", "STONE");
        yaml.set("blocks.roof", "STONE");

        Map<String, Object> effect = new HashMap<>();
        effect.put("type", "SLOWNESS");
        effect.put("amplifier", 1);
        effect.put("duration", 40);
        yaml.set("effects", List.of(effect));

        yaml.createSection("messages");

        yaml.save(file);
    }

    private static Material matchMaterial(String name, Material fallback, Logger logger, String trapName) {
        if (name == null) {
            return fallback;
        }
        Material material = Material.matchMaterial(name.trim().toUpperCase());
        if (material == null) {
            logger.warning("[DarkTrap] Unknown material '" + name + "' in trap '" + trapName + "', using " + fallback);
            return fallback;
        }
        return material;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getItemMaterial() {
        return itemMaterial;
    }

    public String getItemDisplayName() {
        return itemDisplayName;
    }

    public List<String> getItemLore() {
        return itemLore;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public long getActivationDelayTicks() {
        return activationDelayTicks;
    }

    public long getCooldownSeconds() {
        return cooldownSeconds;
    }

    public int getRadius() {
        return radius;
    }

    public int getHeight() {
        return height;
    }

    public Material getWallMaterial() {
        return wallMaterial;
    }

    public Material getFloorMaterial() {
        return floorMaterial;
    }

    public Material getRoofMaterial() {
        return roofMaterial;
    }

    public List<EffectData> getEffects() {
        return effects;
    }

    public String getMessageOverride(String key) {
        return messageOverrides.get(key);
    }
}
