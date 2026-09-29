-- Migración 013: Soporte para notificaciones Push con Firebase Cloud Messaging (FCM)
-- Agrega columna fcm_token a la tabla de perfiles de usuario

ALTER TABLE public.profiles 
ADD COLUMN IF NOT EXISTS fcm_token TEXT;

CREATE INDEX IF NOT EXISTS idx_profiles_fcm_token 
ON public.profiles(fcm_token);

COMMENT ON COLUMN public.profiles.fcm_token IS 'Token de registro de Firebase Cloud Messaging para notificaciones push en segundo plano';
