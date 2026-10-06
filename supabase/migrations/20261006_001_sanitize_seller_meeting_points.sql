-- Migración para sanear profiles.supported_meeting_points
-- Reemplaza nombres legados por UUIDs oficiales de campus_meeting_points y elimina valores huérfanos.

UPDATE profiles
SET supported_meeting_points = ARRAY(
    SELECT DISTINCT cmp.id::text
    FROM unnest(profiles.supported_meeting_points) AS elem
    JOIN campus_meeting_points cmp 
      ON cmp.id::text = elem OR LOWER(cmp.name) = LOWER(elem)
    WHERE cmp.is_active = true
)
WHERE supported_meeting_points IS NOT NULL 
  AND array_length(supported_meeting_points, 1) > 0;
