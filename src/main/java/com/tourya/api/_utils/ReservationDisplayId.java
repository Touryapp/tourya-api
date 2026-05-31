package com.tourya.api._utils;

public final class ReservationDisplayId {

    private static final String PREFIX = "TB-";

    private ReservationDisplayId() {
    }

    public static String format(Long reservationId) {
        return reservationId != null ? PREFIX + reservationId : null;
    }

    public static Long parse(String bookingIdOrReservationId) {
        if (bookingIdOrReservationId == null || bookingIdOrReservationId.isBlank()) {
            throw new IllegalArgumentException("Reservation id is required");
        }
        String value = bookingIdOrReservationId.trim();
        if (value.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
            value = value.substring(PREFIX.length());
        }
        return Long.parseLong(value);
    }
}
