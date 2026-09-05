package com.darktrap.command;

import com.darktrap.DarkTrapPlugin;
import com.darktrap.manager.TrapManager;
import com.darktrap.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles:
 * /darktrap give <player> <trap>
 * /darktrap create <name>
 */
public final class TrapCommand implements CommandExecutor {

    private final DarkTrapPlugin plugin;
    private final TrapManager manager;

    public TrapCommand(DarkTrapPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getTrapManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            MessageUtil.send(sender, manager.getMessage("usage-give"));
            MessageUtil.send(sender, manager.getMessage("usage-create"));
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "give":
                return handleGive(sender, args);
            case "create":
                return handleCreate(sender, args);
            default:
                MessageUtil.send(sender, manager.getMessage("usage-give"));
                MessageUtil.send(sender, manager.getMessage("usage-create"));
                return true;
        }
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "darktrap.give")) {
            MessageUtil.send(sender, manager.getMessage("no-permission"));
            return true;
        }

        if (args.length < 3) {
            MessageUtil.send(sender, manager.getMessage("usage-give"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            MessageUtil.send(sender, manager.getMessage("player-not-found"));
            return true;
        }

        String trapId = args[2].toLowerCase();
        boolean success = manager.giveTrap(target, trapId);
        if (!success) {
            String message = MessageUtil.replace(manager.getMessage("trap-not-found"), "%trap%", trapId);
            MessageUtil.send(sender, message);
            return true;
        }

        String givenMessage = MessageUtil.replace(manager.getMessage("trap-given"), "%trap%", trapId);
        givenMessage = MessageUtil.replace(givenMessage, "%player%", target.getName());
        MessageUtil.send(sender, givenMessage);

        String receivedMessage = MessageUtil.replace(manager.getMessage("trap-received"), "%trap%", trapId);
        MessageUtil.send(target, receivedMessage);
        return true;
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "darktrap.create")) {
            MessageUtil.send(sender, manager.getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            MessageUtil.send(sender, manager.getMessage("usage-create"));
            return true;
        }

        String name = args[1];
        boolean created = manager.createTrap(name);
        if (!created) {
            String message = MessageUtil.replace(manager.getMessage("trap-already-exists"), "%trap%", name);
            MessageUtil.send(sender, message);
            return true;
        }

        String message = MessageUtil.replace(manager.getMessage("trap-created"), "%trap%", name.toLowerCase());
        MessageUtil.send(sender, message);
        return true;
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission(permission) || sender.hasPermission("darktrap.admin");
    }
}
