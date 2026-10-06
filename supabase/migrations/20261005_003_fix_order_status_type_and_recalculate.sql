-- Migration: Create order_status enum type and eliminate fragile cast in recalculate_master_order
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'order_status') THEN
        CREATE TYPE public.order_status AS ENUM (
            'pending',
            'accepted',
            'preparing',
            'ready',
            'completed',
            'rejected',
            'cancelled'
        );
    END IF;
END $$;

CREATE OR REPLACE FUNCTION public.recalculate_master_order(p_order_id uuid)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
AS $function$
DECLARE
    v_total NUMERIC(10, 2) := 0;
    v_all_count INT := 0;
    v_rejected_count INT := 0;
    v_completed_count INT := 0;
    v_in_progress_count INT := 0;
    v_pending_count INT := 0;
    v_new_status VARCHAR(50);
BEGIN
    SELECT 
        COUNT(*),
        COUNT(*) FILTER (WHERE status IN ('rejected', 'cancelled')),
        COUNT(*) FILTER (WHERE status = 'completed'),
        COUNT(*) FILTER (WHERE status IN ('accepted', 'preparing', 'ready', 'waiting_delivery', 'payment_confirmed')),
        COUNT(*) FILTER (WHERE status = 'pending'),
        COALESCE(SUM(CASE WHEN status NOT IN ('rejected', 'cancelled') THEN subtotal_amount ELSE 0 END), 0)
    INTO
        v_all_count,
        v_rejected_count,
        v_completed_count,
        v_in_progress_count,
        v_pending_count,
        v_total
    FROM public.sub_orders
    WHERE order_id = p_order_id;

    IF v_all_count = 0 THEN
        RETURN;
    END IF;

    IF v_all_count = v_rejected_count THEN
        v_new_status := 'cancelled';
    ELSIF v_completed_count > 0 AND (v_completed_count + v_rejected_count = v_all_count) THEN
        v_new_status := 'completed';
    ELSIF v_in_progress_count > 0 OR v_completed_count > 0 THEN
        v_new_status := 'preparing';
    ELSE
        v_new_status := 'pending';
    END IF;

    UPDATE public.orders
    SET total_price = v_total,
        status = v_new_status,
        updated_at = now()
    WHERE id = p_order_id;
END;
$function$;
