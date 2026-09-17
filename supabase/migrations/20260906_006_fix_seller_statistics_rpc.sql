-- Migración: Corregir función get_seller_dashboard_statistics en Supabase
-- Corrige los nombres de columnas de order_items (usando products.name y price_at_sale)
-- y agrega fallback de puntos de encuentro desde la orden principal.

CREATE OR REPLACE FUNCTION public.get_seller_dashboard_statistics(
    p_seller_id UUID,
    p_time_range TEXT DEFAULT 'all'
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_start_time TIMESTAMPTZ;
    v_total_earnings NUMERIC := 0;
    v_completed_count INT := 0;
    v_cancelled_count INT := 0;
    v_in_progress_count INT := 0;
    v_total_count INT := 0;
    v_avg_ticket NUMERIC := 0;
    v_top_products JSONB;
    v_hourly_distribution JSONB;
    v_top_meeting_points JSONB;
BEGIN
    -- 1. Determinar rango temporal según timezone de Perú
    IF p_time_range = 'today' THEN
        v_start_time := date_trunc('day', now() AT TIME ZONE 'America/Lima') AT TIME ZONE 'America/Lima';
    ELSIF p_time_range = 'week' THEN
        v_start_time := (date_trunc('day', now() AT TIME ZONE 'America/Lima') - INTERVAL '6 days') AT TIME ZONE 'America/Lima';
    ELSE
        v_start_time := '1970-01-01'::TIMESTAMPTZ;
    END IF;

    -- 2. Métricas agregadas de subpedidos
    SELECT
        COALESCE(SUM(CASE WHEN status IN ('completed', 'payment_confirmed') THEN subtotal_amount ELSE 0 END), 0),
        COALESCE(COUNT(CASE WHEN status IN ('completed', 'payment_confirmed') THEN 1 END), 0),
        COALESCE(COUNT(CASE WHEN status IN ('cancelled', 'rejected', 'not_delivered') THEN 1 END), 0),
        COALESCE(COUNT(CASE WHEN status IN ('pending', 'accepted', 'in_preparation', 'ready', 'waiting_delivery') THEN 1 END), 0),
        COUNT(*)
    INTO
        v_total_earnings,
        v_completed_count,
        v_cancelled_count,
        v_in_progress_count,
        v_total_count
    FROM public.sub_orders
    WHERE seller_id = p_seller_id
      AND created_at >= v_start_time;

    IF v_completed_count > 0 THEN
        v_avg_ticket := ROUND((v_total_earnings / v_completed_count)::numeric, 2);
    END IF;

    -- 3. Top 5 productos más vendidos (vinculando con products y price_at_sale)
    SELECT COALESCE(jsonb_agg(sub), '[]'::jsonb)
    INTO v_top_products
    FROM (
        SELECT 
            COALESCE(p.name, 'Producto') AS product_name,
            SUM(oi.quantity)::INT AS units_sold,
            ROUND(SUM(oi.price_at_sale * oi.quantity)::NUMERIC, 2) AS total_amount
        FROM public.order_items oi
        JOIN public.sub_orders so ON (so.id = oi.sub_order_id OR so.order_id = oi.order_id)
        LEFT JOIN public.products p ON p.id = oi.product_id
        WHERE so.seller_id = p_seller_id
          AND so.status IN ('completed', 'payment_confirmed')
          AND so.created_at >= v_start_time
        GROUP BY COALESCE(p.name, 'Producto')
        ORDER BY total_amount DESC
        LIMIT 5
    ) sub;

    -- 4. Distribución horaria de demanda
    SELECT COALESCE(jsonb_agg(h), '[]'::jsonb)
    INTO v_hourly_distribution
    FROM (
        SELECT
            CASE 
                WHEN EXTRACT(HOUR FROM (created_at AT TIME ZONE 'America/Lima')) BETWEEN 8 AND 11 THEN 'morning'
                WHEN EXTRACT(HOUR FROM (created_at AT TIME ZONE 'America/Lima')) BETWEEN 12 AND 14 THEN 'lunch'
                WHEN EXTRACT(HOUR FROM (created_at AT TIME ZONE 'America/Lima')) BETWEEN 15 AND 17 THEN 'afternoon'
                ELSE 'night'
            END AS slot,
            COUNT(*)::INT AS order_count
        FROM public.sub_orders
        WHERE seller_id = p_seller_id
          AND created_at >= v_start_time
        GROUP BY slot
    ) h;

    -- 5. Top 4 puntos de encuentro más frecuentes
    SELECT COALESCE(jsonb_agg(p), '[]'::jsonb)
    INTO v_top_meeting_points
    FROM (
        SELECT 
            COALESCE(so.meeting_point_name, o.meeting_point_name, o.delivery_place, 'Punto por acordar') AS point_name,
            COUNT(*)::INT AS delivery_count
        FROM public.sub_orders so
        LEFT JOIN public.orders o ON o.id = so.order_id
        WHERE so.seller_id = p_seller_id
          AND so.created_at >= v_start_time
        GROUP BY point_name
        ORDER BY delivery_count DESC
        LIMIT 4
    ) p;

    RETURN jsonb_build_object(
        'total_earnings', v_total_earnings,
        'completed_count', v_completed_count,
        'cancelled_count', v_cancelled_count,
        'in_progress_count', v_in_progress_count,
        'total_orders_count', v_total_count,
        'average_ticket', v_avg_ticket,
        'top_products', v_top_products,
        'hourly_distribution', v_hourly_distribution,
        'top_meeting_points', v_top_meeting_points
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.get_seller_dashboard_statistics(UUID, TEXT) TO authenticated, anon, service_role;
