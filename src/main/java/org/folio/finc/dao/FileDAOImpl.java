package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import java.util.List;
import org.folio.finc.model.File;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.utils.Constants;

public class FileDAOImpl implements FileDAO {

  @Override
  public Future<File> getById(String id, Context vertxContext) {
    Criteria idCrit =
        new Criteria().addField("id").setJSONB(false).setOperation("=").setVal(id);
    Criterion criterion = new Criterion(idCrit);
    return getByCriterion(criterion, vertxContext);
  }

  @Override
  public Future<File> getByCriterion(Criterion criterion, Context vertxContext) {
    Promise<File> result = Promise.promise();
    PostgresClient.getInstance(vertxContext.owner(), Constants.MODULE_TENANT)
        .get(
            TABLE_NAME,
            File.class,
            criterion,
            false,
            reply -> {
              if (reply.succeeded()) {
                List<File> fileList = reply.result().getResults();
                File file;
                if (fileList.isEmpty()) {
                  file = null;
                } else {
                  file = fileList.get(0);
                }
                result.complete(file);
              } else {
                result.fail("Cannot get file by id " + reply.cause());
              }
            });
    return result.future();
  }
}
