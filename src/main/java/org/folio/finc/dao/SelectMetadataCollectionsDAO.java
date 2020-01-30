package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.FincSelectMetadataCollection;
import org.folio.rest.jaxrs.model.FincSelectMetadataCollections;

public interface SelectMetadataCollectionsDAO {

  Future<FincSelectMetadataCollections> getAll(
      String query, int offset, int limit, String isil, Context vertxContext);

  Future<FincSelectMetadataCollection> getById(String id, String isil, Context vertxContext);
}
