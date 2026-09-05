package com.darktrap.trap;

import com.darktrap.manager.TrapManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Computes the block positions of a trap structure (floor / walls / roof)
 * and safely builds / validates it without touching protected blocks or
 * blocks belonging to other active traps.
 */
public final class TrapBuilder {

    private TrapBuilder() {
    }

    /**
     * A computed set of positions for one trap instance, split by role
     * so each role can use its own material.
     */
    public static final class BuildPlan {
        private final World world;
        private final Set<Location> floor = new LinkedHashSet<>();
        private final Set<Location> walls = new LinkedHashSet<>();
        private final Set<Location> roof = new LinkedHashSet<>();

        private BuildPlan(World world) {
            this.world = world;
        }

        public Set<Location> all() {
            Set<Location> all = new LinkedHashSet<>();
            all.addAll(floor);
            all.addAll(walls);
            all.addAll(roof);
            return all;
        }

        public int lowestY() {
            return floor.isEmpty() ? 0 : floor.iterator().next().getBlockY();
        }

        public int highestY() {
            return roof.isEmpty() ? 0 : roof.iterator().next().getBlockY();
        }
    }

    /**
     * Builds the geometric plan of a trap centered on the given base block location.
     * The base location becomes the floor level.
     */
    public static BuildPlan computePlan(Location base, int radius, int height) {
        World world = base.getWorld();
        BuildPlan plan = new BuildPlan(world);

        int baseX = base.getBlockX();
        int baseY = base.getBlockY();
        int baseZ = base.getBlockZ();

        // Floor: solid square at baseY
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                plan.floor.add(new Location(world, baseX + dx, baseY, baseZ + dz));
            }
        }

        // Walls: hollow perimeter, from baseY+1 up to baseY+height
        for (int dy = 1; dy <= height; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    boolean edge = (Math.abs(dx) == radius) || (Math.abs(dz) == radius);
                    if (edge) {
                        plan.walls.add(new Location(world, baseX + dx, baseY + dy, baseZ + dz));
                    }
                }
            }
        }

        // Roof: solid square at baseY + height + 1
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                plan.roof.add(new Location(world, baseX + dx, baseY + height + 1, baseZ + dz));
            }
        }

        return plan;
    }

    /**
     * Verifies that every position in the plan is safe to build on:
     * not a protected block, and not already used by another active trap.
     */
    public static boolean canBuild(BuildPlan plan, TrapManager manager) {
        for (Location location : plan.all()) {
            Block block = location.getBlock();
            if (manager.isProtectedMaterial(block.getType())) {
                return false;
            }
            if (manager.isLocationUsedByActiveTrap(location)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Places the blocks of the trap, capturing a snapshot of every block that is
     * changed so it can be restored to its original state later.
     */
    public static List<BlockSnapshot> build(BuildPlan plan, TrapConfig config) {
        List<BlockSnapshot> snapshots = new ArrayList<>();
        placeAll(plan.floor, config.getFloorMaterial(), snapshots);
        placeAll(plan.walls, config.getWallMaterial(), snapshots);
        placeAll(plan.roof, config.getRoofMaterial(), snapshots);
        return snapshots;
    }

    private static void placeAll(Set<Location> locations, Material material, List<BlockSnapshot> snapshots) {
        for (Location location : locations) {
            Block block = location.getBlock();
            BlockData original = block.getBlockData().clone();
            snapshots.add(new BlockSnapshot(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), original));
            block.setType(material, false);
        }
    }
}
