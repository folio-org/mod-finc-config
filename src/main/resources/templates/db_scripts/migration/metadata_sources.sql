UPDATE ${myuniversity}_${mymodule}.metadata_sources
SET
  jsonb = jsonb || jsonb_build_object('selectedBy', selectedBy_as_json.selectedBy)
FROM (
    WITH count_selected_collections_per_isil AS (
      SELECT
        COUNT(i) AS c,
        i AS isil,
        s AS mdSource
      FROM (
          SELECT
            jsonb_array_elements_text(jsonb -> 'selectedBy') AS i,
            JSONB -> 'mdSource' ->> 'id' AS s
          FROM ${myuniversity}_${mymodule}.metadata_collections
        ) AS sub
      GROUP BY
        i,
        s
    ),
    all_md_sources_and_isils AS (
      SELECT
        i.jsonb ->> 'isil' AS isil,
        mc.jsonb -> 'mdSource' ->> 'id' AS mdSource
      FROM isils AS i
      CROSS JOIN metadata_collections AS mc
    ),
    add_null_to_counted_selected_collections AS (
      SELECT
        DISTINCT a.isil,
        CASE
          WHEN c.c IS NULL THEN 0
          ELSE c.c
        END AS c,
        a.mdSource
      FROM count_selected_collections_per_isil AS c
      RIGHT JOIN all_md_sources_and_isils AS a ON c.mdSource = a.mdSource
        AND c.isil = a.isil
    ),
    calc_diff_to_counted_selected_collections AS (
      SELECT
        sub.isil,
        sub.mdSource,
        sub.c,
        COUNT(metadata_collections.*) - sub.c AS diff
      FROM add_null_to_counted_selected_collections AS sub
      JOIN ${myuniversity}_${mymodule}.metadata_collections ON metadata_collections.jsonb -> 'mdSource' ->> 'id' = sub.mdSource
      GROUP BY
        sub.isil,
        sub.mdSource,
        sub.c
    ),
    calc_selected_state AS (
      SELECT
        isil,
        CASE
          WHEN diff = 0 THEN 'all'
          WHEN c = 0 THEN 'none'
          ELSE 'some'
        END AS selected,
        mdSource
      FROM calc_diff_to_counted_selected_collections
    )
    SELECT
      to_jsonb(
        array_agg(
          jsonb_build_object('isil', isil, 'selected', selected)
        )
      ) AS selectedBy,
      mdSource
    FROM calc_selected_state AS sub
    GROUP BY
      mdSource
  ) AS selectedBy_as_json
WHERE
  (
    NOT jsonb ? 'selectedBy'
    OR jsonb_array_length(jsonb -> 'selectedBy') = 0
  )
  AND selectedBy_as_json.mdSource :: text = metadata_sources.id :: text;
