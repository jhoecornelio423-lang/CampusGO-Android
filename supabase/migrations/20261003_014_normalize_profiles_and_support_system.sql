-- ==============================================================================
-- MIGRACIÓN 014: NORMALIZACIÓN ARQUITECTÓNICA DE BASE DE DATOS Y SISTEMA DE SOPORTE
-- Proyecto: CampusGO
-- Fecha: 03 de Octubre de 2026
-- ==============================================================================

-- 1. ELIMINAR TABLAS OBSOLETAS
DROP TABLE IF EXISTS public.product_reports CASCADE;
DROP TABLE IF EXISTS public.push_tokens CASCADE;
DROP TABLE IF EXISTS public.product_images CASCADE;
DROP TABLE IF EXISTS public.support_events CASCADE;
DROP TABLE IF EXISTS public.support_messages CASCADE;
DROP TABLE IF EXISTS public.support_tickets CASCADE;

-- 2. TABLA NORMALIZADA: user_profiles (LA PERSONA / ESTUDIANTE)
-- Se retira student_code. Se agrega email, universidad y sede.
CREATE TABLE IF NOT EXISTS public.user_profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name VARCHAR(255) NOT NULL CHECK (char_length(trim(full_name)) >= 2),
    email VARCHAR(255) NOT NULL CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    phone VARCHAR(30) DEFAULT '',
    avatar_url TEXT,
    university VARCHAR(100) NOT NULL DEFAULT 'UCV',
    campus VARCHAR(100) NOT NULL DEFAULT 'Los Olivos',
    role public.user_role NOT NULL DEFAULT 'comprador',
    fcm_token TEXT,
    push_notifications_enabled BOOLEAN NOT NULL DEFAULT true,
    is_suspended BOOLEAN NOT NULL DEFAULT false,
    suspension_reason TEXT DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_user_profiles_email ON public.user_profiles(email);
CREATE INDEX IF NOT EXISTS idx_user_profiles_university_campus ON public.user_profiles(university, campus);
CREATE INDEX IF NOT EXISTS idx_user_profiles_role ON public.user_profiles(role);

ALTER TABLE public.user_profiles ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Lectura publica de perfiles de usuario" ON public.user_profiles;
CREATE POLICY "Lectura publica de perfiles de usuario" ON public.user_profiles FOR SELECT USING (true);
DROP POLICY IF EXISTS "Usuarios editan su propio perfil" ON public.user_profiles;
CREATE POLICY "Usuarios editan su propio perfil" ON public.user_profiles FOR UPDATE USING (auth.uid() = id);
DROP POLICY IF EXISTS "Usuarios insertan su propio perfil" ON public.user_profiles;
CREATE POLICY "Usuarios insertan su propio perfil" ON public.user_profiles FOR INSERT WITH CHECK (auth.uid() = id OR auth.uid() IS NULL);

-- 3. TABLA NORMALIZADA: seller_stores (EL PUESTO / TIENDA COMERCIAL)
CREATE TABLE IF NOT EXISTS public.seller_stores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id UUID UNIQUE NOT NULL REFERENCES public.user_profiles(id) ON DELETE CASCADE,
    business_name VARCHAR(255) NOT NULL CHECK (char_length(trim(business_name)) >= 2),
    business_category VARCHAR(100) NOT NULL DEFAULT 'General',
    business_description TEXT,
    business_location VARCHAR(255),
    open_time VARCHAR(10) NOT NULL DEFAULT '08:00',
    close_time VARCHAR(10) NOT NULL DEFAULT '18:00',
    banner_url TEXT,
    logo_url TEXT,
    rating_average NUMERIC(3, 2) NOT NULL DEFAULT 5.00 CHECK (rating_average >= 0.00 AND rating_average <= 5.00),
    review_count INTEGER NOT NULL DEFAULT 0 CHECK (review_count >= 0),
    accepting_orders BOOLEAN NOT NULL DEFAULT true,
    show_in_explore BOOLEAN NOT NULL DEFAULT true,
    business_status VARCHAR(50) NOT NULL DEFAULT 'PENDIENTE' CHECK (business_status IN ('PENDIENTE', 'ABIERTO', 'CERRADO', 'SUSPENDIDO', 'APROBADO')),
    supported_meeting_points TEXT[] NOT NULL DEFAULT '{}',
    supported_payment_methods TEXT[] NOT NULL DEFAULT '{EFECTIVO,YAPE,PLIN}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_seller_stores_seller_id ON public.seller_stores(seller_id);
