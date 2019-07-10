package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Future;
import java.util.List;
import org.folio.rest.jaxrs.model.Isil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.utils.Constants;

public class IsilDAOImpl implements IsilDAO {

  @Override
  public Future<String> getIsilForTenant(String tenantId, Context context) {
    Future<String> future = Future.future();
    String where = String.format(" WHERE (jsonb->>'tenant' = '%s')", tenantId);
    PostgresClient.getInstance(context.owner(), Constants.MODULE_TENANT)
      .get(
        TABLE_NAME,
        Isil.class,
        where,
        false,
        false,
        ar -> {
          if (ar.succeeded()) {
            List<Isil> isils = ar.result().getResults();
            if (isils.size() != 1) {
              future.fail("Number isils != 1");
            } else {
              Isil isil = isils.get(0);
              future.complete(isil.getIsil());
            }
          } else {
            future.fail("Cannot fetch isil: " + ar.cause());
          }
        });
    return future;
  }
}
