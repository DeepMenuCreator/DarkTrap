package com.darktrap.listener;

import com.darktrap.DarkTrapPlugin;
import com.darktrap.manager.TrapManager;
import com.darktrap.trap.BlockSnapshot;
import com.darktrap.trap.EffectData;
import com.darktrap.trap.Trap;
import com.darktrap.trap.TrapBuilder;
import com.darktrap.trap.TrapConfig;
import com.darktrap.util.ItemUtil;
import com.darktrap.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;

import java.util.List;

/**
 * Handles trap item activation (right click block / right click air)
 * and protects trap blocks from being broken by players.
 */
public final class TrapListener implements Listener {

    private final DarkTrapPlugin plugin;
    private final TrapManager manager;

    public TrapListener(DarkTrapPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getTrapManager();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Avoid double-firing for main hand + off hand on the same physical click.
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        switch (event.getAction()) {
            case RIGHT_CLICK_BLOCK:
            case RIGHT_CLICK_AIR:
                break;
            default:
                return;
        }

        ItemStack item = event.getItem();
        String trapId = ItemUtil.getTrapId(item);
        if (trapId == null) {
            return;
        }

        // This item is a DarkTrap item: never let vanilla block interaction happen
        // (e.g. opening a chest, using a door) regardless of the block clicked.
        event.setCancelled(true);

        Player player = event.getPlayer();

        if (!player.hasPermission("darktrap.use")) {
            MessageUtil.send(player, manager.getMessage("no-permission"));
            return;
        }

        TrapConfig config = manager.getTrapConfig(trapId);
        if (config == null) {
            // Item refers to a trap type that no longer exists.
            return;
        }

        World world = player.getWorld();

        if (manager.isWorldDisabled(world.getName())) {
            deny(player);
            return;
        }

        if (manager.isOnCooldown(player, trapId)) {
            long remaining = manager.getRemainingCooldownSeconds(player, trapId);
            String message = MessageUtil.replace(manager.getMessage("cooldown-active"), "%seconds%", String.valueOf(remaining));
            MessageUtil.send(player, message);
            return;
        }

        Location base = resolveBaseLocation(event, player, world);
        if (base == null) {
            deny(player);
            return;
        }

        if (!isWithinWorldHeight(world, base, config)) {
            deny(player);
            return;
        }

        TrapBuilder.BuildPlan plan = TrapBuilder.computePlan(base, config.getRadius(), config.getHeight());

        if (!TrapBuilder.canBuild(plan, manager)) {
            deny(player);
            return;
        }

        // All checks passed - consume the item, apply cooldown and schedule the build.
        consumeOne(player);
        manager.setCooldown(player, trapId, config.getCooldownSeconds());
        MessageUtil.send(player, manager.getMessage("trap-place-success"));

        long delay = Math.max(0L, config.getActivationDelayTicks());
        Bukkit.getScheduler().runTaskLater(plugin, () -> buildTrap(config, plan, world, base), delay);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Trap trap = manager.findTrapContaining(event.getBlock().getLocation());
        if (trap == null) {
            return;
        }
        event.setCancelled(true);
        event.setDropItems(false);

        if (plugin.getConfig().getBoolean("settings.notify-block-break-attempt", true)) {
            MessageUtil.send(event.getPlayer(), manager.getMessage("block-break-denied"));
        }
    }

    private void deny(Player player) {
        MessageUtil.sendActionBar(player, manager.getMessage("trap-place-denied"));
    }

    private void consumeOne(Player player) {
        ItemStack handItem = player.getInventory().getItemInMainHand();
        int newAmount = handItem.getAmount() - 1;
        if (newAmount <= 0) {
            player.getInventory().setItemInMainHand(null);
        } else {
            handItem.setAmount(newAmount);
        }
    }

    private Location resolveBaseLocation(PlayerInteractEvent event, Player player, World world) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block clicked = event.getClickedBlock();
            if (clicked != null) {
                return clicked.getLocation();
            }
        }

        int maxDistance = plugin.getConfig().getInt("settings.max-target-distance", 12);
        RayTraceResult result = world.rayTraceBlocks(player.getEyeLocation(), player.getEyeLocation().getDirection(), maxDistance);
        if (result != null && result.getHitBlock() != null) {
            return result.getHitBlock().getLocation();
        }

        // Fallback: the block directly under the player's feet.
        Location feet = player.getLocation().clone().subtract(0, 1, 0);
        Block feetBlock = feet.getBlock();
        if (!feetBlock.getType().isAir()) {
            return feetBlock.getLocation();
        }

        return null;
    }

    private boolean isWithinWorldHeight(World world, Location base, TrapConfig config) {
        int minY = world.getMinHeight() + 1;
        int maxY = world.getMaxHeight() - 1;
        return base.getBlockY() >= minY && (base.getBlockY() + config.getHeight() + 1) <= maxY;
    }

    private void buildTrap(TrapConfig config, TrapBuilder.BuildPlan plan, World world, Location base) {
        // Re-validate right before building in case the area changed during the activation delay.
        if (!TrapBuilder.canBuild(plan, manager)) {
            return;
        }

        List<BlockSnapshot> snapshots = TrapBuilder.build(plan, config);
        Trap trap = new Trap(config, world, base, snapshots);
        manager.addActiveTrap(trap);

        BukkitTask expireTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            trap.restore();
            manager.removeActiveTrap(trap);
        }, config.getDurationSeconds() * 20L);
        trap.setExpireTask(expireTask);

        applyEffects(config, plan, world);
    }

    private void applyEffects(TrapConfig config, TrapBuilder.BuildPlan plan, World world) {
        List<EffectData> effects = config.getEffects();
        if (effects.isEmpty()) {
            return;
        }

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

        for (Location location : plan.all()) {
            minX = Math.min(minX, location.getBlockX());
            maxX = Math.max(maxX, location.getBlockX());
            minY = Math.min(minY, location.getBlockY());
            maxY = Math.max(maxY, location.getBlockY());
            minZ = Math.min(minZ, location.getBlockZ());
            maxZ = Math.max(maxZ, location.getBlockZ());
        }

        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            Location location = entity.getLocation();
            if (location.getBlockX() < minX || location.getBlockX() > maxX) continue;
            if (location.getBlockY() < minY || location.getBlockY() > maxY) continue;
            if (location.getBlockZ() < minZ || location.getBlockZ() > maxZ) continue;

            for (EffectData effect : effects) {
                living.addPotionEffect(effect.toPotionEffect());
            }
        }
    }
}
