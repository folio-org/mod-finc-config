package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.TinyMetadataSources;

public interface MetadataSourcesTinyDAO {

  Future<TinyMetadataSources> getAll(Context vertxContext);

}
