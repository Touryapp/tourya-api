-- Flags de cancelación/reagendamiento en reserva (actualizados por cron y al confirmar pago)
ALTER TABLE public.reservation
    ADD COLUMN IF NOT EXISTS can_cancel BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE public.reservation
    ADD COLUMN IF NOT EXISTS can_reschedule BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN public.reservation.can_cancel IS
    'Indica si la reserva puede cancelarse según política y ventana; el cron puede ponerlo en false.';

COMMENT ON COLUMN public.reservation.can_reschedule IS
    'Indica si la reserva puede reagendarse según política y ventana; el cron puede ponerlo en false.';

-- Datos de checkout en carrito (hospedaje y facturación electrónica)
ALTER TABLE public.shopping_cart
    ADD COLUMN IF NOT EXISTS accommodation_name VARCHAR(255) NULL;

ALTER TABLE public.shopping_cart
    ADD COLUMN IF NOT EXISTS accommodation_latitude DOUBLE PRECISION NULL;

ALTER TABLE public.shopping_cart
    ADD COLUMN IF NOT EXISTS accommodation_longitude DOUBLE PRECISION NULL;

ALTER TABLE public.shopping_cart
    ADD COLUMN IF NOT EXISTS electronic_billing BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE public.shopping_cart
    ADD COLUMN IF NOT EXISTS billing_document_type VARCHAR(50) NULL;

ALTER TABLE public.shopping_cart
    ADD COLUMN IF NOT EXISTS billing_document_number VARCHAR(50) NULL;

ALTER TABLE public.shopping_cart
    ADD COLUMN IF NOT EXISTS billing_email VARCHAR(255) NULL;

ALTER TABLE public.shopping_cart
    ADD COLUMN IF NOT EXISTS billing_customer_name VARCHAR(255) NULL;

ALTER TABLE public.shopping_cart
    ADD COLUMN IF NOT EXISTS billing_phone VARCHAR(30) NULL;

COMMENT ON COLUMN public.tour.porcentaje_tourya IS
    'DEPRECATED: usar slot_porcentaje_tourya en tour_schedule_config_slot.';
