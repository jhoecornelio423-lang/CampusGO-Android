-- ============================================================================
-- MIGRACIÓN 009: ESTADO PENDIENTE POR DEFECTO Y DISPARADOR SELLER_APPLICATIONS
-- Proyecto: Valle-Go
-- ============================================================================

-- 1. Actualizar el valor por defecto de business_status a 'PENDIENTE'
ALTER TABLE public.profiles 
ALTER COLUMN business_status SET DEFAULT 'PENDIENTE';

-- 2. Actualizar el trigger handle_new_user para registrar como PENDIENTE y crear seller_applications
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    user_role_val public.user_role;
    is_seller boolean;
    b_status varchar;
BEGIN
    user_role_val := (COALESCE(NEW.raw_user_meta_data->>'role', 'comprador'))::public.user_role;
    is_seller := (user_role_val = 'emprendedor'::public.user_role OR NEW.raw_user_meta_data->>'business_name' IS NOT NULL);
    
    b_status := CASE 
        WHEN is_seller THEN 'PENDIENTE'
        ELSE 'ABIERTO'
    END;

    INSERT INTO public.profiles (
        id, 
        full_name, 
        phone, 
        role, 
        campus, 
        rating_average,
        business_name, 
        business_status, 
        business_category,
        business_description, 
        accepting_orders, 
        supported_meeting_points
    )
    VALUES (
        NEW.id,
        COALESCE(NEW.raw_user_meta_data->>'full_name', 'Estudiante UCV'),
        COALESCE(NEW.raw_user_meta_data->>'phone', ''),
        user_role_val,
        COALESCE(NEW.raw_user_meta_data->>'campus', 'Campus Central'),
        0.00,
        NEW.raw_user_meta_data->>'business_name',
        b_status,
        NEW.raw_user_meta_data->>'business_category',
        NEW.raw_user_meta_data->>'business_description',
        NOT is_seller,
        CASE 
            WHEN NEW.raw_user_meta_data->>'meeting_point' IS NOT NULL 
            THEN ARRAY[NEW.raw_user_meta_data->>'meeting_point']::text[]
            ELSE '{}'::text[]
        END
    )
    ON CONFLICT (id) DO UPDATE SET
        role = EXCLUDED.role,
        business_name = COALESCE(EXCLUDED.business_name, public.profiles.business_name),
        business_status = COALESCE(EXCLUDED.business_status, public.profiles.business_status),
        accepting_orders = EXCLUDED.accepting_orders;

    -- Si es vendedor, registrar automáticamente su postulación en seller_applications
    IF is_seller THEN
        INSERT INTO public.seller_applications (
            id, 
            user_id, 
            dni, 
            open_time, 
            close_time, 
            full_name,
            phone, 
            business_name, 
            business_category, 
            description,
            proposed_location, 
            status
        ) VALUES (
            gen_random_uuid(), 
            NEW.id, 
            '', 
            '08:00:00'::time, 
            '20:00:00'::time,
            COALESCE(NEW.raw_user_meta_data->>'full_name', 'Estudiante UCV'),
            COALESCE(NEW.raw_user_meta_data->>'phone', ''),
            COALESCE(NEW.raw_user_meta_data->>'business_name', 'Mi Tienda'),
            COALESCE(NEW.raw_user_meta_data->>'business_category', 'Varios'),
            COALESCE(NEW.raw_user_meta_data->>'business_description', ''),
            NEW.raw_user_meta_data->>'meeting_point',
            'pending'
        )
        ON CONFLICT DO NOTHING;
    END IF;

    RETURN NEW;
END;
$$;
