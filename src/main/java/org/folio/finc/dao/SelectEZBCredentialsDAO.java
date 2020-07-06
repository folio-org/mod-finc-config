package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.Credential;

public interface SelectEZBCredentialsDAO {

  Future<Credential> getByIsil(String isil, Context ctx);

  Future<Integer> deleteByIsil(String isil, Context vertxContext);

  Future<Credential> upsert(Credential entity, Context ctx);

}