CREATE INDEX IF NOT EXISTS idx_seller_stores_status_explore ON public.seller_stores(business_status, show_in_explore);
CREATE INDEX IF NOT EXISTS idx_seller_stores_category ON public.seller_stores(business_category);

ALTER TABLE public.seller_stores ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Lectura publica de tiendas" ON public.seller_stores;
CREATE POLICY "Lectura publica de tiendas" ON public.seller_stores FOR SELECT USING (true);
DROP POLICY IF EXISTS "Vendedor edita su propia tienda" ON public.seller_stores;
CREATE POLICY "Vendedor edita su propia tienda" ON public.seller_stores FOR UPDATE USING (auth.uid() = seller_id);
DROP POLICY IF EXISTS "Vendedor inserta su propia tienda" ON public.seller_stores;
CREATE POLICY "Vendedor inserta su propia tienda" ON public.seller_stores FOR INSERT WITH CHECK (auth.uid() = seller_id OR auth.uid() IS NULL);

-- 4. PUNTOS DE ENCUENTRO MULTI-UNIVERSIDAD
ALTER TABLE public.campus_meeting_points 
    ADD COLUMN IF NOT EXISTS university VARCHAR(100) NOT NULL DEFAULT 'UCV';
CREATE INDEX IF NOT EXISTS idx_campus_meeting_points_uni_campus 
    ON public.campus_meeting_points(university, campus, is_active);

-- 5. TABLAS DE SOPORTE Y CHAT CON EL ADMINISTRADOR (AUDITORÍA PERMANENTE)
CREATE TABLE IF NOT EXISTS public.support_tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_number SERIAL,
    user_id UUID NOT NULL REFERENCES public.user_profiles(id) ON DELETE CASCADE,
    incident_id UUID REFERENCES public.order_incidents(id) ON DELETE SET NULL,
    subject VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ABIERTO' CHECK (status IN ('ABIERTO', 'EN_PROCESO', 'RESUELTO', 'CERRADO')),
    assigned_admin_id UUID REFERENCES public.user_profiles(id) ON DELETE SET NULL,
    admin_notes TEXT,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_support_tickets_user_status ON public.support_tickets(user_id, status);

