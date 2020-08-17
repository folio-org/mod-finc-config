CREATE OR REPLACE FUNCTION delete_filter_to_collections_on_filter_delete() RETURNS TRIGGER AS
$BODY$
BEGIN
  DELETE FROM filter_to_collections WHERE jsonb->>'id' = OLD.jsonb->>'id';
  RETURN OLD;
END;
$BODY$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS delete_filter_to_collections_on_delete ON filters;

CREATE TRIGGER delete_filter_to_collections_on_delete
AFTER DELETE ON filters
FOR EACH ROW
EXECUTE PROCEDURE delete_filter_to_collections_on_filter_delete();
