package com.banmanager.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class TimeUtil {

    private TimeUtil() {
    }

    public static long parseDurationMillis(String input) {
        if (input == null || input.isBlank()) {
            return -1;
        }

        String trimmed = input.trim().toLowerCase(Locale.ROOT);
        long multiplier;
        char unit = trimmed.charAt(trimmed.length() - 1);

        if (Character.isDigit(unit)) {
            multiplier = 1000L;
        } else {
            multiplier = switch (unit) {
                case 's' -> 1000L;
                case 'm' -> 60_000L;
                case 'h' -> 3_600_000L;
                case 'd' -> 86_400_000L;
                case 'w' -> 604_800_000L;
                default -> -1L;
            };
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        if (multiplier < 0 || trimmed.isBlank()) {
            return -1;
        }

        try {
            long value = Long.parseLong(trimmed);
            return value * multiplier;
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    public static String formatTimestamp(long epochMillis, String pattern) {
        if (epochMillis <= 0) {
            return "never";
        }
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
        format.setTimeZone(TimeZone.getDefault());
        return format.format(new Date(epochMillis));
    }
}
