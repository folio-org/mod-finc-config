package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Future;
import java.util.Optional;
import org.folio.rest.jaxrs.model.Isil;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.interfaces.Results;
import org.folio.rest.utils.Constants;

public class IsilDAOImpl implements IsilDAO {

  private static final String TABLE_NAME = "isils";

  @Override
  public Future<Optional<String>> getIsilForTenant(String tenantId, Context context) {
    Criteria tenantCrit =
        new Criteria().addField("'tenant'").setJSONB(true).setOperation("=").setVal(tenantId);
    Criterion criterion = new Criterion(tenantCrit);

    return PostgresClient.getInstance(context.owner(), Constants.MODULE_TENANT)
        .get(TABLE_NAME, Isil.class, criterion)
        .map(Results::getResults)
        .map(
            isils -> {
              if (isils.isEmpty()) {
                return Optional.empty();
              }
              if (isils.size() == 1) {
                return Optional.of(isils.getFirst().getIsil());
              }
              throw new IllegalStateException("Found multiple isils for tenant " + tenantId);
            });
  }
}
