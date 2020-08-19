CREATE OR REPLACE VIEW metadata_sources_tiny AS SELECT id AS id, jsonb_build_object('id', jsonb->>'id', 'label', jsonb->>'label') AS jsonb FROM metadata_sources;

CREATE OR REPLACE VIEW filters_wo_isil AS SELECT id AS id, jsonb - 'isil' AS jsonb FROM filters;

CREATE OR REPLACE VIEW metadata_sources_contacts AS SELECT *
                                                    FROM (SELECT DISTINCT(jsonb_array_elements(jsonb->'contacts')) AS jsonb from metadata_sources) AS jsonb
                                                    ORDER BY jsonb->>'name';
