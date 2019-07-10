package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollections;

public interface MetadataCollectionsDAO {

  String TABLE_NAME = "metadata_collections";

  Future<FincConfigMetadataCollections> getAll(String query, int offset, int limit, Context vertxContext);

  Future<FincConfigMetadataCollection> getById(String id, Context vertxContext);

  Future<FincConfigMetadataCollection> update(FincConfigMetadataCollection entity, String id, Context vertxContext);
}
