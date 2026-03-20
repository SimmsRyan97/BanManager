package com.banmanager.service;

import com.banmanager.BanManagerPlugin;
import com.banmanager.service.PlayerRepository.ActiveBan;
import com.banmanager.service.PlayerRepository.SanctionRecord;
import com.banmanager.service.PlayerRepository.WarningRecord;
import com.banmanager.service.RuleService.ActionType;
import com.banmanager.service.RuleService.PunishmentConfig;
import com.banmanager.service.RuleService.RuleConfig;
import com.banmanager.util.TimeUtil;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PunishmentService {

    private final BanManagerPlugin plugin;
    private final PlayerRepository repository;
    private final RuleService ruleService;

    public PunishmentService(BanManagerPlugin plugin, PlayerRepository repository, RuleService ruleService) {
        this.plugin = plugin;
        this.repository = repository;
        this.ruleService = ruleService;
    }

    public Optional<String> applyWarning(CommandSender actor, OfflinePlayer target, String ruleKey, String reason) {
        RuleConfig rule = ruleService.getRule(ruleKey);
        if (rule == null) {
            return Optional.of(msg("messages.unknown-rule").replace("%rule%", ruleKey));
        }

        String staff = actor.getName();
        long now = System.currentTimeMillis();
        UUID uuid = target.getUniqueId();
        String targetName = safeTargetName(target);

        repository.addWarning(uuid, targetName, new WarningRecord(now, rule.getKey(), staff, reason));
        repository.addSanction(uuid, targetName, new SanctionRecord(now, "WARN", rule.getKey(), staff, reason, "warn"));

        long ruleWarnCount = repository.countWarningsForRule(uuid, rule.getKey());
        PunishmentConfig punishment = rule.getPunishmentForCount((int) ruleWarnCount);
        if (punishment != null && punishment.getType() != ActionType.NONE) {
            executeAction(actor, target, rule.getKey(), reason, punishment);
        }

        applyAutoKickChecks(actor, target, rule.getKey(), reason, now, rule);
        return Optional.empty();
    }

    public void kick(CommandSender actor, OfflinePlayer target, String reason) {
        String message = color("&cYou were kicked. &7Reason: " + reason);
        long now = System.currentTimeMillis();
        String name = safeTargetName(target);
        repository.addSanction(target.getUniqueId(), name,
                new SanctionRecord(now, "KICK", "manual", actor.getName(), reason, "manual-kick"));
        Player online = target.getPlayer();
        if (online != null) {
            online.kickPlayer(message);
        }
    }

    public Optional<String> tempBan(CommandSender actor, OfflinePlayer target, String durationInput, String reason) {
        long duration = TimeUtil.parseDurationMillis(durationInput);
        if (duration <= 0) {
            return Optional.of("Invalid duration. Use values like 30m, 12h, 7d, 2w.");
        }

        long now = System.currentTimeMillis();
        long expiresAt = now + duration;
        String name = safeTargetName(target);

        ActiveBan ban = new ActiveBan("TEMP_BAN", reason, actor.getName(), now, expiresAt, "");
        repository.setActiveBan(target.getUniqueId(), name, ban);
        repository.addSanction(target.getUniqueId(), name,
                new SanctionRecord(now, "TEMP_BAN", "manual", actor.getName(), reason, durationInput));

        Player online = target.getPlayer();
        if (online != null) {
            String msg = msg("messages.tempban-disconnect")
                    .replace("%reason%", reason)
                    .replace("%staff%", actor.getName())
                    .replace("%expires%", TimeUtil.formatTimestamp(expiresAt,
                            plugin.getConfig().getString("settings.date-format", "yyyy-MM-dd HH:mm:ss")));
            online.kickPlayer(color(msg));
        }

        return Optional.empty();
    }

    public void ban(CommandSender actor, OfflinePlayer target, String reason) {
        long now = System.currentTimeMillis();
        String name = safeTargetName(target);

        ActiveBan ban = new ActiveBan("BAN", reason, actor.getName(), now, 0L, "");
        repository.setActiveBan(target.getUniqueId(), name, ban);
        repository.addSanction(target.getUniqueId(), name,
                new SanctionRecord(now, "BAN", "manual", actor.getName(), reason, "permanent"));

        Player online = target.getPlayer();
        if (online != null) {
            String msg = msg("messages.ban-disconnect")
                    .replace("%reason%", reason)
                    .replace("%staff%", actor.getName());
            online.kickPlayer(color(msg));
        }
    }

    public Optional<String> ipBan(CommandSender actor, OfflinePlayer target, String reason) {
        long now = System.currentTimeMillis();
        String name = safeTargetName(target);

        String ip = null;
        Player online = target.getPlayer();
        if (online != null && online.getAddress() != null) {
            ip = online.getAddress().getAddress().getHostAddress();
        }
        if (ip == null || ip.isBlank()) {
            ip = repository.findByUuid(target.getUniqueId())
                    .map(PlayerRepository.PlayerRecord::getLastKnownIp)
                    .orElse("");
        }
        if (ip.isBlank()) {
            return Optional.of("Cannot IP-ban this player: no known IP recorded yet.");
        }

        ActiveBan ban = new ActiveBan("IP_BAN", reason, actor.getName(), now, 0L, ip);
        repository.setActiveBan(target.getUniqueId(), name, ban);
        repository.addSanction(target.getUniqueId(), name,
                new SanctionRecord(now, "IP_BAN", "manual", actor.getName(), reason, ip));

        if (online != null) {
            String msg = msg("messages.ipban-disconnect")
                    .replace("%reason%", reason)
                    .replace("%staff%", actor.getName());
            online.kickPlayer(color(msg));
        }

        return Optional.empty();
    }

    public void unban(CommandSender actor, OfflinePlayer target) {
        long now = System.currentTimeMillis();
        String name = safeTargetName(target);
        repository.clearActiveBan(target.getUniqueId(), name);
        repository.addSanction(target.getUniqueId(), name,
                new SanctionRecord(now, "UNBAN", "manual", actor.getName(), "", "manual-unban"));
    }

    public Optional<ActiveBan> getActiveBanForLogin(UUID uuid, String ipAddress) {
        long now = System.currentTimeMillis();
        Optional<ActiveBan> byUuid = repository.getActiveBan(uuid, ipAddress, now);
        if (byUuid.isPresent()) {
            return byUuid;
        }
        return repository.getActiveIpBan(ipAddress, now);
    }

    public String formatBanKickMessage(ActiveBan ban) {
        String dateFormat = plugin.getConfig().getString("settings.date-format", "yyyy-MM-dd HH:mm:ss");
        if (ban.isTempBan()) {
            return color(msg("messages.tempban-disconnect")
                    .replace("%reason%", ban.getReason())
                    .replace("%staff%", ban.getStaff())
                    .replace("%expires%", TimeUtil.formatTimestamp(ban.getExpiresAt(), dateFormat)));
        }
        if (ban.isIpBan()) {
            return color(msg("messages.ipban-disconnect")
                    .replace("%reason%", ban.getReason())
                    .replace("%staff%", ban.getStaff()));
        }
        return color(msg("messages.ban-disconnect")
                .replace("%reason%", ban.getReason())
                .replace("%staff%", ban.getStaff()));
    }

    public void updateIdentity(OfflinePlayer player, InetSocketAddress address) {
        if (player == null) {
            return;
        }
        String ip = "";
        if (address != null && address.getAddress() != null) {
            ip = address.getAddress().getHostAddress();
        }
        repository.updateIdentity(player.getUniqueId(), safeTargetName(player), ip);
    }

    private void applyAutoKickChecks(CommandSender actor, OfflinePlayer target, String ruleKey, String reason, long now,
            RuleConfig rule) {
        int windowSeconds = plugin.getConfig().getInt("settings.warn-window-seconds", 600);
        int maxWarns = plugin.getConfig().getInt("settings.max-warns-in-window", 3);
        long windowMillis = windowSeconds * 1000L;

        long warnsInWindow = repository.countWarningsInWindow(target.getUniqueId(), now, windowMillis);
        if (maxWarns > 0 && warnsInWindow >= maxWarns) {
            kick(actor, target, "Too many warnings in short period.");
            return;
        }

        int sameRuleThreshold = rule.getSameRuleAutoKickThreshold();
        long sameRuleInWindow = repository.countWarningsForRuleInWindow(target.getUniqueId(), ruleKey, now,
                windowMillis);
        if (sameRuleThreshold > 0 && sameRuleInWindow >= sameRuleThreshold) {
            kick(actor, target, "Repeatedly breaking rule: " + ruleKey);
        }
    }

    private void executeAction(CommandSender actor, OfflinePlayer target, String ruleKey, String reason,
            PunishmentConfig punishment) {
        String actionMessage = punishment.getMessage() == null ? "" : punishment.getMessage();
        switch (punishment.getType()) {
            case MESSAGE -> {
                Player online = target.getPlayer();
                if (online != null && !actionMessage.isBlank()) {
                    online.sendMessage(color(actionMessage));
                }
                logSanction(actor, target, "MESSAGE", ruleKey, reason, actionMessage);
            }
            case BROADCAST -> {
                String broadcast = punishment.getBroadcast().isBlank() ? actionMessage : punishment.getBroadcast();
                if (!broadcast.isBlank()) {
                    Bukkit.broadcastMessage(color(broadcast
                            .replace("%player%", safeTargetName(target))
                            .replace("%rule%", ruleKey)));
                }
                logSanction(actor, target, "BROADCAST", ruleKey, reason, broadcast);
            }
            case KICK -> {
                kick(actor, target, actionMessage.isBlank() ? "Rule violation: " + ruleKey : actionMessage);
                logSanction(actor, target, "KICK", ruleKey, reason, actionMessage);
            }
            case TEMP_BAN -> {
                String duration = punishment.getDuration().isBlank() ? "1d" : punishment.getDuration();
                tempBan(actor, target, duration, actionMessage.isBlank() ? reason : actionMessage);
                logSanction(actor, target, "TEMP_BAN", ruleKey, reason, duration);
            }
            case BAN -> {
                ban(actor, target, actionMessage.isBlank() ? reason : actionMessage);
                logSanction(actor, target, "BAN", ruleKey, reason, actionMessage);
            }
            case IP_BAN -> {
                ipBan(actor, target, actionMessage.isBlank() ? reason : actionMessage);
                logSanction(actor, target, "IP_BAN", ruleKey, reason, actionMessage);
            }
            default -> {
            }
        }
    }

    private void logSanction(CommandSender actor, OfflinePlayer target, String type, String rule, String reason,
            String details) {
        repository.addSanction(target.getUniqueId(), safeTargetName(target),
                new SanctionRecord(System.currentTimeMillis(), type, rule, actor.getName(), reason, details));
    }

    private String msg(String path) {
        return plugin.getConfig().getString(path, path);
    }

    public String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    public String safeTargetName(OfflinePlayer target) {
        if (target == null || target.getName() == null || target.getName().isBlank()) {
            return "unknown";
        }
        return target.getName();
    }
}
