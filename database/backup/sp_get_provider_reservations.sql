-- DROP FUNCTION public.sp_get_provider_reservations(int4, int8, varchar);

CREATE OR REPLACE FUNCTION public.sp_get_provider_reservations(p_provider_id integer DEFAULT NULL::integer, p_reservation_id bigint DEFAULT NULL::bigint, p_delivery_status character varying DEFAULT NULL::character varying)
 RETURNS TABLE(reservationid bigint, reservationdate timestamp without time zone, reservationdeliverystatus character varying, reservationcreateddate timestamp without time zone, paymentid bigint, paymenttransactionid character varying, payername character varying, payeremail character varying, payerphone character varying, payerdocumenttype character varying, payerdocumentnumber character varying, shoppingitemid integer, shoppingtotalprice numeric, shoppingunitprice numeric, shoppingquantity integer, producttype character varying, productid integer, totaltourists bigint, tourid integer, tourname text, tourcategoryid integer, tourproviderid integer, tourscheduleid integer, scheduledate date, slotid integer, slottime_start time without time zone, slottime_end time without time zone, min_capacity integer, max_capacity integer, service_responsible_name character varying, service_responsible_email character varying, service_responsible_phone character varying)
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
        sci.quantity,

        sci.product_type,
        sci.product_id,

        -- TOTAL DE TURISTAS (SUM devuelve BIGINT ✔)
        COALESCE((
            SELECT SUM(scid.quantity)
            FROM shopping_cart_item_detail scid
            WHERE scid.shopping_cart_item_id = sci.id
        ), 0) AS totalTourists,

        t.id AS tourId,
        t.name AS tourName,
        t.category_id,
        t.provider_id,

        ts.id AS tourScheduleId,
        sci.schedule_date,

        tscs.id AS slotId,
        tscs.start_time,
        tscs.end_time,
        tscs.min_capacity,
        tscs.max_capacity,

        r.service_responsible_name,
        r.service_responsible_email,
        r.service_responsible_phone

    FROM reservation r
    JOIN shopping_cart_item sci ON sci.id = r.item_id
    LEFT JOIN payment p ON p.payment_id = r.payment_id
    LEFT JOIN tour_schedule ts ON ts.id = sci.tour_schedule_id
    LEFT JOIN tour_schedule_config_slot tscs ON tscs.id = sci.slot_id
    LEFT JOIN tour t ON t.id = sci.product_id

    WHERE
        (p_provider_id IS NULL OR t.provider_id = p_provider_id)
        AND (p_reservation_id IS NULL OR r.reservation_id = p_reservation_id)
        AND (p_delivery_status IS NULL OR r.delivery_status = p_delivery_status)

    GROUP BY
        r.reservation_id,
        p.payment_id,
        sci.id,
        t.id,
        ts.id,
        tscs.id;

END;
$function$
;
