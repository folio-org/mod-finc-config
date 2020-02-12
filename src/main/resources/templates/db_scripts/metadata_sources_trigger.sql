CREATE OR REPLACE FUNCTION update_collections_md_source_name_on_update() RETURNS TRIGGER AS
$BODY$
BEGIN
  UPDATE metadata_collections SET jsonb = jsonb_set(jsonb, '{mdSource, name}', NEW.jsonb->'label', TRUE) WHERE jsonb->'mdSource'->>'id' = NEW.jsonb->>'id';
  RETURN NEW;
END;
$BODY$ LANGUAGE plpgsql;

CREATE TRIGGER update_metadata_collections_on_update_source
AFTER UPDATE ON metadata_sources
FOR EACH ROW
WHEN (OLD.jsonb->>'label' IS DISTINCT FROM NEW.jsonb->>'label')
EXECUTE PROCEDURE update_collections_md_source_name_on_update();
