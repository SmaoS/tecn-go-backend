ALTER TABLE countries
    ADD COLUMN IF NOT EXISTS indicativo_celular VARCHAR(8);

UPDATE countries
SET indicativo_celular = '+57'
WHERE code = 'CO'
  AND (indicativo_celular IS NULL OR indicativo_celular = '');