CREATE TABLE IF NOT EXISTS public.support_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL REFERENCES public.support_tickets(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES public.user_profiles(id) ON DELETE CASCADE,
    message TEXT NOT NULL CHECK (char_length(trim(message)) > 0),
    is_admin BOOLEAN NOT NULL DEFAULT false,
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_support_messages_ticket ON public.support_messages(ticket_id, created_at ASC);

ALTER TABLE public.support_tickets ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.support_messages ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Admin acceso total a tickets" ON public.support_tickets;
CREATE POLICY "Admin acceso total a tickets" ON public.support_tickets 
    FOR ALL TO authenticated USING (public.is_admin());

DROP POLICY IF EXISTS "Usuario ve sus tickets no resueltos" ON public.support_tickets;
CREATE POLICY "Usuario ve sus tickets no resueltos" ON public.support_tickets 
    FOR SELECT TO authenticated 
    USING (user_id = auth.uid() AND status NOT IN ('RESUELTO', 'CERRADO'));

DROP POLICY IF EXISTS "Usuario crea sus propios tickets" ON public.support_tickets;
CREATE POLICY "Usuario crea sus propios tickets" ON public.support_tickets 
    FOR INSERT TO authenticated 
    WITH CHECK (user_id = auth.uid());

DROP POLICY IF EXISTS "Admin acceso total a mensajes de soporte" ON public.support_messages;
CREATE POLICY "Admin acceso total a mensajes de soporte" ON public.support_messages 
    FOR ALL TO authenticated USING (public.is_admin());

DROP POLICY IF EXISTS "Usuario ve mensajes de tickets no resueltos" ON public.support_messages;
CREATE POLICY "Usuario ve mensajes de tickets no resueltos" ON public.support_messages 
    FOR SELECT TO authenticated 
    USING (
        EXISTS (
            SELECT 1 FROM public.support_tickets t 
            WHERE t.id = support_messages.ticket_id 
              AND t.user_id = auth.uid() 
              AND t.status NOT IN ('RESUELTO', 'CERRADO')
        )
    );

DROP POLICY IF EXISTS "Usuario envia mensajes en su ticket activo" ON public.support_messages;
CREATE POLICY "Usuario envia mensajes en su ticket activo" ON public.support_messages 
    FOR INSERT TO authenticated 
    WITH CHECK (
        sender_id = auth.uid() AND 
        EXISTS (
            SELECT 1 FROM public.support_tickets t 
            WHERE t.id = support_messages.ticket_id 
              AND t.user_id = auth.uid() 
              AND t.status NOT IN ('RESUELTO', 'CERRADO')
        )
    );

-- 6. VISTA DE COMPATIBILIDAD INTELIGENTE: public.profiles
DROP TABLE IF EXISTS public.profiles CASCADE;
DROP VIEW IF EXISTS public.profiles CASCADE;
CREATE OR REPLACE VIEW public.profiles AS
SELECT 
    u.id,
    u.full_name,
    u.email,
    u.phone,
    u.role,
    u.avatar_url,
    u.university,
    u.campus,
    u.fcm_token,
    u.push_notifications_enabled,
    u.is_suspended,
    u.suspension_reason,
    s.business_name,
    s.business_category,
    s.business_description,
    s.business_location,
    s.open_time,
    s.close_time,
    s.banner_url,
    s.logo_url,
    COALESCE(s.rating_average, 5.00) AS rating_average,
    COALESCE(s.review_count, 0) AS review_count,
    COALESCE(s.accepting_orders, true) AS accepting_orders,
    COALESCE(s.show_in_explore, true) AS show_in_explore,
    COALESCE(s.business_status, 'ABIERTO') AS business_status,
    COALESCE(s.supported_meeting_points, '{}') AS supported_meeting_points,
    COALESCE(s.supported_payment_methods, '{EFECTIVO,YAPE,PLIN}') AS supported_payment_methods,
    u.created_at,
    u.updated_at
FROM public.user_profiles u
LEFT JOIN public.seller_stores s ON s.seller_id = u.id;

CREATE OR REPLACE FUNCTION public.fn_profiles_view_update()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    UPDATE public.user_profiles
    SET 
        full_name = COALESCE(NEW.full_name, full_name),
        phone = COALESCE(NEW.phone, phone),
        role = COALESCE(NEW.role, role),
        avatar_url = COALESCE(NEW.avatar_url, avatar_url),
        campus = COALESCE(NEW.campus, campus),
        fcm_token = COALESCE(NEW.fcm_token, fcm_token),
        push_notifications_enabled = COALESCE(NEW.push_notifications_enabled, push_notifications_enabled),
        suspension_reason = COALESCE(NEW.suspension_reason, suspension_reason),
        updated_at = now()
    WHERE id = OLD.id;

    IF (NEW.business_name IS NOT NULL OR NEW.role = 'emprendedor'::public.user_role) THEN
        INSERT INTO public.seller_stores (
            seller_id,
            business_name,
            business_category,
            business_description,
            business_location,
            open_time,
            close_time,
            banner_url,
            logo_url,
            accepting_orders,
            show_in_explore,
            business_status,
            supported_meeting_points,
            supported_payment_methods,
            updated_at
        ) VALUES (
            OLD.id,
            COALESCE(NEW.business_name, 'Mi Emprendimiento'),
            COALESCE(NEW.business_category, 'General'),
            NEW.business_description,
            NEW.business_location,
            COALESCE(NEW.open_time, '08:00'),
            COALESCE(NEW.close_time, '18:00'),
            NEW.banner_url,
            NEW.logo_url,
            COALESCE(NEW.accepting_orders, true),
            COALESCE(NEW.show_in_explore, true),
            COALESCE(NEW.business_status, 'ABIERTO'),
            COALESCE(NEW.supported_meeting_points, '{}'),
            COALESCE(NEW.supported_payment_methods, '{EFECTIVO,YAPE,PLIN}'),
            now()
        )
        ON CONFLICT (seller_id) DO UPDATE SET
            business_name = COALESCE(EXCLUDED.business_name, seller_stores.business_name),
            business_category = COALESCE(EXCLUDED.business_category, seller_stores.business_category),
            business_description = COALESCE(EXCLUDED.business_description, seller_stores.business_description),
            business_location = COALESCE(EXCLUDED.business_location, seller_stores.business_location),
            open_time = COALESCE(EXCLUDED.open_time, seller_stores.open_time),
            close_time = COALESCE(EXCLUDED.close_time, seller_stores.close_time),
            banner_url = COALESCE(EXCLUDED.banner_url, seller_stores.banner_url),
            logo_url = COALESCE(EXCLUDED.logo_url, seller_stores.logo_url),
            accepting_orders = COALESCE(EXCLUDED.accepting_orders, seller_stores.accepting_orders),
            show_in_explore = COALESCE(EXCLUDED.show_in_explore, seller_stores.show_in_explore),
            business_status = COALESCE(EXCLUDED.business_status, seller_stores.business_status),
            supported_meeting_points = COALESCE(EXCLUDED.supported_meeting_points, seller_stores.supported_meeting_points),
            supported_payment_methods = COALESCE(EXCLUDED.supported_payment_methods, seller_stores.supported_payment_methods),
            updated_at = now();
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_profiles_view_update ON public.profiles;
CREATE TRIGGER trg_profiles_view_update
    INSTEAD OF UPDATE ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION public.fn_profiles_view_update();

-- 7. ACTUALIZAR TRIGGER DE AUTENTICACIÓN
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_role_str TEXT;
    v_user_role public.user_role;
    v_is_seller BOOLEAN;
    v_business_name TEXT;
    v_university TEXT;
    v_campus TEXT;
BEGIN
    v_role_str := LOWER(COALESCE(NEW.raw_user_meta_data->>'role', 'comprador'));
    v_user_role := CASE 
        WHEN v_role_str = 'emprendedor' THEN 'emprendedor'::public.user_role
        WHEN v_role_str = 'admin' THEN 'admin'::public.user_role
        ELSE 'comprador'::public.user_role
    END;

    v_business_name := NEW.raw_user_meta_data->>'business_name';
    v_is_seller := (v_user_role = 'emprendedor'::public.user_role OR v_business_name IS NOT NULL);
    v_university := COALESCE(NEW.raw_user_meta_data->>'university', 'UCV');
    v_campus := COALESCE(NEW.raw_user_meta_data->>'campus', 'Los Olivos');

    INSERT INTO public.user_profiles (
        id,
        full_name,
        email,
        phone,
        university,
        campus,
        role
    ) VALUES (
        NEW.id,
        COALESCE(NEW.raw_user_meta_data->>'full_name', 'Estudiante'),
        NEW.email,
        COALESCE(NEW.raw_user_meta_data->>'phone', ''),
        v_university,
        v_campus,
        v_user_role
    )
    ON CONFLICT (id) DO UPDATE SET
        full_name = EXCLUDED.full_name,
        email = EXCLUDED.email,
        phone = EXCLUDED.phone,
        university = EXCLUDED.university,
        campus = EXCLUDED.campus,
        role = EXCLUDED.role;

    IF v_is_seller THEN
        INSERT INTO public.seller_stores (
            seller_id,
            business_name,
            business_category,
            business_description,
            business_status,
            accepting_orders,
            supported_meeting_points,
            supported_payment_methods
        ) VALUES (
            NEW.id,
            COALESCE(v_business_name, 'Puesto de ' || COALESCE(NEW.raw_user_meta_data->>'full_name', 'Emprendedor')),
            COALESCE(NEW.raw_user_meta_data->>'business_category', 'General'),
            NEW.raw_user_meta_data->>'business_description',
            'PENDIENTE',
            false,
            CASE 
                WHEN NEW.raw_user_meta_data->>'meeting_point' IS NOT NULL 
                THEN ARRAY[NEW.raw_user_meta_data->>'meeting_point']::text[]
                ELSE '{}'::text[]
            END,
            '{EFECTIVO,YAPE,PLIN}'
        )
        ON CONFLICT (seller_id) DO NOTHING;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- 8. RECARGA DE CACHÉ DE POSTGREST
NOTIFY pgrst, 'reload schema';
