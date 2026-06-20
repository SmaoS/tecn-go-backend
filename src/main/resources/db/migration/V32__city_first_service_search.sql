INSERT INTO system_parameters (
    id, parameter_key, parameter_value, description, type, active, updated_at
) VALUES
    (gen_random_uuid(), 'SERVICE_SEARCH_USE_RADIUS', 'false',
     'Aplicar filtro por radio además de la ciudad en solicitudes disponibles', 'BOOLEAN', TRUE, NOW()),
    (gen_random_uuid(), 'SERVICE_SEARCH_DEFAULT_RADIUS_KM', '10',
     'Radio predeterminado para buscar solicitudes cuando el filtro está habilitado', 'DECIMAL', TRUE, NOW()),
    (gen_random_uuid(), 'SERVICE_SEARCH_MAX_RADIUS_KM', '50',
     'Radio máximo permitido para buscar solicitudes', 'DECIMAL', TRUE, NOW())
ON CONFLICT (parameter_key) DO NOTHING;

UPDATE service_requests request
SET city_id = client.city_id
FROM users client
WHERE request.client_id = client.id
  AND request.city_id IS NULL
  AND client.city_id IS NOT NULL;
