-- Migration 065: Eliminar modelo legacy tour_reservation (reemplazado por reservation + POST /reservations)
-- Date: 2026-07-02
-- Verificado en PROD: 1 fila (2025-07-13), 0 en últimos 90 días; flujo activo = reservation (226 filas).

DROP TABLE IF EXISTS public.tour_reservation_status_history;
DROP TABLE IF EXISTS public.tour_reservation_detail;
DROP TABLE IF EXISTS public.tour_reservation;

DROP SEQUENCE IF EXISTS public.tour_reservation_status_history_id_seq;
DROP SEQUENCE IF EXISTS public.tour_reservation_detail_id_seq;
DROP SEQUENCE IF EXISTS public.tour_reservation_id_seq;
