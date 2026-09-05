package com.darktrap.trap;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.UUID;

/**
 * Represents one active, physically-placed trap in the world.
 * Keeps track of exactly which blocks it placed so it can clean up
 * only its own blocks, and never touches blocks it didn't create.
 */
public final class Trap {

    private final UUID id = UUID.randomUUID();
    private final TrapConfig config;
    private final World world;
    private final Location center;
    private final List<BlockSnapshot> snapshots;

    private BukkitTask expireTask;
    private volatile boolean removed = false;

    public Trap(TrapConfig config, World world, Location center, List<BlockSnapshot> snapshots) {
        this.config = config;
        this.world = world;
        this.center = center;
        this.snapshots = snapshots;
    }

    public boolean containsLocation(Location location) {
        for (BlockSnapshot snapshot : snapshots) {
            if (snapshot.matches(location)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Restores every block this trap placed back to its original state.
     * Only ever touches blocks that are part of {@link #snapshots}.
     */
    public synchronized void restore() {
        if (removed) {
            return;
        }
        for (BlockSnapshot snapshot : snapshots) {
            snapshot.restore();
        }
        if (expireTask != null) {
            expireTask.cancel();
        }
        removed = true;
    }

    public UUID getId() {
        return id;
    }

    public TrapConfig getConfig() {
        return config;
    }

    public World getWorld() {
        return world;
    }

    public Location getCenter() {
        return center;
    }

    public List<BlockSnapshot> getSnapshots() {
        return snapshots;
    }

    public boolean isRemoved() {
        return removed;
    }

    public void setExpireTask(BukkitTask expireTask) {
        this.expireTask = expireTask;
    }
}
