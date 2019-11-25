package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Promise;
import org.folio.rest.jaxrs.model.TinyMetadataSources;

public interface MetadataSourcesTinyDAO {

  Promise<TinyMetadataSources> getAll(Context vertxContext);
}
