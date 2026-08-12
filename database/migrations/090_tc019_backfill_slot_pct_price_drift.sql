-- Migration 090: TC-019 (#231) bug 4 — Backfill drift entre slot_porcentaje_tourya y price base.
-- Date: 2026-08-12
-- Description:
--   Luis 2026-08-12: al editar slotPorcentajeTourya en el "slot de un dia", el valor se guarda
--   pero el price base del slot NO se recalcula (por dia se recalcula el override, pero si un
--   flujo previo persistio el pct base sin recalcular price, quedo drift).
--
--   Analisis del backend (2026-08-12):
--   - manageSlotsUpdate() en TourScheduleConfigGeneralService NO aceptaba slotPorcentajeTourya
--     del DTO (el DTO no tenia el campo). Solo actualizaba el pct base cuando era slot nuevo o
--     cuando el pct base era 0 y el tour tenia margen.
--   - Este PR agrega el campo al DTO y hace que manageSlotsUpdate persista el nuevo pct + recalcule
--     price base via applyPriceDtoToEntity (que ya recalcula desde providerPrice + slotPct).
--   - Migration 088 (parte 2) ya cubrio el caso price == provider_price + slot_pct > 0. Este
--     backfill cubre el caso residual: price != provider_price * (1 + slot_pct) con drift > 0.01.
--
--   Idempotente: WHERE filtra solo filas con drift. Ejecutar 2 veces = no-op.
--   No toca overrides per-schedule (tour_schedule_price_override) — esos ya se manejan por endpoint.

UPDATE public.tour_schedule_config_price p
SET price = ROUND(p.provider_price * (1 + COALESCE(sl.slot_porcentaje_tourya, 0)), 2)
FROM public.tour_schedule_config_slot sl
WHERE p.slot_id = sl.id
  AND p.provider_price IS NOT NULL
  AND p.provider_price > 0
  AND sl.slot_porcentaje_tourya IS NOT NULL
  AND ABS(p.price - p.provider_price * (1 + sl.slot_porcentaje_tourya)) > 0.01;
