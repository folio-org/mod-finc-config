package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Future;

public interface IsilDAO {

  String TABLE_NAME = "isils";

  Future<String> getIsilForTenant(String tenantId, Context context);

}
