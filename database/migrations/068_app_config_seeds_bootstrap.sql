-- Migration 068: Seeds default en app_config para BE-06/07/08
-- Date: 2026-07-09
-- Description:
--   Inserta 3 configuraciones default que hasta ahora estaban hardcoded en el codigo:
--     - HOLD_MINUTES: minutos de duracion del hold temporal del carrito (ReservationService)
--     - PAYOUT_BUFFER_DAYS: dias entre fecha de la reserva y payout al proveedor
--     - CREDIT_EXPIRATION_MONTHS: meses de vigencia de un credito emitido (default: 6)
--
--   La tabla app_config YA EXISTE en la BD (creada fuera del sistema de migraciones);
--   este script solo agrega filas idempotentemente con ON CONFLICT DO NOTHING.
--   Los servicios refactorizados leen estos valores via AppConfigService.getInt(key, fallback),
--   asi que si por alguna razon estas filas no estan, la app usa el fallback hardcoded.

INSERT INTO public.app_config (config_key, config_value, description, created_date, created_by)
VALUES
    ('HOLD_MINUTES',
     '{"value": 15}'::jsonb,
     'Minutos que dura el hold temporal de una reserva antes de expirar. Usado por ReservationService al crear reservas TEMPORAL.',
     now(), 1),
    ('PAYOUT_BUFFER_DAYS',
     '{"value": 2}'::jsonb,
     'Dias entre la fecha de la reserva y la fecha en que el proveedor puede recibir el payout. Usado al confirmar el pago para setear reservation.payout_available_date.',
     now(), 1),
    ('CREDIT_EXPIRATION_MONTHS',
     '{"value": 6}'::jsonb,
     'Meses de vigencia de un credito emitido tras cancelacion de reserva. Usado al crear el Credit para setear expirationDate.',
     now(), 1)
ON CONFLICT (config_key) DO NOTHING;
