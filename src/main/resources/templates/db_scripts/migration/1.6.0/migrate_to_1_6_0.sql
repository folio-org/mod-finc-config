START TRANSACTION;

UPDATE  ${myuniversity}_${mymodule}.metadata_collections
SET     jsonb = jsonb - 'filters'
WHERE   jsonb->'filters' IS NOT NULL;

UPDATE  ${myuniversity}_${mymodule}.metadata_collections
SET     jsonb = jsonb || '{"collectionId": "n/a"}'::jsonb
WHERE   jsonb->'collectionId' IS NULL;

UPDATE  ${myuniversity}_${mymodule}.metadata_collections
SET     jsonb = jsonb - 'facetLabel'
WHERE   jsonb->'facetLabel' IS NOT NULL;

UPDATE  ${myuniversity}_${mymodule}.metadata_sources
SET     jsonb = jsonb - 'contracts'
WHERE   jsonb->'contracts' IS NOT NULL;

END TRANSACTION;
