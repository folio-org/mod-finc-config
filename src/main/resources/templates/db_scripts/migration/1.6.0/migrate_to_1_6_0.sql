START TRANSACTION;

UPDATE  ${myuniversity}_${mymodule}.metadata_collections
SET     jsonb = jsonb - 'filters'
WHERE   jsonb->'filters' IS NOT NULL;

UPDATE  ${myuniversity}_${mymodule}.metadata_sources
SET     jsonb = jsonb - 'contracts'
WHERE   jsonb->'contracts' IS NOT NULL;

END TRANSACTION;