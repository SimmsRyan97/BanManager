package com.pvptoggle;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("(?i)(?:&#|#)([A-F0-9]{6})");

    private ColorUtil() {
    }

    public static String colorize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(result, Matcher.quoteReplacement(toMinecraftHex(hex)));
        }

        matcher.appendTail(result);
        return ChatColor.translateAlternateColorCodes('&', result.toString());
    }

    private static String toMinecraftHex(String hex) {
        StringBuilder builder = new StringBuilder("\u00a7x");
        for (char c : hex.toCharArray()) {
            builder.append('\u00a7').append(c);
        }
        return builder.toString();
    }
}
