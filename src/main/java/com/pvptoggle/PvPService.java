package com.pvptoggle;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PvPService {

    private final PvPTogglePlugin plugin;
    private final Map<UUID, Boolean> playerStates = new ConcurrentHashMap<>();
    private final Map<UUID, Long> toggleTimestamps = new ConcurrentHashMap<>();

    public PvPService(PvPTogglePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isPvpEnabled(UUID uuid) {
        return playerStates.getOrDefault(uuid, getDefaultPvpEnabled());
    }

    public boolean toggle(UUID uuid) {
        boolean newState = !isPvpEnabled(uuid);
        playerStates.put(uuid, newState);
        toggleTimestamps.put(uuid, System.currentTimeMillis());
        return newState;
    }

    public long getRemainingCooldownSeconds(UUID uuid) {
        long cooldownMs = getCooldownSeconds() * 1000L;
        if (cooldownMs <= 0) {
            return 0;
        }

        long lastToggle = toggleTimestamps.getOrDefault(uuid, 0L);
        long elapsed = System.currentTimeMillis() - lastToggle;
        long remainingMs = cooldownMs - elapsed;

        if (remainingMs <= 0) {
            return 0;
        }

        return (remainingMs + 999L) / 1000L;
    }

    private boolean getDefaultPvpEnabled() {
        FileConfiguration config = plugin.getConfig();
        return config.getBoolean("default-pvp-enabled", false);
    }

    private int getCooldownSeconds() {
        FileConfiguration config = plugin.getConfig();
        return Math.max(0, config.getInt("cooldown-seconds", 30));
    }
}
