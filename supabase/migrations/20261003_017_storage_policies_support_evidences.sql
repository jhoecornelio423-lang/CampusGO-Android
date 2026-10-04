-- ============================================================================
-- MIGRACIÓN 017: POLÍTICAS DE STORAGE PARA EVIDENCIAS DE SOPORTE
-- Proyecto: CampusGO
-- ============================================================================

DROP POLICY IF EXISTS "Public Storage Read" ON storage.objects;
CREATE POLICY "Public Storage Read" ON storage.objects FOR SELECT TO public
USING (bucket_id = ANY (ARRAY['business-assets'::text, 'product-images'::text, 'support-evidences'::text]));

DROP POLICY IF EXISTS "Authenticated Users Upload" ON storage.objects;
CREATE POLICY "Authenticated Users Upload" ON storage.objects FOR INSERT TO authenticated
WITH CHECK (bucket_id = ANY (ARRAY['business-assets'::text, 'product-images'::text, 'support-evidences'::text]));

DROP POLICY IF EXISTS "Authenticated Users Update" ON storage.objects;
CREATE POLICY "Authenticated Users Update" ON storage.objects FOR UPDATE TO authenticated
USING (bucket_id = ANY (ARRAY['business-assets'::text, 'product-images'::text, 'support-evidences'::text]));

DROP POLICY IF EXISTS "Authenticated Users Delete" ON storage.objects;
CREATE POLICY "Authenticated Users Delete" ON storage.objects FOR DELETE TO authenticated
USING (bucket_id = ANY (ARRAY['business-assets'::text, 'product-images'::text, 'support-evidences'::text]));
