-- Migration 071: Reactivar tour.porcentaje_tourya como default del tour
-- Date: 2026-07-09
-- Description:
--   El campo tour.porcentaje_tourya se agrego en la migracion 043 y despues
--   se marco como "DEPRECATED" en la 050 al mover la logica al slot. Sin embargo
--   Luis (RN-015) confirmo que el campo debe existir como el default por tour
--   que se hereda al crear cada slot. Esta migracion:
--     1) Hace backfill: pone 0.15 (15%) en todos los tours que hoy tienen 0 o NULL.
--     2) Cambia el DEFAULT de la columna a 0.15 para nuevos tours.
--     3) Actualiza el COMMENT quitando "DEPRECATED" y explicando el nuevo rol.
--
--   Impacto:
--   - El campo tour.porcentaje_tourya NO se lee en el codigo hoy, entonces el
--     backfill no cambia comportamiento observable inmediato.
--   - En el PR de codigo asociado, TourScheduleConfigGeneralService al crear un
--     nuevo slot hereda este valor en vez de setear ZERO. Los slots ya existentes
--     con slot_porcentaje_tourya especifico NO se tocan.
--   - sp_get_tour_schedule_json y demas SPs ya usan slot_porcentaje_tourya (no
--     tour.porcentaje_tourya), asi que no hay cambio en calculo de precios de
--     tours existentes.

UPDATE public.tour
SET porcentaje_tourya = 0.15
WHERE porcentaje_tourya IS NULL OR porcentaje_tourya = 0;

ALTER TABLE public.tour
    ALTER COLUMN porcentaje_tourya SET DEFAULT 0.15;

COMMENT ON COLUMN public.tour.porcentaje_tourya IS
    'Porcentaje Tourya default del tour (fraccion decimal, ej. 0.15 = 15%). Solo editable por backoffice Tourya. Se hereda al crear un nuevo tour_schedule_config_slot como valor inicial de slot_porcentaje_tourya. El slot puede tener override por dia via tour_schedule_slot_override.';
