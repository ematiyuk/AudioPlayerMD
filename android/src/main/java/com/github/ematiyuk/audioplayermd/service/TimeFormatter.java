package com.github.ematiyuk.audioplayermd.service;

import java.util.Formatter;
import java.util.Locale;

public class TimeFormatter {
    public static String format(long timeMs) {
        long totalSeconds = timeMs / 1000;

        int seconds = (int) (totalSeconds % 60);
        int minutes = (int) ((totalSeconds / 60) % 60);
        int hours   = (int) (totalSeconds / 3600);

        StringBuilder formatBuilder = new StringBuilder();
        Formatter formatter = new Formatter(formatBuilder, Locale.getDefault());

        formatBuilder.setLength(0);
        if (hours > 0) {
            return formatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return formatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }
}
