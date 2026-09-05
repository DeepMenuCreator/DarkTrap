package com.darktrap;

import com.darktrap.command.TrapCommand;
import com.darktrap.command.TrapTabCompleter;
import com.darktrap.listener.TrapListener;
import com.darktrap.manager.TrapManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * DarkTrap - custom PvP trap system.
 * Entry point of the plugin.
 */
public final class DarkTrapPlugin extends JavaPlugin {

    private static DarkTrapPlugin instance;

    private TrapManager trapManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        this.trapManager = new TrapManager(this);
        this.trapManager.loadAll();

        getServer().getPluginManager().registerEvents(new TrapListener(this), this);

        PluginCommand command = getCommand("darktrap");
        if (command != null) {
            TrapCommand executor = new TrapCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(new TrapTabCompleter(this));
        } else {
            getLogger().warning("Command 'darktrap' is not registered in plugin.yml!");
        }

        getLogger().info("DarkTrap enabled. Loaded " + trapManager.getTrapConfigs().size() + " trap type(s).");
    }

    @Override
    public void onDisable() {
        if (trapManager != null) {
            trapManager.shutdown();
        }
        getLogger().info("DarkTrap disabled. All active traps have been safely removed.");
    }

    public static DarkTrapPlugin getInstance() {
        return instance;
    }

    public TrapManager getTrapManager() {
        return trapManager;
    }
}
