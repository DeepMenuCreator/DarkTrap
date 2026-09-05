package com.darktrap.util;

import com.darktrap.DarkTrapPlugin;
import com.darktrap.trap.TrapConfig;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles creation of trap items and reading trap identity back from an ItemStack
 * via the item's PersistentDataContainer.
 */
public final class ItemUtil {

    private static final String KEY_NAME = "trap_id";

    private ItemUtil() {
    }

    private static NamespacedKey key() {
        return new NamespacedKey(DarkTrapPlugin.getInstance(), KEY_NAME);
    }

    /**
     * Builds a physical item representing the given trap type.
     */
    public static ItemStack createTrapItem(TrapConfig config) {
        ItemStack item = new ItemStack(config.getItemMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtil.colorize(config.getItemDisplayName()));

            List<String> lore = new ArrayList<>();
            for (String line : config.getItemLore()) {
                lore.add(MessageUtil.colorize(line));
            }
            meta.setLore(lore);

            meta.getPersistentDataContainer().set(key(), PersistentDataType.STRING, config.getId());
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Reads the trap id stored on an item, or null if the item is not a DarkTrap item.
     */
    public static String getTrapId(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(key(), PersistentDataType.STRING);
    }
}
