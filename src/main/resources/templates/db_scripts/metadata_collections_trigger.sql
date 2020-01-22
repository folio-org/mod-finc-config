CREATE OR REPLACE FUNCTION count_selected_collections_per_isil(
  mdSourceId TEXT
  ) RETURNS TABLE(c bigint, isil text, mdSource text) AS
$$
  SELECT  COUNT(i) AS c, i, s
	FROM (
	    SELECT  jsonb_array_elements_text(jsonb->'selectedBy') AS i,
			        JSONB->'mdSource'->>'id' AS s
			FROM    metadata_collections
		  WHERE   JSONB->'mdSource'->>'id' = $1) AS sub
			GROUP BY i, s;
$$
LANGUAGE SQL;


CREATE OR REPLACE FUNCTION add_null_to_counted_selected_collections(
 mdSourceId TEXT
 ) RETURNS TABLE(isil text, c bigint, mdSource text) AS
$$
  SELECT  isils.jsonb->>'isil' AS i,
	  			CASE WHEN sub.c IS NULL THEN 0
		  		ELSE sub.c
			  	END AS c,
			  	sub.mdSource
  FROM    isils
	    		LEFT JOIN count_selected_collections_per_isil($1) AS sub
			    ON (isils.jsonb->>'isil' = sub.isil);
$$
LANGUAGE SQL;

CREATE OR REPLACE FUNCTION calc_diff_to_counted_selected_collections(
 mdSourceId TEXT
 ) RETURNS TABLE(isil text, mdSource text, count bigint, diff bigint) AS
$$
  SELECT  sub.isil,
          sub.mdSource,
          sub.c,
          ( SELECT COUNT(*)
            FROM metadata_collections
            WHERE metadata_collections.JSONB->'mdSource'->>'id' = $1) - sub.c AS diff
  FROM    add_null_to_counted_selected_collections($1) AS sub;
$$
LANGUAGE SQL;

CREATE OR REPLACE FUNCTION calc_selected_state(
 mdSourceId TEXT
 ) RETURNS TABLE(isil text, selected text) AS
$$
  SELECT  sub.isil,
          CASE WHEN sub.diff = 0 THEN 'all'
          WHEN sub.diff = sub.count THEN 'none'
          WHEN sub.count = 0 THEN 'none'
          ELSE 'some'
          END AS selected
  FROM calc_diff_to_counted_selected_collections($1) AS sub;
$$
LANGUAGE SQL;

CREATE OR REPLACE FUNCTION calc_selected_state_as_json(
 mdSourceId TEXT
 ) RETURNS TABLE(selected jsonb) AS
$$
  SELECT  to_jsonb(array_agg(sub))
  FROM    calc_selected_state($1) AS sub;
$$
LANGUAGE SQL;

CREATE OR REPLACE FUNCTION update_sources_selected_by_on_update() RETURNS TRIGGER AS
$BODY$
DECLARE selected jsonb;
BEGIN
  SELECT calc_selected_state_as_json(NEW.jsonb->'mdSource'->>'id') INTO selected;
  IF selected IS NOT NULL THEN
    UPDATE metadata_sources SET jsonb = jsonb_set(jsonb, '{selectedBy}', selected, TRUE) WHERE jsonb->>'id' = NEW.jsonb->'mdSource'->>'id';
  END IF;
  RETURN NEW;
END;
$BODY$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION update_sources_selected_by_on_delete() RETURNS TRIGGER AS
$BODY$
DECLARE selected jsonb;
BEGIN
  SELECT calc_selected_state_as_json(OLD.jsonb->'mdSource'->>'id') INTO selected;
  UPDATE metadata_sources SET jsonb = jsonb_set(jsonb, '{selectedBy}', selected, TRUE) WHERE jsonb->>'id' = OLD.jsonb->'mdSource'->>'id';
  RETURN OLD;
END;
$BODY$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS update_metadata_sources_selected_by_on_update ON metadata_collections;

CREATE TRIGGER update_metadata_sources_selected_by_on_update
AFTER INSERT OR UPDATE ON metadata_collections
FOR EACH ROW
EXECUTE PROCEDURE update_sources_selected_by_on_update();

DROP TRIGGER IF EXISTS update_metadata_sources_selected_by_on_delete ON metadata_collections;

CREATE TRIGGER update_metadata_sources_selected_by_on_delete
AFTER DELETE ON metadata_collections
FOR EACH ROW
EXECUTE PROCEDURE update_sources_selected_by_on_delete();
