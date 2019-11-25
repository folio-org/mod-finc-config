package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Promise;
import java.util.List;
import org.folio.rest.jaxrs.model.Isil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.utils.Constants;

public class IsilDAOImpl implements IsilDAO {

  private static final String TABLE_NAME = "isils";

  @Override
  public Promise<String> getIsilForTenant(String tenantId, Context context) {
    Promise<String> future = Promise.promise();
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
                if (isils.isEmpty()) {
                  future.fail("Cannot find isil for tenant " + tenantId);
                } else if (isils.size() > 1) {
                  future.fail("Found multiple isils for tenant " + tenantId);
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
