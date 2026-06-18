UPDATE users u
SET work_experience_description = p.description
FROM technician_profiles p
WHERE p.user_id = u.id
  AND u.role = 'TECHNICIAN'
  AND (u.work_experience_description IS NULL OR btrim(u.work_experience_description) = '')
  AND p.description IS NOT NULL
  AND btrim(p.description) <> '';

UPDATE users u
SET onboarding_completed = FALSE,
    onboarding_step = 'TECHNICIAN_PROFESSIONAL_PROFILE'
WHERE u.role = 'TECHNICIAN'
  AND (
      NOT EXISTS (
          SELECT 1
          FROM technician_profiles p
          WHERE p.user_id = u.id
            AND length(btrim(p.description)) >= 30
      )
      OR NOT EXISTS (
          SELECT 1
          FROM technician_profiles p
          JOIN technician_profile_categories pc ON pc.technician_profile_id = p.id
          WHERE p.user_id = u.id
      )
      OR u.work_experience_description IS NULL
      OR length(btrim(u.work_experience_description)) < 30
  );
