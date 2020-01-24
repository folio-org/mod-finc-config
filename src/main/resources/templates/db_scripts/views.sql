CREATE OR REPLACE VIEW metadata_sources_tiny AS SELECT id AS id, jsonb_build_object('id', jsonb->>'id', 'label', jsonb->>'label') AS jsonb FROM metadata_sources;

CREATE OR REPLACE VIEW filters_wo_isil AS SELECT id AS id, jsonb - 'isil' AS jsonb FROM filters;
