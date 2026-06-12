UPDATE legal_documents
SET content = BTRIM(REPLACE(content, 'BORRADOR PARA REVISIÓN JURÍDICA.', '')),
    version = REGEXP_REPLACE(version, '-draft$', '')
WHERE content LIKE '%BORRADOR PARA REVISIÓN JURÍDICA.%'
   OR version LIKE '%-draft';
