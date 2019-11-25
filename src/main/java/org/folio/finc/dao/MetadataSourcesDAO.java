package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Promise;
import org.folio.rest.jaxrs.model.FincConfigMetadataSource;
import org.folio.rest.jaxrs.model.FincConfigMetadataSources;

public interface MetadataSourcesDAO {

  Promise<FincConfigMetadataSources> getAll(
      String query, int offset, int limit, Context vertxContext);

  Promise<FincConfigMetadataSource> getById(String id, Context vertxContext);
}
