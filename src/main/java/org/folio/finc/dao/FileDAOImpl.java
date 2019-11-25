package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Promise;
import java.util.List;
import org.folio.finc.model.File;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.utils.Constants;

public class FileDAOImpl implements FileDAO {

  private static final String ID_FIELD = "id";
  private static final String TABLE_NAME = "files";

  @Override
  public Promise<File> getById(String id, String isil, Context vertxContext) {
    Promise<File> result = Promise.promise();
    Criterion criterion = getCriterion(id, isil);
    PostgresClient.getInstance(vertxContext.owner(), Constants.MODULE_TENANT)
        .get(
            TABLE_NAME,
            File.class,
            criterion,
            false,
            reply -> {
              if (reply.succeeded()) {
                List<File> fileList = reply.result().getResults();

                if (fileList.size() > 1) {
                  result.fail(
                      "Error while getting file by id. Found "
                          + fileList.size()
                          + " entries for id "
                          + id
                          + ".");
                }

                File file;
                if (fileList.isEmpty()) {
                  file = null;
                } else {
                  file = fileList.get(0);
                  if (!(id.equals(file.getId()))) {
                    result.fail(
                        "Error while getting file by id. Ids do not match. Expected: "
                            + id
                            + " - Actual: "
                            + file.getId());
                  }
                }
                result.complete(file);
              } else {
                result.fail("Cannot get file by id " + reply.cause());
              }
            });
    return result;
  }

  @Override
  public Promise<File> upsert(File entity, String id, Context vertxContext) {
    Promise<File> result = Promise.promise();
    PostgresClient.getInstance(vertxContext.owner(), Constants.MODULE_TENANT)
        .upsert(
            TABLE_NAME,
            id,
            entity,
            asyncResult -> {
              if (asyncResult.succeeded()) {
                result.complete(entity);
              } else {
                result.fail("Cannot upsert file: " + asyncResult.cause());
              }
            });
    return result;
  }

  @Override
  public Promise<Integer> deleteById(String id, String isil, Context vertxContext) {
    Promise<Integer> result = Promise.promise();
    Criterion criterion = getCriterion(id, isil);
    PostgresClient.getInstance(vertxContext.owner(), Constants.MODULE_TENANT)
        .delete(
            TABLE_NAME,
            criterion,
            reply -> {
              if (reply.succeeded()) {
                result.complete(reply.result().getUpdated());
              } else {
                result.fail(reply.cause());
              }
            });
    return result;
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
