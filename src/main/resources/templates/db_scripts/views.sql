CREATE OR REPLACE VIEW metadata_sources_tiny AS SELECT id AS id, jsonb_build_object('id', jsonb->>'id', 'label', jsonb->>'label') AS jsonb FROM metadata_sources;

CREATE OR REPLACE VIEW filters_wo_isil AS SELECT id AS id, jsonb - 'isil' AS jsonb FROM filters;

CREATE OR REPLACE VIEW metadata_sources_contacts AS SELECT DISTINCT ON (c.jsonb->>'name') c.jsonb
                                                    FROM (SELECT jsonb_array_elements(jsonb->'contacts') AS jsonb from finc_mod_finc_config.metadata_sources) AS c
                                                    ORDER BY c.jsonb->>'name';
