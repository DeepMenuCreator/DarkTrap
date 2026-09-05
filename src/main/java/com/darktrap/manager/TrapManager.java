package com.darktrap.manager;

import com.darktrap.DarkTrapPlugin;
import com.darktrap.trap.Trap;
import com.darktrap.trap.TrapConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central manager for everything DarkTrap: loads the main config, discovers
 * trap types from the Traps/ folder, tracks active trap instances and
 * per-player cooldowns.
 */
public final class TrapManager {

    private final DarkTrapPlugin plugin;

    private final File trapsFolder;

    private final Map<String, TrapConfig> trapConfigs = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final CopyOnWriteArrayList<Trap> activeTraps = new CopyOnWriteArrayList<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    private final Set<String> disabledWorlds = new HashSet<>();
    private final Set<Material> protectedBlocks = new HashSet<>();
    private final Map<String, String> messages = new HashMap<>();

    public TrapManager(DarkTrapPlugin plugin) {
        this.plugin = plugin;
        this.trapsFolder = new File(plugin.getDataFolder(), "Traps");
    }

    public void loadAll() {
        loadMainConfig();
        ensureDefaultTraps();
        loadTrapConfigs();
    }

    private void loadMainConfig() {
        plugin.reloadConfig();

        disabledWorlds.clear();
        disabledWorlds.addAll(plugin.getConfig().getStringList("disabled-worlds"));

        protectedBlocks.clear();
        for (String name : plugin.getConfig().getStringList("protected-blocks")) {
            Material material = Material.matchMaterial(name.trim().toUpperCase());
            if (material != null) {
                protectedBlocks.add(material);
            } else {
                plugin.getLogger().warning("[DarkTrap] Unknown protected-block material: " + name);
            }
        }

        messages.clear();
        if (plugin.getConfig().isConfigurationSection("messages")) {
            for (String key : plugin.getConfig().getConfigurationSection("messages").getKeys(false)) {
                messages.put(key, plugin.getConfig().getString("messages." + key));
            }
        }
    }

    private void ensureDefaultTraps() {
        if (trapsFolder.exists()) {
            return;
        }
        if (!trapsFolder.mkdirs()) {
            plugin.getLogger().warning("[DarkTrap] Could not create Traps folder!");
            return;
        }
        copyBundledTrap("traps/default.yml", "default");
        copyBundledTrap("traps/obsidian.yml", "obsidian");
    }

    private void copyBundledTrap(String resourcePath, String trapName) {
        File destinationFolder = new File(trapsFolder, trapName);
        File destinationFile = new File(destinationFolder, "config.yml");
        if (!destinationFolder.mkdirs()) {
            plugin.getLogger().warning("[DarkTrap] Could not create folder for trap: " + trapName);
            return;
        }
        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in == null) {
                plugin.getLogger().warning("[DarkTrap] Bundled resource not found: " + resourcePath);
                return;
            }
            Files.copy(in, destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            plugin.getLogger().warning("[DarkTrap] Failed to copy bundled trap '" + trapName + "': " + exception.getMessage());
        }
    }

    private void loadTrapConfigs() {
        trapConfigs.clear();
        File[] folders = trapsFolder.listFiles(File::isDirectory);
        if (folders == null) {
            return;
        }
        for (File folder : folders) {
            TrapConfig config = TrapConfig.load(folder, plugin.getLogger());
            if (config != null) {
                trapConfigs.put(config.getId(), config);
            }
        }
    }

    /**
     * Creates a brand new trap type folder + config.yml. Returns false if it already exists.
     */
    public boolean createTrap(String rawName) {
        String id = rawName.toLowerCase();
        if (trapConfigs.containsKey(id)) {
            return false;
        }
        File folder = new File(trapsFolder, id);
        try {
            TrapConfig.writeTemplate(folder, rawName, plugin);
        } catch (IOException exception) {
            plugin.getLogger().warning("[DarkTrap] Failed to create trap '" + rawName + "': " + exception.getMessage());
            return false;
        }
        TrapConfig config = TrapConfig.load(folder, plugin.getLogger());
        if (config != null) {
            trapConfigs.put(config.getId(), config);
        }
        return true;
    }

    /**
     * Gives a trap item to a player. Returns false if the trap id is unknown.
     */
    public boolean giveTrap(Player player, String trapId) {
        TrapConfig config = trapConfigs.get(trapId);
        if (config == null) {
            return false;
        }
        ItemStack item = com.darktrap.util.ItemUtil.createTrapItem(config);
        player.getInventory().addItem(item);
        return true;
    }

    public boolean isWorldDisabled(String worldName) {
        return disabledWorlds.contains(worldName);
    }

    public boolean isProtectedMaterial(Material material) {
        return protectedBlocks.contains(material);
    }

    public String getMessage(String key) {
        return messages.getOrDefault(key, "");
    }

    public Map<String, TrapConfig> getTrapConfigs() {
        return trapConfigs;
    }

    public TrapConfig getTrapConfig(String id) {
        return trapConfigs.get(id);
    }

    // ----- Active trap tracking -----

    public void addActiveTrap(Trap trap) {
        activeTraps.add(trap);
    }

    public void removeActiveTrap(Trap trap) {
        activeTraps.remove(trap);
    }

    public boolean isLocationUsedByActiveTrap(Location location) {
        for (Trap trap : activeTraps) {
            if (trap.containsLocation(location)) {
                return true;
            }
        }
        return false;
    }

    public Trap findTrapContaining(Location location) {
        for (Trap trap : activeTraps) {
            if (trap.containsLocation(location)) {
                return trap;
            }
        }
        return null;
    }

    // ----- Cooldowns -----

    public boolean isOnCooldown(Player player, String trapId) {
        return getRemainingCooldownSeconds(player, trapId) > 0;
    }

    public long getRemainingCooldownSeconds(Player player, String trapId) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) {
            return 0;
        }
        Long expiresAt = playerCooldowns.get(trapId);
        if (expiresAt == null) {
            return 0;
        }
        long remainingMillis = expiresAt - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            return 0;
        }
        return (remainingMillis / 1000L) + 1;
    }

    public void setCooldown(Player player, String trapId, long seconds) {
        if (seconds <= 0) {
            return;
        }
        cooldowns.computeIfAbsent(player.getUniqueId(), key -> new ConcurrentHashMap<>())
                .put(trapId, System.currentTimeMillis() + (seconds * 1000L));
    }

    /**
     * Called on plugin disable / reload: immediately restores every active
     * trap's blocks so the world is never left in a modified state.
     */
    public void shutdown() {
        for (Trap trap : activeTraps) {
            trap.restore();
        }
        activeTraps.clear();
    }
}
