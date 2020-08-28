START TRANSACTION;

-- Deleting contacts in migration as migration path is unclear
UPDATE  ${myuniversity}_${mymodule}.metadata_sources
SET     jsonb = jsonb - 'contacts'
WHERE   jsonb->'contacts' IS NOT NULL;

-- Update status wish to request
UPDATE  ${myuniversity}_${mymodule}.metadata_sources
SET     jsonb = jsonb_set(jsonb, '{status}', ('"request"')::jsonb)
WHERE 	metadata_sources.jsonb->>'status' = 'wish';

END TRANSACTION;