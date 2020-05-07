package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.Credential;
import org.folio.rest.jaxrs.model.Credentials;

public interface EZBCredentialsDAO {

  Future<Credentials> getAll(
      String query, int offset, int limit, Context vertxContext);

  Future<Credential> getByIsil(String isil, Context vertxContext);

  Future<Credential> insert(Credential entity, Context vertxContext);

  Future<Integer> deleteByIsil(String isil, Context vertxContext);


}
