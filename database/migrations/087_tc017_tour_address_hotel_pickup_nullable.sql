-- Migration 087: TC-017 (#220) — tour_address country/state/city NULLABLE cuando addressType='Hotel Pickup'
-- Date: 2026-08-11
-- Description:
--   Cuando un tour es Hotel Pickup (recogida en el hotel del turista) no hay pais/estado/ciudad
--   fija — la ubicacion es la del hotel del cliente al momento de la reserva. El backend Java
--   ya fue relajado en PRs #226 y #228 (guard null en country/state/city lookup, enum matcher
--   tolerante), pero la BD sigue con NOT NULL en tour_address.country_id/state_id/city_id, lo
--   que hace fallar el INSERT con:
--     ERROR: null value in column "country_id" of relation "tour_address" violates not-null constraint
--
--   Cambios (idempotentes):
--   (a) Hace NULLABLE tour_address.country_id, state_id, city_id (DROP NOT NULL).
--   (b) Agrega CHECK constraint tour_address_geo_required_unless_hotel_pickup:
--       cuando address_type != 'Hotel Pickup', los tres geo IDs deben ser NOT NULL.
--       Solo 'Hotel Pickup' permite las 3 columnas NULL. Se crea NOT VALID para no romper
--       filas existentes (no deberia haberlas violando dado que los NOT NULL previos ya lo
--       garantizaban), y luego se valida.
--
--   Nota: el valor almacenado en address_type es el display value del enum
--   (AddressTypeEnum stores getValue() = 'Hotel Pickup', no la key 'HOTEL_PICKUP').
--   Verificado en com.tourya.api.constans.enums.AddressTypeEnumConverter.

-- (a) Relajar NOT NULL — idempotente: DROP NOT NULL no falla si ya es nullable.
ALTER TABLE public.tour_address ALTER COLUMN country_id DROP NOT NULL;
ALTER TABLE public.tour_address ALTER COLUMN state_id   DROP NOT NULL;
ALTER TABLE public.tour_address ALTER COLUMN city_id    DROP NOT NULL;

-- (b) CHECK constraint — solo Hotel Pickup admite geo NULL.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'tour_address_geo_required_unless_hotel_pickup'
          AND conrelid = 'public.tour_address'::regclass
    ) THEN
        ALTER TABLE public.tour_address
            ADD CONSTRAINT tour_address_geo_required_unless_hotel_pickup
            CHECK (
                address_type = 'Hotel Pickup'
                OR (country_id IS NOT NULL AND state_id IS NOT NULL AND city_id IS NOT NULL)
            )
            NOT VALID;
    END IF;
END $$;

-- Validar el constraint. Si alguna fila legacy no cumple, esto fallara y hay que
-- revisar/backfillear antes de mergear. Con el schema previo (NOT NULL en las 3
-- columnas) toda fila existente cumple la parte OR, asi que deberia pasar limpio.
ALTER TABLE public.tour_address
    VALIDATE CONSTRAINT tour_address_geo_required_unless_hotel_pickup;
