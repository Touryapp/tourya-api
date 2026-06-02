-- Filtro por cliente (user_id del carrito), columna providerPrice y fix de tipos en listados
DROP FUNCTION IF EXISTS public.sp_get_provider_reservations(int4, int8, varchar, varchar);

CREATE OR REPLACE FUNCTION public.sp_get_provider_reservations(
    p_provider_id integer DEFAULT NULL,
    p_customer_user_id integer DEFAULT NULL,
    p_reservation_id bigint DEFAULT NULL,
    p_delivery_status character varying DEFAULT NULL,
    p_sub_category character varying DEFAULT NULL
)
RETURNS TABLE(
    reservationid bigint,
    reservationdate timestamp without time zone,
    reservationdeliverystatus character varying,
    reservationcreateddate timestamp without time zone,
    paymentid bigint,
    paymenttransactionid character varying,
    payername character varying,
    payeremail character varying,
    payerphone character varying,
    payerdocumenttype character varying,
    payerdocumentnumber character varying,
    shoppingitemid integer,
    shoppingtotalprice numeric,
    shoppingunitprice numeric,
    providerprice numeric,
    shoppingquantity integer,
    producttype character varying,
    productid integer,
    totaltourists bigint,
    tourid integer,
    tourname jsonb,
    tourcategoryid integer,
    tourproviderid integer,
    toursubcategory character varying,
    tourscheduleid integer,
    scheduledate date,
    slotid integer,
    slottime_start time without time zone,
    slottime_end time without time zone,
    service_responsible_name character varying,
    service_responsible_email character varying,
    service_responsible_phone character varying,
    max_cancellation_date date,
    max_rescheduling_date date,
    cancellation_reason character varying,
    cancellation_date timestamp without time zone
)
LANGUAGE plpgsql
AS $function$
BEGIN
    RETURN QUERY
    SELECT
        r.reservation_id,
        r.reservation_date,
        r.delivery_status,
        r.created_date,
        p.payment_id,
        p.transaction_id,
        p.payer_name,
        p.payer_email,
        p.payer_phone,
        p.payer_document_type,
        p.payer_document_number,
        sci.id AS shoppingItemId,
        sci.total_price,
        sci.unit_price,
        COALESCE((
            SELECT SUM(COALESCE(scid.provider_unit_price, 0) * COALESCE(scid.quantity, 0))
            FROM shopping_cart_item_detail scid
            WHERE scid.shopping_cart_item_id = sci.id
        ), 0)::numeric AS providerPrice,
        sci.quantity,
        sci.product_type,
        sci.product_id,
        COALESCE((
            SELECT SUM(scid.quantity)
            FROM shopping_cart_item_detail scid
            WHERE scid.shopping_cart_item_id = sci.id
        ), 0) AS totalTourists,
        t.id AS tourId,
        t.name AS tourName,
        t.category_id,
        t.provider_id,
        t.sub_category::character varying AS tourSubcategory,
        ts.id AS tourScheduleId,
        sci.schedule_date,
        tscs.id AS slotId,
        tscs.start_time,
        tscs.end_time,
        r.service_responsible_name,
        r.service_responsible_email,
        r.service_responsible_phone,
        r.max_cancellation_date,
        r.max_rescheduling_date,
        r.cancellation_reason,
        r.cancellation_date
    FROM reservation r
    JOIN shopping_cart_item sci ON sci.id = r.item_id
    JOIN shopping_cart sc ON sc.id = sci.shopping_cart_id
    LEFT JOIN payment p ON p.payment_id = r.payment_id
    LEFT JOIN tour_schedule ts ON ts.id = sci.tour_schedule_id
    LEFT JOIN tour_schedule_config_slot tscs ON tscs.id = sci.slot_id
    LEFT JOIN tour t ON t.id = sci.product_id
    WHERE
        (p_provider_id IS NULL OR t.provider_id = p_provider_id)
        AND (p_customer_user_id IS NULL OR sc.user_id = p_customer_user_id)
        AND (p_reservation_id IS NULL OR r.reservation_id = p_reservation_id)
        AND (p_delivery_status IS NULL OR r.delivery_status = p_delivery_status)
        AND (p_sub_category IS NULL OR t.sub_category::text = p_sub_category)
    GROUP BY
        r.reservation_id,
        p.payment_id,
        sci.id,
        sc.id,
        t.id,
        ts.id,
        tscs.id;
END;
$function$;
