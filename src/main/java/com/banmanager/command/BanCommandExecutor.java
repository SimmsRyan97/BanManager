package com.banmanager.command;

import com.banmanager.BanManagerPlugin;
import com.banmanager.service.PlayerRepository;
import com.banmanager.service.PunishmentService;
import com.banmanager.service.RuleService;
import com.banmanager.util.TimeUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class BanCommandExecutor implements CommandExecutor, TabCompleter {

    private final BanManagerPlugin plugin;
    private final PunishmentService punishmentService;
    private final PlayerRepository repository;
    private final RuleService ruleService;

    public BanCommandExecutor(BanManagerPlugin plugin, PunishmentService punishmentService, PlayerRepository repository,
            RuleService ruleService) {
        this.plugin = plugin;
        this.punishmentService = punishmentService;
        this.repository = repository;
        this.ruleService = ruleService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        return switch (name) {
            case "warn" -> handleWarn(sender, args);
            case "kickplayer" -> handleKick(sender, args);
            case "tempban" -> handleTempBan(sender, args);
            case "banplayer" -> handleBan(sender, args);
            case "ipban" -> handleIpBan(sender, args);
            case "unbanplayer" -> handleUnban(sender, args);
            case "bmhistory" -> handleHistory(sender, args);
            case "bmreload" -> handleReload(sender);
            default -> false;
        };
    }

    private boolean handleWarn(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /warn <player> <rule> [reason]");
            return true;
        }

        OfflinePlayer target = resolveTarget(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.getConfig().getString("messages.player-not-found", "Player not found: %player%")
                    .replace("%player%", args[0]));
            return true;
        }

        String rule = args[1];
        String reason = args.length >= 3 ? String.join(" ", slice(args, 2)) : "Rule violation: " + rule;

        Optional<String> error = punishmentService.applyWarning(sender, target, rule, reason);
        if (error.isPresent()) {
            sender.sendMessage(punishmentService.color(error.get()));
            return true;
        }

        sender.sendMessage(punishmentService.color(
                plugin.getConfig().getString("messages.warn-applied", "Warned %player% for %rule%")
                        .replace("%player%", punishmentService.safeTargetName(target))
                        .replace("%rule%", rule)));
        return true;
    }

    private boolean handleKick(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("Usage: /kickplayer <player> [reason]");
            return true;
        }
        OfflinePlayer target = resolveTarget(args[0]);
        if (target == null) {
            sender.sendMessage("Unknown player: " + args[0]);
            return true;
        }

        String reason = args.length >= 2 ? String.join(" ", slice(args, 1)) : "Kicked by staff.";
        punishmentService.kick(sender, target, reason);
        sender.sendMessage("Kicked " + punishmentService.safeTargetName(target));
        return true;
    }

    private boolean handleTempBan(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /tempban <player> <duration> [reason]");
            return true;
        }
        OfflinePlayer target = resolveTarget(args[0]);
        if (target == null) {
            sender.sendMessage("Unknown player: " + args[0]);
            return true;
        }

        String duration = args[1];
        String reason = args.length >= 3 ? String.join(" ", slice(args, 2)) : "Temporarily banned by staff.";
        Optional<String> error = punishmentService.tempBan(sender, target, duration, reason);
        if (error.isPresent()) {
            sender.sendMessage(error.get());
            return true;
        }
        sender.sendMessage("Temp banned " + punishmentService.safeTargetName(target) + " for " + duration);
        return true;
    }

    private boolean handleBan(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("Usage: /banplayer <player> [reason]");
            return true;
        }
        OfflinePlayer target = resolveTarget(args[0]);
        if (target == null) {
            sender.sendMessage("Unknown player: " + args[0]);
            return true;
        }

        String reason = args.length >= 2 ? String.join(" ", slice(args, 1)) : "Permanently banned by staff.";
        punishmentService.ban(sender, target, reason);
        sender.sendMessage("Banned " + punishmentService.safeTargetName(target));
        return true;
    }

    private boolean handleIpBan(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("Usage: /ipban <player> [reason]");
            return true;
        }
        OfflinePlayer target = resolveTarget(args[0]);
        if (target == null) {
            sender.sendMessage("Unknown player: " + args[0]);
            return true;
        }

        String reason = args.length >= 2 ? String.join(" ", slice(args, 1)) : "IP banned by staff.";
        Optional<String> error = punishmentService.ipBan(sender, target, reason);
        if (error.isPresent()) {
            sender.sendMessage(error.get());
            return true;
        }

        sender.sendMessage("IP banned " + punishmentService.safeTargetName(target));
        return true;
    }

    private boolean handleUnban(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("Usage: /unbanplayer <player>");
            return true;
        }

        OfflinePlayer target = resolveTarget(args[0]);
        if (target == null) {
            sender.sendMessage("Unknown player: " + args[0]);
            return true;
        }

        punishmentService.unban(sender, target);
        sender.sendMessage("Unbanned " + punishmentService.safeTargetName(target));
        return true;
    }

    private boolean handleHistory(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("Usage: /bmhistory <player>");
            return true;
        }

        OfflinePlayer target = resolveTarget(args[0]);
        if (target == null) {
            sender.sendMessage("Unknown player: " + args[0]);
            return true;
        }

        UUID uuid = target.getUniqueId();
        List<PlayerRepository.WarningRecord> warnings = repository.getWarnings(uuid);
        List<PlayerRepository.SanctionRecord> sanctions = repository.getSanctions(uuid);
        String fmt = plugin.getConfig().getString("settings.date-format", "yyyy-MM-dd HH:mm:ss");

        sender.sendMessage("---- History for " + punishmentService.safeTargetName(target) + " ----");
        sender.sendMessage("Warnings: " + warnings.size());
        for (PlayerRepository.WarningRecord warning : warnings.stream().skip(Math.max(0, warnings.size() - 5))
                .toList()) {
            sender.sendMessage(" - [" + TimeUtil.formatTimestamp(warning.getTimestamp(), fmt) + "] "
                    + warning.getRule() + " by " + warning.getStaff() + " (" + warning.getReason() + ")");
        }

        sender.sendMessage("Sanctions: " + sanctions.size());
        for (PlayerRepository.SanctionRecord sanction : sanctions.stream().skip(Math.max(0, sanctions.size() - 5))
                .toList()) {
            sender.sendMessage(" - [" + TimeUtil.formatTimestamp(sanction.getTimestamp(), fmt) + "] "
                    + sanction.getType() + " by " + sanction.getStaff() + " (" + sanction.getReason() + ")");
        }
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        plugin.reloadAll();
        sender.sendMessage("BanManager configuration reloaded.");
        return true;
    }

    @SuppressWarnings("deprecation")
    private OfflinePlayer resolveTarget(String nameOrUuid) {
        try {
            UUID uuid = UUID.fromString(nameOrUuid);
            return Bukkit.getOfflinePlayer(uuid);
        } catch (IllegalArgumentException ignored) {
        }

        PlayerRepository.PlayerRecord fromData = repository.findByName(nameOrUuid).orElse(null);
        if (fromData != null) {
            return Bukkit.getOfflinePlayer(fromData.getUuid());
        }

        for (org.bukkit.entity.Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().equalsIgnoreCase(nameOrUuid)) {
                return online;
            }
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(nameOrUuid);
        if (offline != null && (offline.hasPlayedBefore() || offline.isOnline())) {
            return offline;
        }
        return null;
    }

    private static String[] slice(String[] input, int fromIndex) {
        if (fromIndex >= input.length) {
            return new String[0];
        }
        String[] result = new String[input.length - fromIndex];
        System.arraycopy(input, fromIndex, result, 0, result.length);
        return result;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String commandName = command.getName().toLowerCase(Locale.ROOT);
        if (commandName.equals("warn") && args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return ruleService.getRules().keySet().stream()
                    .filter(k -> k.startsWith(prefix))
                    .sorted()
                    .collect(Collectors.toList());
        }

        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
            return names.stream().filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix)).sorted()
                    .collect(Collectors.toList());
        }

        if (commandName.equals("tempban") && args.length == 2) {
            return List.of("30m", "12h", "1d", "7d");
        }

        return Collections.emptyList();
    }
}
