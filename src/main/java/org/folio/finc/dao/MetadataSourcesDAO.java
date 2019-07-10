package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.FincConfigMetadataSource;
import org.folio.rest.jaxrs.model.FincConfigMetadataSources;

public interface MetadataSourcesDAO {

  String TABLE_NAME = "metadata_sources";

  Future<FincConfigMetadataSources> getAll(String query, int offset, int limit, Context vertxContext);

  Future<FincConfigMetadataSource> getById(String id, Context vertxContext);

}
