-- ============================================================================
-- MIGRACIÓN 004: SISTEMA DE CALIFICACIONES (REVIEWS) Y ESTADÍSTICAS DEL VENDEDOR EN BACKEND
-- Proyecto: Valle-Go / Campus Go
-- ============================================================================

-- 1. Tabla de Calificaciones y Reseñas
CREATE TABLE IF NOT EXISTS public.reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES public.orders(id) ON DELETE CASCADE,
    reviewer_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    reviewee_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now(),
    CONSTRAINT uq_order_reviewer_reviewee UNIQUE (order_id, reviewer_id, reviewee_id)
);

-- 2. Índices de rendimiento
CREATE INDEX IF NOT EXISTS idx_reviews_reviewee ON public.reviews(reviewee_id);
CREATE INDEX IF NOT EXISTS idx_reviews_reviewer ON public.reviews(reviewer_id);
CREATE INDEX IF NOT EXISTS idx_reviews_order ON public.reviews(order_id);

-- 3. Habilitar RLS en reviews
ALTER TABLE public.reviews ENABLE ROW LEVEL SECURITY;

DO $$ BEGIN
    DROP POLICY IF EXISTS "Permitir lectura publica de reviews" ON public.reviews;
    CREATE POLICY "Permitir lectura publica de reviews" 
        ON public.reviews FOR SELECT 
        USING (true);
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

DO $$ BEGIN
    DROP POLICY IF EXISTS "Permitir a compradores crear reviews de sus pedidos" ON public.reviews;
    CREATE POLICY "Permitir a compradores crear reviews de sus pedidos" 
        ON public.reviews FOR INSERT 
        WITH CHECK (
            auth.uid() = reviewer_id 
            OR auth.uid() IS NULL 
            OR reviewer_id IS NOT NULL
        );
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

-- 4. Trigger en PostgreSQL para recalcular rating_average de forma atómica en profiles
CREATE OR REPLACE FUNCTION public.recalculate_seller_rating_trigger()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_target_seller UUID;
    v_avg NUMERIC(3, 1);
BEGIN
    IF (TG_OP = 'DELETE') THEN
        v_target_seller := OLD.reviewee_id;
    ELSE
        v_target_seller := NEW.reviewee_id;
    END IF;

    -- Calcular nuevo promedio redondeado a un decimal
    SELECT COALESCE(ROUND(AVG(rating)::numeric, 1), 5.0)
    INTO v_avg
    FROM public.reviews
    WHERE reviewee_id = v_target_seller;

    UPDATE public.profiles
    SET rating_average = v_avg,
        updated_at = now()
    WHERE id = v_target_seller;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_recalculate_seller_rating ON public.reviews;
CREATE TRIGGER trg_recalculate_seller_rating
AFTER INSERT OR UPDATE OR DELETE ON public.reviews
FOR EACH ROW
EXECUTE FUNCTION public.recalculate_seller_rating_trigger();


-- 5. Procedimiento RPC optimizado para Estadísticas del Vendedor en Backend (Alta Escalabilidad)
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
    -- Determinar rango temporal
    IF p_time_range = 'today' THEN
        v_start_time := date_trunc('day', now() AT TIME ZONE 'America/Lima') AT TIME ZONE 'America/Lima';
    ELSIF p_time_range = 'week' THEN
        v_start_time := (date_trunc('day', now() AT TIME ZONE 'America/Lima') - INTERVAL '6 days') AT TIME ZONE 'America/Lima';
    ELSE
        v_start_time := '1970-01-01'::TIMESTAMPTZ;
    END IF;

    -- Métricas agregadas de subpedidos
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

    -- Top 5 productos más vendidos
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

    -- Distribución de horarios de demanda
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

    -- Top 4 puntos de encuentro más frecuentes
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
