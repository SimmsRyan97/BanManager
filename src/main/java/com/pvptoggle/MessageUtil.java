package com.pvptoggle;

import org.bukkit.command.CommandSender;

import java.util.Map;

public final class MessageUtil {

    private MessageUtil() {
    }

    public static void send(PvPTogglePlugin plugin, CommandSender sender, String key) {
        send(plugin, sender, key, Map.of());
    }

    public static void send(PvPTogglePlugin plugin, CommandSender sender, String key,
            Map<String, String> placeholders) {
        String message = plugin.getConfig().getString("messages." + key, "");
        if (message.isBlank()) {
            return;
        }

        String prefix = plugin.getConfig().getString("messages.prefix", "");
        message = message.replace("{prefix}", prefix);

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        sender.sendMessage(ColorUtil.colorize(message));
    }
}
