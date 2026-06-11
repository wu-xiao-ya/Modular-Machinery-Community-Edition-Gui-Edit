package com.fushu.mmceguiext.common.util;

import java.util.Locale;

public final class UnitFormat {
    private static final String[] UNITS = new String[]{"", "K", "M", "G", "T", "P", "E"};

    private UnitFormat() {
    }

    public static String compact(long value) {
        long abs = value == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(value);
        if (abs < 1000L) {
            return Long.toString(value);
        }

        double scaled = value;
        int unit = 0;
        while (Math.abs(scaled) >= 1000.0D && unit < UNITS.length - 1) {
            scaled /= 1000.0D;
            unit++;
        }

        String pattern = Math.abs(scaled) >= 100.0D ? "%.0f%s" : Math.abs(scaled) >= 10.0D ? "%.1f%s" : "%.2f%s";
        String formatted = String.format(Locale.ROOT, pattern, scaled, UNITS[unit]);
        return trimTrailingZeros(formatted);
    }

    public static String amountWithUnit(long value, String unit) {
        String suffix = unit == null || unit.trim().isEmpty() ? "" : " " + unit.trim();
        return compact(value) + suffix;
    }

    private static String trimTrailingZeros(String value) {
        int unitStart = value.length();
        while (unitStart > 0 && Character.isLetter(value.charAt(unitStart - 1))) {
            unitStart--;
        }
        String number = value.substring(0, unitStart);
        String suffix = value.substring(unitStart);
        if (number.indexOf('.') >= 0) {
            while (number.endsWith("0")) {
                number = number.substring(0, number.length() - 1);
            }
            if (number.endsWith(".")) {
                number = number.substring(0, number.length() - 1);
            }
        }
        return number + suffix;
    }
}
