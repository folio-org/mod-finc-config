package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Promise;
import org.folio.rest.jaxrs.model.FincSelectMetadataSource;
import org.folio.rest.jaxrs.model.FincSelectMetadataSources;

public interface SelectMetadataSourcesDAO {

  Promise<FincSelectMetadataSources> getAll(
      String query, int offset, int limit, String isil, Context vertxContext);

  Promise<FincSelectMetadataSource> getById(String id, String isil, Context vertxContext);
}
