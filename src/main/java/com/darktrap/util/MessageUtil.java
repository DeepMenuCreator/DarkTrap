package com.darktrap.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Helper methods for colorizing and sending messages / action bars.
 */
public final class MessageUtil {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private MessageUtil() {
    }

    /**
     * Translates '&' color codes into actual Minecraft color codes.
     */
    public static String colorize(String message) {
        if (message == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Sends a colorized chat message to a sender, if the message is not blank.
     */
    public static void send(CommandSender sender, String message) {
        if (sender == null || message == null || message.isEmpty()) {
            return;
        }
        sender.sendMessage(colorize(message));
    }

    /**
     * Sends a colorized message as an ActionBar to a player.
     */
    public static void sendActionBar(Player player, String message) {
        if (player == null || message == null || message.isEmpty()) {
            return;
        }
        Component component = LEGACY.deserialize(message);
        player.sendActionBar(component);
    }

    /**
     * Replaces a single placeholder occurrence in a message.
     */
    public static String replace(String message, String placeholder, String value) {
        if (message == null) {
            return "";
        }
        return message.replace(placeholder, value == null ? "" : value);
    }
}
