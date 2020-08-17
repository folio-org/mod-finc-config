package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.folio.finc.model.File;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.utils.Constants;

public class SelectFileDAOImpl implements SelectFileDAO {

  private static final String ID_FIELD = "id";
  private static final String TABLE_NAME = "files";
  private final FileDAO fileDAO = new FileDAOImpl();

  @Override
  public Future<File> getById(String id, String isil, Context vertxContext) {
    Criterion criterion = getCriterion(id, isil);
    return fileDAO.getByCriterion(criterion, vertxContext);
  }

  @Override
  public Future<File> upsert(File entity, String id, Context vertxContext) {
    return fileDAO.upsert(entity, id, vertxContext);
  }

  @Override
  public Future<Integer> deleteById(String id, String isil, Context vertxContext) {
    Promise<Integer> result = Promise.promise();
    Criterion criterion = getCriterion(id, isil);
    PostgresClient.getInstance(vertxContext.owner(), Constants.MODULE_TENANT)
        .delete(
            TABLE_NAME,
            criterion,
            reply -> {
              if (reply.succeeded()) {
                result.complete(reply.result().rowCount());
              } else {
                result.fail(reply.cause());
              }
            });
    return result.future();
  }

  private Criterion getCriterion(String id, String isil) {
    Criteria idCrit =
        new Criteria().addField(ID_FIELD).setJSONB(false).setOperation("=").setVal(id);
    Criterion criterion = new Criterion(idCrit);

    Criteria isilCrit =
        new Criteria().addField("'isil'").setJSONB(true).setOperation("=").setVal(isil);
    criterion.addCriterion(isilCrit);
    return criterion;
  }
}
