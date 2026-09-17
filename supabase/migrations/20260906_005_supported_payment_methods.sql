-- ============================================================================
-- MIGRACIÓN 005: SOPORTE DE MÉTODOS DE PAGO CONFIGURABLES POR VENDEDOR
-- Proyecto: Valle-Go / Campus Go
-- ============================================================================

-- 1. Asegurar columna supported_payment_methods en la tabla profiles
ALTER TABLE public.profiles 
ADD COLUMN IF NOT EXISTS supported_payment_methods TEXT[] 
DEFAULT ARRAY['EFECTIVO', 'YAPE', 'PLIN']::TEXT[];

-- 2. Asegurar que supported_meeting_points exista en caso de entornos no sincronizados
ALTER TABLE public.profiles 
ADD COLUMN IF NOT EXISTS supported_meeting_points TEXT[] 
DEFAULT ARRAY[]::TEXT[];

-- 3. Asignar valores por defecto a los perfiles existentes que tengan null
UPDATE public.profiles
SET supported_payment_methods = ARRAY['EFECTIVO', 'YAPE', 'PLIN']::TEXT[]
WHERE supported_payment_methods IS NULL;

-- 4. Comentarios informativos de documentación
COMMENT ON COLUMN public.profiles.supported_payment_methods IS 
'Métodos de pago aceptados por el vendedor (EFECTIVO, YAPE, PLIN). Utilizados para filtrar medios de pago en el carrito de compras.';
