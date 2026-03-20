package com.banmanager.service;

import com.banmanager.BanManagerPlugin;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class PlayerRepository {

    public static class WarningRecord {
        private final long timestamp;
        private final String rule;
        private final String staff;
        private final String reason;

        public WarningRecord(long timestamp, String rule, String staff, String reason) {
            this.timestamp = timestamp;
            this.rule = rule;
            this.staff = staff;
            this.reason = reason;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getRule() {
            return rule;
        }

        public String getStaff() {
            return staff;
        }

        public String getReason() {
            return reason;
        }

        public Map<String, Object> serialize() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("timestamp", timestamp);
            map.put("rule", rule);
            map.put("staff", staff);
            map.put("reason", reason);
            return map;
        }

        public static WarningRecord deserialize(Map<?, ?> input) {
            long timestamp = asLong(input.get("timestamp"));
            String rule = asString(input.get("rule"), "unknown");
            String staff = asString(input.get("staff"), "system");
            String reason = asString(input.get("reason"), "");
            return new WarningRecord(timestamp, rule, staff, reason);
        }
    }

    public static class SanctionRecord {
        private final long timestamp;
        private final String type;
        private final String rule;
        private final String staff;
        private final String reason;
        private final String details;

        public SanctionRecord(long timestamp, String type, String rule, String staff, String reason, String details) {
            this.timestamp = timestamp;
            this.type = type;
            this.rule = rule;
            this.staff = staff;
            this.reason = reason;
            this.details = details;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getType() {
            return type;
        }

        public String getRule() {
            return rule;
        }

        public String getStaff() {
            return staff;
        }

        public String getReason() {
            return reason;
        }

        public String getDetails() {
            return details;
        }

        public Map<String, Object> serialize() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("timestamp", timestamp);
            map.put("type", type);
            map.put("rule", rule);
            map.put("staff", staff);
            map.put("reason", reason);
            map.put("details", details);
            return map;
        }

        public static SanctionRecord deserialize(Map<?, ?> input) {
            long timestamp = asLong(input.get("timestamp"));
            String type = asString(input.get("type"), "UNKNOWN");
            String rule = asString(input.get("rule"), "");
            String staff = asString(input.get("staff"), "system");
            String reason = asString(input.get("reason"), "");
            String details = asString(input.get("details"), "");
            return new SanctionRecord(timestamp, type, rule, staff, reason, details);
        }
    }

    public static class ActiveBan {
        private final String type;
        private final String reason;
        private final String staff;
        private final long createdAt;
        private final long expiresAt;
        private final String ipAddress;

        public ActiveBan(String type, String reason, String staff, long createdAt, long expiresAt, String ipAddress) {
            this.type = type;
            this.reason = reason;
            this.staff = staff;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
            this.ipAddress = ipAddress;
        }

        public String getType() {
            return type;
        }

        public String getReason() {
            return reason;
        }

        public String getStaff() {
            return staff;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public long getExpiresAt() {
            return expiresAt;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public boolean isTempBan() {
            return "TEMP_BAN".equalsIgnoreCase(type);
        }

        public boolean isIpBan() {
            return "IP_BAN".equalsIgnoreCase(type);
        }

        public boolean isExpired(long now) {
            return isTempBan() && expiresAt > 0 && now >= expiresAt;
        }

        public Map<String, Object> serialize() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", type);
            map.put("reason", reason);
            map.put("staff", staff);
            map.put("createdAt", createdAt);
            map.put("expiresAt", expiresAt);
            map.put("ipAddress", ipAddress);
            return map;
        }

        public static ActiveBan deserialize(ConfigurationSection section) {
            if (section == null) {
                return null;
            }
            String type = section.getString("type", "NONE");
            if ("NONE".equalsIgnoreCase(type)) {
                return null;
            }
            return new ActiveBan(
                    type,
                    section.getString("reason", ""),
                    section.getString("staff", "system"),
                    section.getLong("createdAt", 0L),
                    section.getLong("expiresAt", 0L),
                    section.getString("ipAddress", ""));
        }
    }

    public static class PlayerRecord {
        private final UUID uuid;
        private String lastKnownName;
        private String lastKnownIp;
        private final List<WarningRecord> warnings = new ArrayList<>();
        private final List<SanctionRecord> sanctions = new ArrayList<>();
        private ActiveBan activeBan;

        public PlayerRecord(UUID uuid, String lastKnownName) {
            this.uuid = uuid;
            this.lastKnownName = lastKnownName;
        }

        public UUID getUuid() {
            return uuid;
        }

        public String getLastKnownName() {
            return lastKnownName;
        }

        public void setLastKnownName(String lastKnownName) {
            if (lastKnownName != null && !lastKnownName.isBlank()) {
                this.lastKnownName = lastKnownName;
            }
        }

        public String getLastKnownIp() {
            return lastKnownIp;
        }

        public void setLastKnownIp(String lastKnownIp) {
            this.lastKnownIp = lastKnownIp;
        }

        public List<WarningRecord> getWarnings() {
            return warnings;
        }

        public List<SanctionRecord> getSanctions() {
            return sanctions;
        }

        public ActiveBan getActiveBan() {
            return activeBan;
        }

        public void setActiveBan(ActiveBan activeBan) {
            this.activeBan = activeBan;
        }
    }

    private final BanManagerPlugin plugin;
    private final File dataFile;
    private final Map<UUID, PlayerRecord> players = new HashMap<>();

    public PlayerRepository(BanManagerPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
    }

    public synchronized void load() {
        players.clear();
        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection root = yaml.getConfigurationSection("players");
        if (root == null) {
            return;
        }

        for (String uuidKey : root.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidKey);
            } catch (IllegalArgumentException ex) {
                continue;
            }

            ConfigurationSection section = root.getConfigurationSection(uuidKey);
            if (section == null) {
                continue;
            }

            PlayerRecord record = new PlayerRecord(uuid, section.getString("lastKnownName", "unknown"));
            record.setLastKnownIp(section.getString("lastKnownIp", ""));

            List<Map<?, ?>> warnings = section.getMapList("warnings");
            for (Map<?, ?> warning : warnings) {
                record.getWarnings().add(WarningRecord.deserialize(warning));
            }

            List<Map<?, ?>> sanctions = section.getMapList("sanctions");
            for (Map<?, ?> sanction : sanctions) {
                record.getSanctions().add(SanctionRecord.deserialize(sanction));
            }

            record.setActiveBan(ActiveBan.deserialize(section.getConfigurationSection("activeBan")));
            players.put(uuid, record);
        }
    }

    public synchronized void save() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder.");
            return;
        }

        YamlConfiguration yaml = new YamlConfiguration();
        for (PlayerRecord record : players.values()) {
            String base = "players." + record.getUuid();
            yaml.set(base + ".lastKnownName", record.getLastKnownName());
            yaml.set(base + ".lastKnownIp", record.getLastKnownIp());

            List<Map<String, Object>> warningData = record.getWarnings().stream()
                    .map(WarningRecord::serialize)
                    .collect(Collectors.toList());
            yaml.set(base + ".warnings", warningData);

            List<Map<String, Object>> sanctionData = record.getSanctions().stream()
                    .map(SanctionRecord::serialize)
                    .collect(Collectors.toList());
            yaml.set(base + ".sanctions", sanctionData);

            yaml.set(base + ".activeBan", record.getActiveBan() == null ? null : record.getActiveBan().serialize());
        }

        try {
            yaml.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to save data.yml: " + ex.getMessage());
        }
    }

    public synchronized PlayerRecord getOrCreate(UUID uuid, String playerName) {
        PlayerRecord record = players.computeIfAbsent(uuid, key -> new PlayerRecord(uuid, safeName(playerName)));
        record.setLastKnownName(playerName);
        return record;
    }

    public synchronized Optional<PlayerRecord> findByName(String playerName) {
        if (playerName == null) {
            return Optional.empty();
        }
        return players.values().stream()
                .filter(r -> r.getLastKnownName() != null && r.getLastKnownName().equalsIgnoreCase(playerName))
                .findFirst();
    }

    public synchronized Optional<PlayerRecord> findByUuid(UUID uuid) {
        return Optional.ofNullable(players.get(uuid));
    }

    public synchronized void updateIdentity(UUID uuid, String playerName, String ipAddress) {
        PlayerRecord record = getOrCreate(uuid, playerName);
        record.setLastKnownName(playerName);
        if (ipAddress != null && !ipAddress.isBlank()) {
            record.setLastKnownIp(ipAddress);
        }
        save();
    }

    public synchronized void addWarning(UUID uuid, String playerName, WarningRecord warning) {
        PlayerRecord record = getOrCreate(uuid, playerName);
        record.getWarnings().add(warning);
        save();
    }

    public synchronized void addSanction(UUID uuid, String playerName, SanctionRecord sanction) {
        PlayerRecord record = getOrCreate(uuid, playerName);
        record.getSanctions().add(sanction);
        save();
    }

    public synchronized void setActiveBan(UUID uuid, String playerName, ActiveBan activeBan) {
        PlayerRecord record = getOrCreate(uuid, playerName);
        record.setActiveBan(activeBan);
        save();
    }

    public synchronized void clearActiveBan(UUID uuid, String playerName) {
        PlayerRecord record = getOrCreate(uuid, playerName);
        record.setActiveBan(null);
        save();
    }

    public synchronized List<WarningRecord> getWarnings(UUID uuid) {
        PlayerRecord record = players.get(uuid);
        if (record == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(record.getWarnings());
    }

    public synchronized List<SanctionRecord> getSanctions(UUID uuid) {
        PlayerRecord record = players.get(uuid);
        if (record == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(record.getSanctions());
    }

    public synchronized long countWarningsForRule(UUID uuid, String rule) {
        return getWarnings(uuid).stream()
                .filter(w -> w.getRule().equalsIgnoreCase(rule))
                .count();
    }

    public synchronized long countWarningsInWindow(UUID uuid, long now, long windowMillis) {
        long minTs = now - windowMillis;
        return getWarnings(uuid).stream()
                .filter(w -> w.getTimestamp() >= minTs)
                .count();
    }

    public synchronized long countWarningsForRuleInWindow(UUID uuid, String rule, long now, long windowMillis) {
        long minTs = now - windowMillis;
        return getWarnings(uuid).stream()
                .filter(w -> w.getTimestamp() >= minTs)
                .filter(w -> w.getRule().equalsIgnoreCase(rule))
                .count();
    }

    public synchronized Optional<ActiveBan> getActiveBan(UUID uuid, String ipAddress, long now) {
        PlayerRecord record = players.get(uuid);
        if (record == null || record.getActiveBan() == null) {
            return Optional.empty();
        }

        ActiveBan activeBan = record.getActiveBan();
        if (activeBan.isExpired(now)) {
            record.setActiveBan(null);
            save();
            return Optional.empty();
        }

        if (activeBan.isIpBan()) {
            if (ipAddress == null || ipAddress.isBlank()) {
                return Optional.empty();
            }
            if (!Objects.equals(activeBan.getIpAddress(), ipAddress)) {
                return Optional.empty();
            }
        }

        return Optional.of(activeBan);
    }

    public synchronized Optional<ActiveBan> getActiveIpBan(String ipAddress, long now) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return Optional.empty();
        }

        for (PlayerRecord record : players.values()) {
            ActiveBan ban = record.getActiveBan();
            if (ban == null || !ban.isIpBan()) {
                continue;
            }
            if (ban.isExpired(now)) {
                record.setActiveBan(null);
                continue;
            }
            if (ipAddress.equals(ban.getIpAddress())) {
                return Optional.of(ban);
            }
        }

        save();
        return Optional.empty();
    }

    private static String safeName(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return "unknown";
        }
        return playerName;
    }

    private static long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private static String asString(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? fallback : text;
    }
}
