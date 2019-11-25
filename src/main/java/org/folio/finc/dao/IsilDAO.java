package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Promise;

public interface IsilDAO {

  Promise<String> getIsilForTenant(String tenantId, Context context);
}
