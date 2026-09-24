-- ============================================================================
-- MIGRACIÓN 010: ACCESO DE ADMINISTRADOR A PEDIDOS Y MÉTRICAS HISTÓRICAS DE CAMPUS
-- Proyecto: Valle-Go
-- ============================================================================

-- 1. Políticas RLS para que usuarios con rol 'admin' puedan auditar y ver todos los pedidos
DROP POLICY IF EXISTS "Admin puede ver todos los pedidos" ON public.orders;
CREATE POLICY "Admin puede ver todos los pedidos" ON public.orders
    FOR SELECT TO authenticated
    USING (public.is_admin());

DROP POLICY IF EXISTS "Admin puede ver todos los subpedidos" ON public.sub_orders;
CREATE POLICY "Admin puede ver todos los subpedidos" ON public.sub_orders
    FOR SELECT TO authenticated
    USING (public.is_admin());

DROP POLICY IF EXISTS "Admin puede ver todos los items" ON public.order_items;
CREATE POLICY "Admin puede ver todos los items" ON public.order_items
    FOR SELECT TO authenticated
    USING (public.is_admin());

-- 2. Función RPC para obtener métricas y telemetría ejecutiva del campus por período
CREATE OR REPLACE FUNCTION public.get_campus_admin_metrics(
    p_period TEXT DEFAULT 'all'
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_start_time TIMESTAMPTZ;
    v_total_sales NUMERIC(10,2) := 0.00;
    v_total_orders INT := 0;
    v_completed_orders INT := 0;
    v_cancelled_orders INT := 0;
    v_average_ticket NUMERIC(10,2) := 0.00;
    v_fulfillment_rate NUMERIC(5,2) := 100.00;
    v_seller_rankings JSONB := '[]'::JSONB;
    v_payment_methods JSONB := '[]'::JSONB;
    v_top_points JSONB := '[]'::JSONB;
BEGIN
    -- Verificar privilegios de administrador
    IF NOT public.is_admin() THEN
        RAISE EXCEPTION 'FORBIDDEN: Se requiere rol de administrador para consultar métricas ejecutivas.';
    END IF;

    -- Determinar rango de tiempo
    IF p_period = 'today' THEN
        v_start_time := date_trunc('day', now() AT TIME ZONE 'America/Lima') AT TIME ZONE 'America/Lima';
    ELSIF p_period = 'week' THEN
        v_start_time := now() - INTERVAL '7 days';
    ELSIF p_period = 'month' THEN
        v_start_time := now() - INTERVAL '30 days';
    ELSE
        v_start_time := '2000-01-01 00:00:00+00'::TIMESTAMPTZ;
    END IF;

    -- Resumen general de subpedidos
    SELECT 
        COALESCE(SUM(CASE WHEN s.status IN ('completed', 'pago_confirmado') THEN s.subtotal_amount ELSE 0 END), 0.00),
        COUNT(*),
        COUNT(CASE WHEN s.status IN ('completed', 'pago_confirmado') THEN 1 END),
        COUNT(CASE WHEN s.status IN ('cancelled', 'rejected', 'no_entregado') THEN 1 END)
    INTO 
        v_total_sales,
        v_total_orders,
        v_completed_orders,
        v_cancelled_orders
    FROM public.sub_orders s
    WHERE s.created_at >= v_start_time;

    -- Calcular ticket promedio y tasa de éxito
    IF v_completed_orders > 0 THEN
        v_average_ticket := ROUND(v_total_sales / v_completed_orders, 2);
    END IF;

    IF (v_completed_orders + v_cancelled_orders) > 0 THEN
        v_fulfillment_rate := ROUND((v_completed_orders::NUMERIC * 100.0) / (v_completed_orders + v_cancelled_orders)::NUMERIC, 1);
    END IF;

    -- Ranking de ventas por puesto / vendedor
    SELECT COALESCE(jsonb_agg(r), '[]'::JSONB)
    INTO v_seller_rankings
    FROM (
        SELECT 
            s.seller_id,
            COALESCE(p.business_name, p.full_name, 'Puesto Universitario') AS store_name,
            COALESCE(p.full_name, 'Titular') AS owner_name,
            p.avatar_url,
            COALESCE(SUM(CASE WHEN s.status IN ('completed', 'pago_confirmado') THEN s.subtotal_amount ELSE 0 END), 0.00) AS total_sales,
            COUNT(CASE WHEN s.status IN ('completed', 'pago_confirmado') THEN 1 END) AS completed_orders,
            CASE 
                WHEN v_total_sales > 0 THEN ROUND((COALESCE(SUM(CASE WHEN s.status IN ('completed', 'pago_confirmado') THEN s.subtotal_amount ELSE 0 END), 0.00) * 100.0) / v_total_sales, 1)
                ELSE 0.0
            END AS percentage
        FROM public.sub_orders s
        LEFT JOIN public.profiles p ON p.id = s.seller_id
        WHERE s.created_at >= v_start_time
        GROUP BY s.seller_id, p.business_name, p.full_name, p.avatar_url
        ORDER BY total_sales DESC, completed_orders DESC
        LIMIT 20
    ) r;

    -- Distribución por métodos de pago
    SELECT COALESCE(jsonb_agg(pm), '[]'::JSONB)
    INTO v_payment_methods
    FROM (
        SELECT 
            UPPER(COALESCE(s.payment_method, 'EFECTIVO')) AS method,
            COUNT(*) AS count,
            COALESCE(SUM(s.subtotal_amount), 0.00) AS total_amount,
            CASE 
                WHEN v_total_orders > 0 THEN ROUND((COUNT(*)::NUMERIC * 100.0) / v_total_orders::NUMERIC, 1)
                ELSE 0.0
            END AS percentage
        FROM public.sub_orders s
        WHERE s.created_at >= v_start_time
        GROUP BY UPPER(COALESCE(s.payment_method, 'EFECTIVO'))
        ORDER BY count DESC
    ) pm;

    -- Puntos de entrega oficiales con más tráfico
    SELECT COALESCE(jsonb_agg(pt), '[]'::JSONB)
    INTO v_top_points
    FROM (
        SELECT 
            COALESCE(o.meeting_point_name, o.delivery_place, 'Campus General') AS point_name,
            COUNT(*) AS count,
            CASE 
                WHEN v_total_orders > 0 THEN ROUND((COUNT(*)::NUMERIC * 100.0) / v_total_orders::NUMERIC, 1)
                ELSE 0.0
            END AS percentage
        FROM public.sub_orders s
        JOIN public.orders o ON o.id = s.order_id
        WHERE s.created_at >= v_start_time
        GROUP BY COALESCE(o.meeting_point_name, o.delivery_place, 'Campus General')
        ORDER BY count DESC
        LIMIT 6
    ) pt;

    RETURN jsonb_build_object(
        'period', p_period,
        'total_sales', v_total_sales,
        'total_orders', v_total_orders,
        'completed_orders', v_completed_orders,
        'cancelled_orders', v_cancelled_orders,
        'average_ticket', v_average_ticket,
        'fulfillment_rate', v_fulfillment_rate,
        'seller_rankings', v_seller_rankings,
        'payment_methods', v_payment_methods,
        'top_meeting_points', v_top_points
    );
END;
$$;
