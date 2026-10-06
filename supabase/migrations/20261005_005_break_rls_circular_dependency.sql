-- Migration: 20261005_005_break_rls_circular_dependency.sql
-- Description: Break RLS circular dependency between orders and sub_orders using SECURITY DEFINER functions

CREATE OR REPLACE FUNCTION public.is_seller_of_order(p_order_id UUID, p_user_id UUID)
RETURNS BOOLEAN
LANGUAGE sql
SECURITY DEFINER
STABLE
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.sub_orders 
        WHERE order_id = p_order_id AND seller_id = p_user_id
    );
$$;

CREATE OR REPLACE FUNCTION public.is_buyer_of_order(p_order_id UUID, p_user_id UUID)
RETURNS BOOLEAN
LANGUAGE sql
SECURITY DEFINER
STABLE
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.orders 
        WHERE id = p_order_id AND buyer_id = p_user_id
    );
$$;

DROP POLICY IF EXISTS "Involucrados ven sus pedidos" ON public.orders;
DROP POLICY IF EXISTS "Comprador ve sus ordenes" ON public.orders;

CREATE POLICY "Involucrados ven sus pedidos" ON public.orders
    FOR SELECT TO authenticated
    USING (
        buyer_id = auth.uid()
        OR seller_id = auth.uid()
        OR is_seller_of_order(id, auth.uid())
        OR is_admin()
    );

DROP POLICY IF EXISTS "Comprador puede ver sus subpedidos" ON public.sub_orders;

CREATE POLICY "Comprador puede ver sus subpedidos" ON public.sub_orders
    FOR SELECT TO authenticated
    USING (
        buyer_id = auth.uid()
        OR is_buyer_of_order(order_id, auth.uid())
        OR seller_id = auth.uid()
        OR is_admin()
    );
