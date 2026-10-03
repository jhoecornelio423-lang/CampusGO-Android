-- ============================================================================
-- MIGRACIÓN 015: EVIDENCIA EN INCIDENCIAS Y SOPORTE EN TIEMPO REAL
-- Proyecto: CampusGO
-- ============================================================================

-- 1. Agregar columna evidence_url a order_incidents si no existe
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' 
          AND table_name = 'order_incidents' 
          AND column_name = 'evidence_url'
    ) THEN
        ALTER TABLE public.order_incidents ADD COLUMN evidence_url TEXT;
    END IF;
END $$;

-- 2. Crear bucket support-evidences en Storage si no existe
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'support-evidences',
    'support-evidences',
    true,
    10485760, -- 10MB
    ARRAY['image/jpeg', 'image/png', 'image/webp', 'image/jpg']
)
ON CONFLICT (id) DO UPDATE SET 
    public = true,
    file_size_limit = 10485760,
    allowed_mime_types = ARRAY['image/jpeg', 'image/png', 'image/webp', 'image/jpg'];

-- 3. Habilitar Realtime para soporte
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables 
        WHERE pubname = 'supabase_realtime' AND tablename = 'support_tickets'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.support_tickets;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables 
        WHERE pubname = 'supabase_realtime' AND tablename = 'support_messages'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.support_messages;
    END IF;
END $$;

-- 4. Agregar columna attachment_url a support_messages si no existe
ALTER TABLE public.support_messages ADD COLUMN IF NOT EXISTS attachment_url TEXT;

