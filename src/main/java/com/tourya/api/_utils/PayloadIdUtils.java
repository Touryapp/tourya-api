package com.tourya.api._utils;

/**
 * El front suele enviar {@code 0} o {@code ""} cuando el registro aún no tiene id en BD.
 */
public final class PayloadIdUtils {

    private PayloadIdUtils() {
    }

    /** Id ausente o placeholder del front (0, negativo). */
    public static boolean isTransientId(Integer id) {
        return id == null || id <= 0;
    }

    /** Id que puede usarse para buscar/actualizar en BD. */
    public static boolean isPersistedId(Integer id) {
        return !isTransientId(id);
    }

    public static Integer normalizeId(Integer id) {
        return isTransientId(id) ? null : id;
    }
}
