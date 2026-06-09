DO $$
BEGIN
IF to_regclass('public.service_categories') IS NULL THEN
    RETURN;
END IF;

ALTER TABLE service_categories
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE technician_profiles
    ALTER COLUMN specialty DROP NOT NULL;

CREATE TABLE IF NOT EXISTS technician_profile_categories (
    technician_profile_id UUID NOT NULL REFERENCES technician_profiles(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES service_categories(id),
    PRIMARY KEY (technician_profile_id, category_id)
);

ALTER TABLE service_requests
    ADD COLUMN IF NOT EXISTS estimated_price NUMERIC(12, 2),
    ADD COLUMN IF NOT EXISTS technician_price NUMERIC(12, 2),
    ADD COLUMN IF NOT EXISTS final_price NUMERIC(12, 2);

UPDATE service_categories SET name = 'Electricista', slug = 'electricista'
WHERE slug = 'electricidad';
UPDATE service_categories SET name = 'Plomero', slug = 'plomero'
WHERE slug = 'plomeria';
UPDATE service_categories SET name = 'Técnico de computadores', slug = 'tecnico-computadores'
WHERE slug = 'computadores';
UPDATE service_categories SET name = 'Técnico de celulares', slug = 'tecnico-celulares'
WHERE slug = 'celulares';
UPDATE service_categories SET name = 'Aire acondicionado', slug = 'aire-acondicionado'
WHERE slug = 'aires-acondicionados';
UPDATE service_categories SET name = 'Internet / redes', slug = 'internet-redes'
WHERE slug = 'internet';

INSERT INTO service_categories (id, name, slug, description, active)
VALUES
    (gen_random_uuid(), 'Electricista', 'electricista', 'Instalaciones y reparaciones eléctricas', TRUE),
    (gen_random_uuid(), 'Plomero', 'plomero', 'Fugas, tuberías y aparatos sanitarios', TRUE),
    (gen_random_uuid(), 'Técnico de computadores', 'tecnico-computadores', 'Soporte, mantenimiento y reparación', TRUE),
    (gen_random_uuid(), 'Técnico de celulares', 'tecnico-celulares', 'Diagnóstico y reparación de dispositivos', TRUE),
    (gen_random_uuid(), 'Aire acondicionado', 'aire-acondicionado', 'Instalación y mantenimiento', TRUE),
    (gen_random_uuid(), 'Cámaras de seguridad', 'camaras-seguridad', 'Instalación y mantenimiento de videovigilancia', TRUE),
    (gen_random_uuid(), 'Internet / redes', 'internet-redes', 'Redes, Wi-Fi y conectividad', TRUE),
    (gen_random_uuid(), 'Cerrajería', 'cerrajeria', 'Apertura, cambio e instalación de cerraduras', TRUE)
ON CONFLICT (slug) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    active = TRUE;

INSERT INTO technician_profile_categories (technician_profile_id, category_id)
SELECT profile.id, category.id
FROM technician_profiles profile
JOIN service_categories category
  ON category.slug = CASE
      WHEN lower(profile.specialty) LIKE '%electric%' THEN 'electricista'
      WHEN lower(profile.specialty) LIKE '%plom%' THEN 'plomero'
      WHEN lower(profile.specialty) LIKE '%comput%' THEN 'tecnico-computadores'
      WHEN lower(profile.specialty) LIKE '%celular%' THEN 'tecnico-celulares'
      WHEN lower(profile.specialty) LIKE '%aire%' THEN 'aire-acondicionado'
      WHEN lower(profile.specialty) LIKE '%cámara%' OR lower(profile.specialty) LIKE '%camara%' THEN 'camaras-seguridad'
      WHEN lower(profile.specialty) LIKE '%internet%' OR lower(profile.specialty) LIKE '%red%' THEN 'internet-redes'
      WHEN lower(profile.specialty) LIKE '%cerraj%' THEN 'cerrajeria'
      ELSE ''
  END
ON CONFLICT DO NOTHING;

INSERT INTO technician_profile_categories (technician_profile_id, category_id)
SELECT profile.id, category.id
FROM technician_profiles profile
CROSS JOIN LATERAL (
    SELECT id FROM service_categories WHERE active = TRUE ORDER BY name LIMIT 1
) category
WHERE NOT EXISTS (
    SELECT 1 FROM technician_profile_categories link
    WHERE link.technician_profile_id = profile.id
)
ON CONFLICT DO NOTHING;

END;
$$;
