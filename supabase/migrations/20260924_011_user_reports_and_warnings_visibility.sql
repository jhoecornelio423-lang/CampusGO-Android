-- 20260924_011_user_reports_and_warnings_visibility.sql
-- Permite que los usuarios (comprador y vendedor) consulten sus advertencias emitidas por el administrador
-- y enriquece la tabla order_incidents para el flujo completo de reportes y resoluciones del administrador.

-- 1. Políticas RLS en profile_warnings
DROP POLICY IF EXISTS "Usuarios ven sus propias advertencias" ON public.profile_warnings;
CREATE POLICY "Usuarios ven sus propias advertencias" ON public.profile_warnings
    FOR SELECT TO authenticated
    USING (profile_id = auth.uid() OR public.is_admin());

DROP POLICY IF EXISTS "Usuarios anon ven advertencias" ON public.profile_warnings;
CREATE POLICY "Usuarios anon ven advertencias" ON public.profile_warnings
    FOR SELECT TO anon
    USING (true);

-- 2. Campos adicionales en order_incidents para seguimiento del administrador
ALTER TABLE public.order_incidents ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMPTZ;
ALTER TABLE public.order_incidents ADD COLUMN IF NOT EXISTS resolved_by UUID REFERENCES profiles(id);
ALTER TABLE public.order_incidents ADD COLUMN IF NOT EXISTS resolution_action TEXT;
ALTER TABLE public.order_incidents ADD COLUMN IF NOT EXISTS admin_notes TEXT;

-- 3. Función RPC para que el administrador resuelva una incidencia con acción directa (advertencia, suspensión o descarte)
CREATE OR REPLACE FUNCTION public.resolve_incident_rpc(
    p_incident_id UUID,
    p_status TEXT,
    p_action TEXT DEFAULT NULL,
    p_admin_notes TEXT DEFAULT NULL
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    UPDATE public.order_incidents
    SET 
        status = p_status,
        resolution_action = p_action,
        admin_notes = p_admin_notes,
        resolved_at = now(),
        resolved_by = auth.uid()
    WHERE id = p_incident_id;

    RETURN jsonb_build_object('success', true, 'incident_id', p_incident_id);
END;
$$;
