-- Migration: Add missing delivery and meeting point columns to sub_orders
ALTER TABLE public.sub_orders ADD COLUMN IF NOT EXISTS meeting_point_id UUID;
ALTER TABLE public.sub_orders ADD COLUMN IF NOT EXISTS meeting_point_name VARCHAR(255);
ALTER TABLE public.sub_orders ADD COLUMN IF NOT EXISTS scheduled_time VARCHAR(100);
ALTER TABLE public.sub_orders ADD COLUMN IF NOT EXISTS delivery_code VARCHAR(10);
