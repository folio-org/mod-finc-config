package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.UpdateResult;
import java.util.Arrays;
import java.util.List;
import org.folio.rest.jaxrs.model.FincSelectFilterFile;
import org.folio.rest.jaxrs.model.FincSelectFilterFiles;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.utils.Constants;
import org.folio.cql2pgjson.CQL2PgJSON;
import org.folio.cql2pgjson.exception.FieldException;

public class FilterFileDAOImpl implements FilterFileDAO {

  private static final String ID_FIELD = "id";
  private static final String TABLE_NAME = "filter_files";
  private final Logger logger = LoggerFactory.getLogger(FilterFileDAOImpl.class);

  @Override
  public Future<FincSelectFilterFiles> getAll(
      String query, int offset, int limit, String isil, Context vertxContext) {

    Future<org.folio.rest.jaxrs.model.FincSelectFilterFiles> result = Future.future();
    String field = "*";
    String[] fieldList = {field};

    CQLWrapper cql = null;
    try {
      cql = getCQL(query, limit, offset, isil);
    } catch (FieldException e) {
      logger.error("Error while processing CQL " + e.getMessage());
      result.fail("Cannot get filter files. Error while processing CQL: " + e);
    }

    PostgresClient.getInstance(vertxContext.owner(), Constants.MODULE_TENANT)
        .get(
            TABLE_NAME,
            FincSelectFilterFile.class,
            fieldList,
            cql,
            true,
            false,
            reply -> {
              if (reply.succeeded()) {
                org.folio.rest.jaxrs.model.FincSelectFilterFiles filterFiles =
                    new org.folio.rest.jaxrs.model.FincSelectFilterFiles();
                List<FincSelectFilterFile> fileList = reply.result().getResults();
                filterFiles.setFincSelectFilterFiles(fileList);
                filterFiles.setTotalRecords(fileList.size());
                result.complete(filterFiles);
              } else {
                result.fail("Cannot get filter files: " + reply.cause());
              }
            });
    return result;
  }

  @Override
  public Future<FincSelectFilterFile> getById(String id, String isil, Context vertxContext) {

    Future<FincSelectFilterFile> result = Future.future();
    Criterion criterion = getCriterion(id, isil);
    PostgresClient.getInstance(vertxContext.owner(), Constants.MODULE_TENANT)
        .get(
            TABLE_NAME,
            FincSelectFilterFile.class,
            criterion,
            true,
            false,
            reply -> {
              if (reply.succeeded()) {
                List<FincSelectFilterFile> fileList = reply.result().getResults();

                if (fileList.isEmpty()) {
                  result.complete(null);
                }
                if (fileList.size() > 1) {
                  result.fail(
                      "Error while getting filter file by id. Found "
                          + fileList.size()
                          + " entries for id "
                          + id
                          + ".");
                }
                FincSelectFilterFile fincSelectFilterFile = fileList.get(0);

                if (!(fincSelectFilterFile.getId().equals(id))) {
                  result.fail(
                      "Error while getting filter file by id. Ids do not match. Expected: "
                          + id
                          + " - Actual: "
                          + fincSelectFilterFile.getId());
                }
                result.complete(fincSelectFilterFile);
              } else {
                result.fail("Cannot get filter files by id " + reply.cause());
              }
            });
    return result;
  }

  @Override
  public Future<FincSelectFilterFile> insert(FincSelectFilterFile entity, Context vertxContext) {
    Future<FincSelectFilterFile> result = Future.future();
    PostgresClient.getInstance(vertxContext.owner(), Constants.MODULE_TENANT)
        .save(
            TABLE_NAME,
            entity.getId(),
            entity,
            asyncResult -> {
              if (asyncResult.succeeded()) {
                result.complete(entity);
              } else {
                result.fail("Cannot insert filter file: " + asyncResult.cause());
              }
            });
    return result;
  }

  @Override
  public Future<Integer> deleteById(String id, String isil, Context vertxContext) {
    Future<Integer> result = Future.future();
    Criterion criterion = getCriterion(id, isil);

    PostgresClient.getInstance(vertxContext.owner(), Constants.MODULE_TENANT)
        .delete(
            TABLE_NAME,
            criterion,
            reply -> {
              if (reply.succeeded()) {
                UpdateResult updateResult = reply.result();
                result.complete(updateResult.getUpdated());
              } else {
                result.fail("Error while deleting finc select filter file. " + reply.cause());
              }
            });
    return result;
  }

  private CQLWrapper getCQL(String query, int limit, int offset, String isil)
      throws FieldException {
    CQL2PgJSON cql2PgJSON = new CQL2PgJSON(Arrays.asList(TABLE_NAME + ".jsonb"));

    query = addIsilTo(query, isil);

    return new CQLWrapper(cql2PgJSON, query)
        .setLimit(new Limit(limit))
        .setOffset(new Offset(offset));
  }

  private String addIsilTo(String query, String isil) {
    if (query == null || "".equals(query)) {
      return "isil=\"" + isil + "\"";
    } else {
      return query + "AND isil=\"" + isil + "\"";
    }
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
