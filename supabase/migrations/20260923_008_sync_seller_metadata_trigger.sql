-- ============================================================================
-- MIGRACIÓN 008: SINCRONIZAR METADATOS DE VENDEDOR EN TRIGGER DE AUTH
-- Proyecto: Valle-Go
-- ============================================================================

CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
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
        supported_meeting_points
    )
    VALUES (
        NEW.id,
        COALESCE(NEW.raw_user_meta_data->>'full_name', 'Estudiante UCV'),
        COALESCE(NEW.raw_user_meta_data->>'phone', ''),
        (COALESCE(NEW.raw_user_meta_data->>'role', 'comprador'))::public.user_role,
        COALESCE(NEW.raw_user_meta_data->>'campus', 'Campus Central'),
        0.00,
        NEW.raw_user_meta_data->>'business_name',
        COALESCE(NEW.raw_user_meta_data->>'business_status', 'ABIERTO'),
        NEW.raw_user_meta_data->>'business_category',
        NEW.raw_user_meta_data->>'business_description',
        CASE 
            WHEN NEW.raw_user_meta_data->>'meeting_point' IS NOT NULL 
            THEN ARRAY[NEW.raw_user_meta_data->>'meeting_point']
            ELSE '{}'::text[]
        END
    );
    RETURN NEW;
END;
$$;
