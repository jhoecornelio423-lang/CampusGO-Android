-- ============================================================================
-- MIGRACIÓN 016: PERMITIR REPORTES GENERALES SIN USUARIO REPORTADO
-- Proyecto: CampusGO
-- ============================================================================

ALTER TABLE public.order_incidents ALTER COLUMN reported_user_id DROP NOT NULL;
ALTER TABLE public.order_incidents ALTER COLUMN sub_order_id DROP NOT NULL;
