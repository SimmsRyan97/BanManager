package com.banmanager.listener;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

public class CommandOverrideListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String raw = event.getMessage();
        if (raw == null || raw.length() < 2 || raw.charAt(0) != '/') {
            return;
        }

        String commandLine = raw.substring(1).trim();
        String remapped = remapCommand(commandLine);
        if (remapped == null) {
            return;
        }

        event.setCancelled(true);
        Bukkit.dispatchCommand(event.getPlayer(), remapped);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        String commandLine = event.getCommand();
        if (commandLine == null || commandLine.isBlank()) {
            return;
        }

        String remapped = remapCommand(commandLine.trim());
        if (remapped == null) {
            return;
        }

        event.setCancelled(true);
        Bukkit.dispatchCommand(event.getSender(), remapped);
    }

    private String remapCommand(String commandLine) {
        String[] parts = commandLine.split("\\s+", 2);
        String base = parts[0].toLowerCase();

        if (!base.equals("kick") && !base.equals("ban") && !base.equals("unban")) {
            return null;
        }

        if (base.contains(":")) {
            return null;
        }

        String args = parts.length > 1 ? " " + parts[1] : "";
        return "banmanager:" + base + args;
    }
}
