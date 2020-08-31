START TRANSACTION;

-- Update status wish to request
UPDATE  ${myuniversity}_${mymodule}.metadata_sources
SET     jsonb = jsonb_set(jsonb, '{status}', ('"request"')::jsonb)
WHERE 	metadata_sources.jsonb->>'status' = 'wish';

END TRANSACTION;
