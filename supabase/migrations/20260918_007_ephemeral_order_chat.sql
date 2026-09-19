-- ==============================================================================
-- Migración: Chat Temporal Efímero entre Comprador y Vendedor por Subpedido
-- Archivo: 20260918_007_ephemeral_order_chat.sql
-- ==============================================================================

-- 1. Tabla de mensajes vinculada al subpedido
CREATE TABLE IF NOT EXISTS public.order_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sub_order_id UUID NOT NULL REFERENCES public.sub_orders(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    receiver_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_read BOOLEAN NOT NULL DEFAULT false
);

-- 2. Habilitar Seguridad por Fila (Row Level Security - RLS)
ALTER TABLE public.order_messages ENABLE ROW LEVEL SECURITY;

-- 3. Políticas de acceso (lectura, inserción y actualización de leído)
DROP POLICY IF EXISTS "order_messages_select_policy" ON public.order_messages;
CREATE POLICY "order_messages_select_policy"
    ON public.order_messages
    FOR SELECT
    USING (auth.uid() = sender_id OR auth.uid() = receiver_id);

DROP POLICY IF EXISTS "order_messages_insert_policy" ON public.order_messages;
CREATE POLICY "order_messages_insert_policy"
    ON public.order_messages
    FOR INSERT
    WITH CHECK (auth.uid() = sender_id);

DROP POLICY IF EXISTS "order_messages_update_policy" ON public.order_messages;
CREATE POLICY "order_messages_update_policy"
    ON public.order_messages
    FOR UPDATE
    USING (auth.uid() = receiver_id)
    WITH CHECK (auth.uid() = receiver_id);

-- 4. Índice para optimizar consultas de mensajes ordenados por fecha
CREATE INDEX IF NOT EXISTS idx_order_messages_suborder_created
    ON public.order_messages(sub_order_id, created_at ASC);

-- 5. Habilitar Supabase Realtime para la tabla de mensajes
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables 
        WHERE pubname = 'supabase_realtime' AND tablename = 'order_messages'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.order_messages;
    END IF;
END $$;

-- 6. Función y Trigger de Autodestrucción Automática
-- Al completarse o cancelarse un subpedido, todos sus mensajes se eliminan automáticamente
-- para proteger la privacidad y mantener cero consumo de almacenamiento innecesario.
CREATE OR REPLACE FUNCTION public.cleanup_order_messages_on_finish()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status IN ('completed', 'cancelled', 'rejected', 'not_delivered') THEN
        DELETE FROM public.order_messages WHERE sub_order_id = NEW.id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS trigger_cleanup_order_messages ON public.sub_orders;

CREATE TRIGGER trigger_cleanup_order_messages
    AFTER UPDATE OF status ON public.sub_orders
    FOR EACH ROW
    EXECUTE FUNCTION public.cleanup_order_messages_on_finish();
