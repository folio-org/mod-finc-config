package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.cql2pgjson.CQL2PgJSON;
import org.folio.cql2pgjson.exception.FieldException;
import org.folio.finc.select.query.MetadataCollectionsQueryTranslator;
import org.folio.finc.select.query.QueryTranslator;
import org.folio.rest.jaxrs.model.FincSelectFilter;
import org.folio.rest.jaxrs.model.FincSelectFilters;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.PgExceptionUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.utils.Constants;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class SelectFilterDAOImpl implements SelectFilterDAO {

  private static final String ID_FIELD = "id";
  private static final String TABLE_NAME = "filters";

  private final Logger logger = LogManager.getLogger(SelectFilterDAOImpl.class);

  private final QueryTranslator queryTranslator;

  public SelectFilterDAOImpl() {
    queryTranslator = new MetadataCollectionsQueryTranslator();
  }

  @Override
  public Future<FincSelectFilters> getAll(
      String query, int offset, int limit, String isil, Context vertxContext) {

    Promise<FincSelectFilters> result = Promise.promise();

    String field = "*";
    String[] fieldList = {field};

    CQLWrapper cql = null;
    try {
      cql = getCQL(query, limit, offset, isil);
    } catch (FieldException e) {
      logger.error("Error while processing CQL {}", PgExceptionUtil.getMessage(e));
      result.fail("Cannot get filters. Error while processing CQL: " + e);
    }

    PostgresClient.getInstance(vertxContext.owner(), Constants.MODULE_TENANT)
        .get(
            TABLE_NAME,
            FincSelectFilter.class,
            fieldList,
            cql,
            true,
            false,
            reply -> {
              if (reply.succeeded()) {

                org.folio.rest.jaxrs.model.FincSelectFilters fincSelectFilters =
                    new org.folio.rest.jaxrs.model.FincSelectFilters();
                List<FincSelectFilter> filterList = reply.result().getResults();
                fincSelectFilters.setFincSelectFilters(filterList);
                fincSelectFilters.setTotalRecords(reply.result().getResultInfo().getTotalRecords());
                result.complete(fincSelectFilters);
              } else {
                result.fail("Cannot get filter files: " + reply.cause());
              }
            });
    return result.future();
  }

  @Override
  public Future<FincSelectFilter> getById(String id, String isil, Context vertxContext) {
    Promise<FincSelectFilter> result = Promise.promise();
    Criterion criterion = getCriterion(id, isil);
    PostgresClient.getInstance(vertxContext.owner(), Constants.MODULE_TENANT)
        .get(
            TABLE_NAME,
            FincSelectFilter.class,
            criterion,
            true,
            false,
            reply -> {
              if (reply.succeeded()) {
                List<FincSelectFilter> filterList = reply.result().getResults();

                if (filterList.isEmpty()) {
                  result.complete(null);
                  return;
                }
                if (filterList.size() != 1) {
                  result.fail(
                      "Error while getting filter by id. Found "
                          + filterList.size()
                          + " entries for id "
                          + id
                          + ".");
                }
                FincSelectFilter fincSelectFilter = filterList.get(0);

                if (!(fincSelectFilter.getId().equals(id))) {
                  result.fail(
                      "Error while getting filter by id. Ids do not match. Expected: "
                          + id
                          + " - Actual: "
                          + fincSelectFilter.getId());
                }
                result.complete(fincSelectFilter);
              } else {
                result.fail("Cannot get filter by id " + reply.cause());
              }
            });
    return result.future();
  }

  @Override
  public Future<FincSelectFilter> insert(FincSelectFilter entity, Context vertxContext) {
    if (entity.getId() == null) {
      entity.setId(UUID.randomUUID().toString());
    }
    Promise<FincSelectFilter> result = Promise.promise();
    PostgresClient.getInstance(vertxContext.owner(), Constants.MODULE_TENANT)
        .save(
            TABLE_NAME,
            entity.getId(),
            entity,
            asyncResult -> {
              if (asyncResult.succeeded()) {
                result.complete(entity);
              } else {
                result.fail("Cannot insert filter: " + asyncResult.cause());
              }
            });
    return result.future();
  }

  @Override
  public Future<FincSelectFilter> update(FincSelectFilter entity, String id, Context vertxContext) {
    Promise<FincSelectFilter> result = Promise.promise();
    PostgresClient.getInstance(vertxContext.owner(), Constants.MODULE_TENANT)
        .update(
            TABLE_NAME,
            entity,
            id,
            asyncResult -> {
              if (asyncResult.succeeded()) {
                result.complete(entity);
              } else {
                result.fail("Cannot update filter: " + asyncResult.cause());
              }
            });
    return result.future();
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
                RowSet<Row> rowSet = reply.result();
                result.complete(rowSet.rowCount());
              } else {
                result.fail("Error while deleting finc select filter. " + reply.cause());
              }
            });
    return result.future();
  }

  private CQLWrapper getCQL(String query, int limit, int offset, String isil)
      throws FieldException {
    CQL2PgJSON cql2PgJSON = new CQL2PgJSON(Collections.singletonList(TABLE_NAME + ".jsonb"));
    query = addIsilTo(query, isil);
    return new CQLWrapper(cql2PgJSON, query)
        .setLimit(new Limit(limit))
        .setOffset(new Offset(offset));
  }

  private String addIsilTo(String query, String isil) {
    String[] queryAndSortBy = queryTranslator.splitSortBy(query);
    query = queryAndSortBy[0];
    String sortBy = queryAndSortBy[1];
    if (query == null || "".equals(query)) {
      return "isil==\"" + isil + "\"";
    } else {
      return query + " AND isil==\"" + isil + "\"" + sortBy;
    }
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
