package com.darktrap.trap;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

/**
 * Captures the state of a block before a trap replaces it, so that the
 * ORIGINAL block (not just air) can be restored once the trap expires.
 */
public final class BlockSnapshot {

    private final World world;
    private final int x;
    private final int y;
    private final int z;
    private final BlockData originalData;

    public BlockSnapshot(World world, int x, int y, int z, BlockData originalData) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.originalData = originalData;
    }

    public boolean matches(Location location) {
        if (location.getWorld() == null || !location.getWorld().equals(world)) {
            return false;
        }
        return location.getBlockX() == x && location.getBlockY() == y && location.getBlockZ() == z;
    }

    public void restore() {
        Location location = new Location(world, x, y, z);
        location.getBlock().setBlockData(originalData, false);
    }

    public World getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }
}
