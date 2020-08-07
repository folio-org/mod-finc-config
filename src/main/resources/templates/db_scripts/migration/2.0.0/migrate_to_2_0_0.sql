START TRANSACTION;

UPDATE  ${myuniversity}_${mymodule}.metadata_collections
SET     jsonb = jsonb - 'filters'
WHERE   jsonb->'filters' IS NOT NULL;

UPDATE  ${myuniversity}_${mymodule}.metadata_collections
SET     jsonb = jsonb || jsonb_build_object('collectionId', 'n/a - ' || random())
WHERE   jsonb->'collectionId' IS NULL;

UPDATE  ${myuniversity}_${mymodule}.metadata_collections
SET     jsonb = jsonb - 'facetLabel'
WHERE   jsonb->'facetLabel' IS NOT NULL;

UPDATE  ${myuniversity}_${mymodule}.metadata_collections
SET     jsonb = jsonb_set(jsonb, '{lod, publication}', ('"' || new_publication.value::text || '"')::jsonb)
FROM    (
      SELECT	jsonb->>'id' AS id,
      		    CASE  WHEN jsonb->'lod'->>'publication' = 'permitted (interpreted)' THEN 'yes'
                    WHEN jsonb->'lod'->>'publication' = 'permitted (explicit)' THEN 'yes'
                    WHEN jsonb->'lod'->>'publication' = 'permitted (explicit) under conditions' THEN 'yes'
                    WHEN jsonb->'lod'->>'publication' = 'prohibited (interpreted)' THEN 'no'
                    WHEN jsonb->'lod'->>'publication' = 'prohibited (explicit)' THEN 'no'
                    WHEN jsonb->'lod'->>'publication' = 'silent' THEN 'undetermined'
                    ELSE 'other'
              END   AS value
      FROM    ${myuniversity}_${mymodule}.metadata_collections
      WHERE   jsonb->'lod'->>'publication' IS NOT NULL
) AS new_publication
WHERE 	metadata_collections.jsonb->>'id' = new_publication.id;

UPDATE  ${myuniversity}_${mymodule}.metadata_sources
SET     jsonb = jsonb - 'contracts'
WHERE   jsonb->'contracts' IS NOT NULL;

END TRANSACTION;
