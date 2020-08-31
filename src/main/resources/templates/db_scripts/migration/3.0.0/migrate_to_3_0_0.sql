START TRANSACTION;

-- Deleting contacts in migration as migration path is unclear
UPDATE  ${myuniversity}_${mymodule}.metadata_sources
SET     jsonb = jsonb - 'contacts'
WHERE   jsonb->'contacts' IS NOT NULL;

END TRANSACTION;