-- Migración: Añadir columna supported_payment_methods a profiles
-- ValleGO Android v0.5.0-beta

ALTER TABLE public.profiles 
ADD COLUMN IF NOT EXISTS supported_payment_methods TEXT[] DEFAULT '{EFECTIVO,YAPE,PLIN}';

COMMENT ON COLUMN public.profiles.supported_payment_methods IS 'Métodos de pago aceptados por el vendedor: EFECTIVO, YAPE, PLIN';
