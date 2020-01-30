package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.FincSelectMetadataSource;
import org.folio.rest.jaxrs.model.FincSelectMetadataSources;

public interface SelectMetadataSourcesDAO {

  Future<FincSelectMetadataSources> getAll(
      String query, int offset, int limit, String isil, Context vertxContext);

  Future<FincSelectMetadataSource> getById(String id, String isil, Context vertxContext);
}
