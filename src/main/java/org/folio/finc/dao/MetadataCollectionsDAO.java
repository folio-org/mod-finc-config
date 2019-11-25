package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Promise;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollections;

public interface MetadataCollectionsDAO {

  Promise<FincConfigMetadataCollections> getAll(
      String query, int offset, int limit, Context vertxContext);

  Promise<FincConfigMetadataCollection> getById(String id, Context vertxContext);

  Promise<FincConfigMetadataCollection> update(
      FincConfigMetadataCollection entity, String id, Context vertxContext);
}
