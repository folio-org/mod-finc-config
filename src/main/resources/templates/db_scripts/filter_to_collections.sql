-- Add a GIN index for the join on collections.id for the metadata_collections_w_filters view
DROP INDEX IF EXISTS filter_to_collections_collectionids_idx_gin;
CREATE INDEX filter_to_collections_collectionids_idx_gin
  ON filter_to_collections USING gin ((jsonb->'collectionIds'));
