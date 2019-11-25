package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Promise;
import org.folio.rest.jaxrs.model.FincSelectMetadataCollection;
import org.folio.rest.jaxrs.model.FincSelectMetadataCollections;

public interface SelectMetadataCollectionsDAO {

  Promise<FincSelectMetadataCollections> getAll(
      String query, int offset, int limit, String isil, Context vertxContext);

  Promise<FincSelectMetadataCollection> getById(String id, String isil, Context vertxContext);
}
