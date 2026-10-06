-- Migration: 20261005_004_fix_seller_orders_rls_and_suborders_buyer_id.sql
-- Description: Allow sellers to view parent orders of their suborders and add buyer_id to sub_orders

-- 1. Actualizar RLS en orders para que el vendedor pueda consultar los pedidos que le pertenecen o que contienen sus subpedidos
DROP POLICY IF EXISTS "Comprador ve sus ordenes" ON public.orders;
DROP POLICY IF EXISTS "Involucrados ven sus pedidos" ON public.orders;

CREATE POLICY "Involucrados ven sus pedidos" ON public.orders
    FOR SELECT TO authenticated
    USING (
        buyer_id = auth.uid()
        OR seller_id = auth.uid()
        OR EXISTS (
            SELECT 1 FROM public.sub_orders s
            WHERE s.order_id = orders.id AND s.seller_id = auth.uid()
        )
        OR is_admin()
    );

-- 2. Agregar columna buyer_id en sub_orders para acceso directo desnormalizado de alto rendimiento
ALTER TABLE public.sub_orders 
ADD COLUMN IF NOT EXISTS buyer_id UUID REFERENCES auth.users(id) ON DELETE CASCADE;

-- 3. Rellenar buyer_id en sub_orders existentes desde orders
UPDATE public.sub_orders s
SET buyer_id = o.buyer_id
FROM public.orders o
WHERE s.order_id = o.id AND s.buyer_id IS NULL;

-- 4. Trigger para autollenar buyer_id en sub_orders al crearse si no viene especificado
CREATE OR REPLACE FUNCTION public.fn_sync_sub_order_buyer_id()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.buyer_id IS NULL AND NEW.order_id IS NOT NULL THEN
        SELECT buyer_id INTO NEW.buyer_id FROM public.orders WHERE id = NEW.order_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_sync_sub_order_buyer_id ON public.sub_orders;
CREATE TRIGGER trg_sync_sub_order_buyer_id
    BEFORE INSERT ON public.sub_orders
    FOR EACH ROW EXECUTE FUNCTION public.fn_sync_sub_order_buyer_id();
