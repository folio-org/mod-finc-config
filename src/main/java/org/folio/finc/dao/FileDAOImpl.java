package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Future;
import java.util.List;
import org.folio.finc.model.File;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.utils.Constants;

public class FileDAOImpl implements FileDAO {

  private static final String ID_FIELD = "id";

  @Override
  public Future<File> getById(String id, String isil, Context vertxContext) {
    Future<File> result = Future.future();
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

                /*if (fileList.isEmpty()) {
                  result.complete(new File());
                  return;
                }*/
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
  public Future<File> upsert(File entity, String id, Context vertxContext) {
    Future<File> result = Future.future();
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
  public Future<Integer> deleteById(String id, String isil, Context vertxContext) {
    return null;
  }

  private Criterion getCriterion(String id, String isil) {
    Criteria idCrit =
        new Criteria()
            .addField(ID_FIELD)
            .setJSONB(false)
            .setOperation("=")
            .setVal(id);
    Criterion criterion = new Criterion(idCrit);

    Criteria isilCrit =
        new Criteria().addField("'isil'").setJSONB(true).setOperation("=").setVal(isil);
    criterion.addCriterion(isilCrit);
    return criterion;
  }
}
