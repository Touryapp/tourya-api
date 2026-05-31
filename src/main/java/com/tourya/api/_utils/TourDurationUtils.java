package com.tourya.api._utils;

import com.tourya.api.constans.enums.TourDurationEnum;
import com.tourya.api.models.Tour;

import java.time.LocalDateTime;

/**
 * Resuelve duración y fecha de regreso para reservas/pagos a partir de {@code tour.duration}
 * o {@code tour.duration_enum} cuando el texto libre no está definido.
 */
public final class TourDurationUtils {

    private TourDurationUtils() {
    }

    public static String resolveDurationLabel(Tour tour) {
        if (tour == null) {
            return null;
        }
        if (tour.getDuration() != null && !tour.getDuration().isBlank()) {
            return tour.getDuration().trim();
        }
        if (tour.getDurationEnum() != null) {
            return formatDurationEnum(tour.getDurationEnum());
        }
        return null;
    }

    public static LocalDateTime computeReturnDate(LocalDateTime checkIn, Tour tour) {
        if (checkIn == null || tour == null) {
            return null;
        }
        if (tour.getDuration() != null && !tour.getDuration().isBlank()) {
            LocalDateTime fromText = computeReturnDateFromDurationDays(checkIn, tour.getDuration());
            if (fromText != null) {
                return fromText;
            }
        }
        Integer days = daysFromDurationEnum(tour.getDurationEnum());
        if (days == null) {
            return null;
        }
        if (days == 0) {
            return checkIn;
        }
        return checkIn.plusDays(days);
    }

    public static LocalDateTime computeReturnDateFromDurationDays(LocalDateTime checkIn, String durationRaw) {
        if (checkIn == null || durationRaw == null || durationRaw.isBlank()) {
            return null;
        }
        int days = parseDurationDays(durationRaw);
        if (days > 0) {
            return checkIn.plusDays(days);
        }
        return null;
    }

    public static int parseDurationDays(String durationRaw) {
        try {
            String durationStr = durationRaw.trim();
            try {
                return Integer.parseInt(durationStr);
            } catch (NumberFormatException e) {
                String[] parts = durationStr.split(" ");
                for (int i = 0; i < parts.length; i++) {
                    if (parts[i].equalsIgnoreCase("Days") || parts[i].equalsIgnoreCase("Day")) {
                        if (i > 0) {
                            return Integer.parseInt(parts[i - 1]);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // fall through
        }
        return 0;
    }

    private static Integer daysFromDurationEnum(TourDurationEnum durationEnum) {
        if (durationEnum == null) {
            return null;
        }
        return switch (durationEnum) {
            case H1_2, H2_4, H4_6 -> 0;
            case D1 -> 1;
            case D3 -> 3;
            case D5 -> 5;
        };
    }

    private static String formatDurationEnum(TourDurationEnum durationEnum) {
        return durationEnum.getValue().replace('_', ' ');
    }
}
