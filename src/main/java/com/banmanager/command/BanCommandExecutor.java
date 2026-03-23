package com.banmanager.command;

import com.banmanager.BanManagerPlugin;
import com.banmanager.service.PlayerRepository;
import com.banmanager.service.PunishmentService;
import com.banmanager.service.RuleService;
import com.banmanager.util.TimeUtil;
import java.util.ArrayList;
import java.util.Comparator;
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
            case "unwarn" -> handleUnwarn(sender, args);
            case "kick" -> handleKick(sender, args);
            case "tempban" -> handleTempBan(sender, args);
            case "ban" -> handleBan(sender, args);
            case "ipban" -> handleIpBan(sender, args);
            case "unban" -> handleUnban(sender, args);
            case "clearpunishments" -> handleClearPunishments(sender, args);
            case "bmhistory" -> handleHistory(sender, args);
            case "bmreload" -> handleReload(sender);
            default -> false;
        };
    }

    private boolean handleUnwarn(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(punishmentService.color("&eUsage: &f/unwarn <player> <count|all>"));
            return true;
        }

        OfflinePlayer target = resolveTarget(args[0]);
        if (target == null) {
            sender.sendMessage(punishmentService.color("&cUnknown player: &f" + args[0]));
            return true;
        }

        int removed = removeWarnings(target, args[1]);
        if (removed < 0) {
            sender.sendMessage(punishmentService.color("&cInvalid amount. Use a number or 'all'."));
            return true;
        }

        sender.sendMessage(punishmentService.color(
                "&aRemoved &f" + removed + " &awarning(s) from &f" + punishmentService.safeTargetName(target)));
        return true;
    }

    private boolean handleWarn(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(punishmentService.color("&eUsage: &f/warn <player> <rule> [reason]"));
            return true;
        }

        OfflinePlayer target = resolveTarget(args[0]);
        if (target == null) {
            sender.sendMessage(punishmentService.color(plugin.getConfig()
                    .getString("messages.player-not-found", "&cPlayer not found: %player%")
                    .replace("%player%", args[0])));
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
            sender.sendMessage(punishmentService.color("&eUsage: &f/kick <player> [reason]"));
            return true;
        }
        OfflinePlayer target = resolveTarget(args[0]);
        if (target == null) {
            sender.sendMessage(punishmentService.color("&cUnknown player: &f" + args[0]));
            return true;
        }

        String reason = args.length >= 2 ? String.join(" ", slice(args, 1)) : "Kicked by staff.";
        punishmentService.kick(sender, target, reason);
        sender.sendMessage(punishmentService.color("&aKicked &f" + punishmentService.safeTargetName(target)));
        return true;
    }

    private boolean handleTempBan(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(punishmentService.color("&eUsage: &f/tempban <player> <duration> [reason]"));
            return true;
        }
        OfflinePlayer target = resolveTarget(args[0]);
        if (target == null) {
            sender.sendMessage(punishmentService.color("&cUnknown player: &f" + args[0]));
            return true;
        }

        String duration = args[1];
        String reason = args.length >= 3 ? String.join(" ", slice(args, 2)) : "Temporarily banned by staff.";
        Optional<String> error = punishmentService.tempBan(sender, target, duration, reason);
        if (error.isPresent()) {
            sender.sendMessage(punishmentService.color("&c" + error.get()));
            return true;
        }
        sender.sendMessage(punishmentService.color(
                "&aTemp banned &f" + punishmentService.safeTargetName(target) + " &afor &f" + duration));
        return true;
    }

    private boolean handleBan(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(punishmentService.color("&eUsage: &f/ban <player> [reason]"));
            return true;
        }
        OfflinePlayer target = resolveTarget(args[0]);
        if (target == null) {
            sender.sendMessage(punishmentService.color("&cUnknown player: &f" + args[0]));
            return true;
        }

        String reason = args.length >= 2 ? String.join(" ", slice(args, 1)) : "Permanently banned by staff.";
        punishmentService.ban(sender, target, reason);
        sender.sendMessage(punishmentService.color("&aBanned &f" + punishmentService.safeTargetName(target)));
        return true;
    }

    private boolean handleIpBan(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(punishmentService.color("&eUsage: &f/ipban <player> [reason]"));
            return true;
        }
        OfflinePlayer target = resolveTarget(args[0]);
        if (target == null) {
            sender.sendMessage(punishmentService.color("&cUnknown player: &f" + args[0]));
            return true;
        }

        String reason = args.length >= 2 ? String.join(" ", slice(args, 1)) : "IP banned by staff.";
        Optional<String> error = punishmentService.ipBan(sender, target, reason);
        if (error.isPresent()) {
            sender.sendMessage(punishmentService.color("&c" + error.get()));
            return true;
        }

        sender.sendMessage(punishmentService.color("&aIP banned &f" + punishmentService.safeTargetName(target)));
        return true;
    }

    private boolean handleUnban(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(punishmentService.color("&eUsage: &f/unban <player>"));
            return true;
        }

        OfflinePlayer target = resolveTarget(args[0]);
        if (target == null) {
            sender.sendMessage(punishmentService.color("&cUnknown player: &f" + args[0]));
            return true;
        }

        punishmentService.unban(sender, target);
        sender.sendMessage(punishmentService.color("&aUnbanned &f" + punishmentService.safeTargetName(target)));
        return true;
    }

    private boolean handleClearPunishments(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(punishmentService.color("&eUsage: &f/clearpunishments <player> <count|all>"));
            return true;
        }

        OfflinePlayer target = resolveTarget(args[0]);
        if (target == null) {
            sender.sendMessage(punishmentService.color("&cUnknown player: &f" + args[0]));
            return true;
        }

        int removed = removeSanctions(target, args[1]);
        if (removed < 0) {
            sender.sendMessage(punishmentService.color("&cInvalid amount. Use a number or 'all'."));
            return true;
        }

        sender.sendMessage(punishmentService.color(
                "&aRemoved &f" + removed + " &apunishment record(s) from &f"
                        + punishmentService.safeTargetName(target)));
        return true;
    }

    private boolean handleHistory(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(punishmentService.color("&eUsage: &f/bmhistory <player>"));
            return true;
        }

        OfflinePlayer target = resolveTarget(args[0]);
        if (target == null) {
            sender.sendMessage(punishmentService.color("&cUnknown player: &f" + args[0]));
            return true;
        }

        UUID uuid = target.getUniqueId();
        List<PlayerRepository.WarningRecord> warnings = repository.getWarnings(uuid);
        List<PlayerRepository.SanctionRecord> sanctions = repository.getSanctions(uuid);
        String fmt = plugin.getConfig().getString("settings.date-format", "yyyy-MM-dd HH:mm:ss");

        record EventLine(long timestamp, String text) {
        }

        List<EventLine> lines = new ArrayList<>();
        for (PlayerRepository.WarningRecord warning : warnings) {
            lines.add(new EventLine(
                    warning.getTimestamp(),
                    "WARN | rule=" + warning.getRule() + " | staff=" + warning.getStaff() + " | reason="
                            + warning.getReason()));
        }
        for (PlayerRepository.SanctionRecord sanction : sanctions) {
            lines.add(new EventLine(
                    sanction.getTimestamp(),
                    sanction.getType() + " | rule=" + sanction.getRule() + " | staff=" + sanction.getStaff()
                            + " | reason=" + sanction.getReason()
                            + (sanction.getDetails() == null || sanction.getDetails().isBlank() ? ""
                                    : " | details=" + sanction.getDetails())));
        }
        lines.sort(Comparator.comparingLong(EventLine::timestamp));

        sender.sendMessage(
                punishmentService.color("&6---- History for " + punishmentService.safeTargetName(target) + " ----"));
        sender.sendMessage(punishmentService.color("&7UUID: &f" + uuid));
        sender.sendMessage(punishmentService.color(
                "&cWarnings: &f" + warnings.size()
                        + " &8| &eSanctions: &f" + sanctions.size()
                        + " &8| &bEvents: &f" + lines.size()));
        if (lines.isEmpty()) {
            sender.sendMessage(punishmentService.color("&7No moderation history found."));
            return true;
        }
        for (EventLine line : lines) {
            String rowColor = line.text().startsWith("WARN") ? "&c" : "&b";
            sender.sendMessage(punishmentService.color(
                    rowColor + "- [" + TimeUtil.formatTimestamp(line.timestamp(), fmt) + "] &f" + line.text()));
        }
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        plugin.reloadAll();
        sender.sendMessage(punishmentService.color("&aBanManager configuration and data reloaded from disk."));
        return true;
    }

    private int removeWarnings(OfflinePlayer target, String amountArg) {
        String normalized = amountArg.toLowerCase(Locale.ROOT);
        if (normalized.equals("all")) {
            return repository.clearWarnings(target.getUniqueId(), punishmentService.safeTargetName(target));
        }

        int amount;
        try {
            amount = Integer.parseInt(normalized);
        } catch (NumberFormatException ex) {
            return -1;
        }
        if (amount <= 0) {
            return -1;
        }
        return repository.removeRecentWarnings(target.getUniqueId(), punishmentService.safeTargetName(target), amount);
    }

    private int removeSanctions(OfflinePlayer target, String amountArg) {
        String normalized = amountArg.toLowerCase(Locale.ROOT);
        if (normalized.equals("all")) {
            return repository.clearSanctions(target.getUniqueId(), punishmentService.safeTargetName(target));
        }

        int amount;
        try {
            amount = Integer.parseInt(normalized);
        } catch (NumberFormatException ex) {
            return -1;
        }
        if (amount <= 0) {
            return -1;
        }
        return repository.removeRecentSanctions(target.getUniqueId(), punishmentService.safeTargetName(target), amount);
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
            return List.of("30s", "30m", "12h", "1d", "7d");
        }

        if ((commandName.equals("unwarn") || commandName.equals("clearpunishments")) && args.length == 2) {
            return List.of("1", "2", "5", "all");
        }

        return Collections.emptyList();
    }
}
