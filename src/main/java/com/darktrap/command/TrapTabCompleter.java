package com.darktrap.command;

import com.darktrap.DarkTrapPlugin;
import com.darktrap.manager.TrapManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides tab-completion for /darktrap give <player> <trap> and /darktrap create <name>.
 */
public final class TrapTabCompleter implements TabCompleter {

    private final TrapManager manager;

    public TrapTabCompleter(DarkTrapPlugin plugin) {
        this.manager = plugin.getTrapManager();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();

        if (args.length == 1) {
            result.add("give");
            result.add("create");
            return filter(result, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                result.add(player.getName());
            }
            return filter(result, args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            result.addAll(manager.getTrapConfigs().keySet());
            return filter(result, args[2]);
        }

        return result;
    }

    private List<String> filter(List<String> options, String input) {
        String lower = input.toLowerCase();
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }
}
