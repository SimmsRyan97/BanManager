package com.pvptoggle;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PvPCommand implements CommandExecutor, TabCompleter {

    private final PvPTogglePlugin plugin;
    private final PvPService pvpService;

    public PvPCommand(PvPTogglePlugin plugin, PvPService pvpService) {
        this.plugin = plugin;
        this.pvpService = pvpService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(plugin, sender, "players-only");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("status")) {
            sendStatus(player);
            return true;
        }

        long remaining = pvpService.getRemainingCooldownSeconds(player.getUniqueId());
        if (remaining > 0) {
            MessageUtil.send(plugin, player, "cooldown", Map.of("seconds", String.valueOf(remaining)));
            return true;
        }

        boolean enabled = pvpService.toggle(player.getUniqueId());
        if (enabled) {
            MessageUtil.send(plugin, player, "toggled-on");
        } else {
            MessageUtil.send(plugin, player, "toggled-off");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && "status".startsWith(args[0].toLowerCase())) {
            List<String> completions = new ArrayList<>();
            completions.add("status");
            return completions;
        }
        return List.of();
    }

    private void sendStatus(Player player) {
        if (pvpService.isPvpEnabled(player.getUniqueId())) {
            MessageUtil.send(plugin, player, "status-on");
        } else {
            MessageUtil.send(plugin, player, "status-off");
        }
    }
}
