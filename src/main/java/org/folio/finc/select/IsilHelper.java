package org.folio.finc.select;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import java.util.List;
import org.folio.rest.jaxrs.model.Isil;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.utils.Constants;

public class IsilHelper {

  private static final String TABLE_NAME = "isils";

  public Future<String> fetchIsil(String tenantId, Context context) {
    Promise<String> future = Promise.promise();
    Criterion tenantIdCriterion =
        new Criterion(new Criteria().addField("tenant").setJSONB(true).setOperation("=").setVal(tenantId));
    PostgresClient.getInstance(context.owner(), Constants.MODULE_TENANT)
        .get(
            TABLE_NAME,
            Isil.class,
            tenantIdCriterion,
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
    return future.future();
  }
}
