package com.banmanager.service;

import com.banmanager.BanManagerPlugin;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.bukkit.configuration.ConfigurationSection;

public class RuleService {

    public enum ActionType {
        NONE,
        MESSAGE,
        BROADCAST,
        KICK,
        TEMP_BAN,
        BAN,
        IP_BAN;

        public static ActionType from(String value) {
            if (value == null) {
                return NONE;
            }
            try {
                return ActionType.valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                return NONE;
            }
        }
    }

    public static class PunishmentConfig {
        private final ActionType type;
        private final String message;
        private final String duration;
        private final String broadcast;

        public PunishmentConfig(ActionType type, String message, String duration, String broadcast) {
            this.type = type;
            this.message = message;
            this.duration = duration;
            this.broadcast = broadcast;
        }

        public ActionType getType() {
            return type;
        }

        public String getMessage() {
            return message;
        }

        public String getDuration() {
            return duration;
        }

        public String getBroadcast() {
            return broadcast;
        }
    }

    public static class RuleConfig {
        private final String key;
        private final String displayName;
        private final int sameRuleAutoKickThreshold;
        private final NavigableMap<Integer, PunishmentConfig> punishments;

        public RuleConfig(String key, String displayName, int sameRuleAutoKickThreshold,
                NavigableMap<Integer, PunishmentConfig> punishments) {
            this.key = key;
            this.displayName = displayName;
            this.sameRuleAutoKickThreshold = sameRuleAutoKickThreshold;
            this.punishments = punishments;
        }

        public String getKey() {
            return key;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getSameRuleAutoKickThreshold() {
            return sameRuleAutoKickThreshold;
        }

        public PunishmentConfig getPunishmentForCount(int warningCount) {
            if (punishments.containsKey(warningCount)) {
                return punishments.get(warningCount);
            }
            Map.Entry<Integer, PunishmentConfig> floor = punishments.floorEntry(warningCount);
            if (floor != null) {
                return floor.getValue();
            }
            return null;
        }
    }

    private final BanManagerPlugin plugin;
    private final Map<String, RuleConfig> rules = new HashMap<>();

    public RuleService(BanManagerPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        rules.clear();
        ConfigurationSection rulesSection = plugin.getConfig().getConfigurationSection("rules");
        if (rulesSection == null) {
            return;
        }

        for (String ruleKey : rulesSection.getKeys(false)) {
            ConfigurationSection section = rulesSection.getConfigurationSection(ruleKey);
            if (section == null) {
                continue;
            }

            String displayName = section.getString("display-name", ruleKey);
            int sameRuleThreshold = section.getInt("same-rule-auto-kick-threshold", 0);

            NavigableMap<Integer, PunishmentConfig> punishmentMap = new TreeMap<>();

            ConfigurationSection punishments = section.getConfigurationSection("punishments");
            if (punishments != null) {
                for (String key : punishments.getKeys(false)) {
                    int count;
                    try {
                        count = Integer.parseInt(key);
                    } catch (NumberFormatException ex) {
                        continue;
                    }

                    ConfigurationSection action = punishments.getConfigurationSection(key);
                    if (action == null) {
                        continue;
                    }

                    ActionType type = ActionType.from(action.getString("type", "NONE"));
                    String message = action.getString("message", "");
                    String duration = action.getString("duration", "");
                    String broadcast = action.getString("broadcast", "");
                    punishmentMap.put(count, new PunishmentConfig(type, message, duration, broadcast));
                }
            }

            rules.put(ruleKey.toLowerCase(), new RuleConfig(ruleKey, displayName, sameRuleThreshold, punishmentMap));
        }
    }

    public RuleConfig getRule(String key) {
        if (key == null) {
            return null;
        }
        return rules.get(key.toLowerCase());
    }

    public Map<String, RuleConfig> getRules() {
        return Collections.unmodifiableMap(rules);
    }
}
