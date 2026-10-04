-- Habilitar publicaciones Realtime para order_incidents y seller_applications
ALTER PUBLICATION supabase_realtime ADD TABLE public.order_incidents, public.seller_applications;
