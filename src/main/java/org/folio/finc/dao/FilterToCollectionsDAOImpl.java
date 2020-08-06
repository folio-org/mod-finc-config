package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import java.util.List;
import org.folio.rest.jaxrs.model.FincSelectFilterToCollections;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.utils.Constants;

public class FilterToCollectionsDAOImpl implements FilterToCollectionsDAO {

  private static final String ID_FIELD = "id";
  private static final String TABLE_NAME = "filter_to_collections";

  @Override
  public Future<FincSelectFilterToCollections> getByIdAndIsil(
      String filterId, String isil, Context vertxContext) {
    Criteria filterIDCrit =
        new Criteria().addField("'id'").setOperation("=").setVal(filterId).setJSONB(true);
    Criteria isilCrit =
        new Criteria().addField("'isil'").setOperation("=").setVal(isil).setJSONB(true);
    Criterion criterion = new Criterion();
    criterion.addCriterion(filterIDCrit);
    criterion.addCriterion(isilCrit);

    return getByCriterion(criterion, vertxContext);
  }

  @Override
  public Future<FincSelectFilterToCollections> getById(String filterId, Context vertxContext) {
    Criteria filterIDCrit =
        new Criteria().addField("'id'").setOperation("=").setVal(filterId).setJSONB(true);
    Criterion criterion = new Criterion();
    criterion.addCriterion(filterIDCrit);

    return getByCriterion(criterion, vertxContext);
  }

  @Override
  public Future<FincSelectFilterToCollections> getByCriterion(
      Criterion criterion, Context vertxContext) {
    Promise<FincSelectFilterToCollections> result = Promise.promise();

    PostgresClient.getInstance(vertxContext.owner(), Constants.MODULE_TENANT)
        .get(
            TABLE_NAME,
            FincSelectFilterToCollections.class,
            criterion,
            false,
            reply -> {
              if (reply.succeeded()) {
                List<FincSelectFilterToCollections> filterToCollections =
                    reply.result().getResults();
                if (!filterToCollections.isEmpty()) {
                  result.complete(filterToCollections.get(0));
                } else {
                  result.complete(null);
                }
              } else {
                result.fail("Cannot get filters to collections. " + reply.cause());
              }
            });
    return result.future();
  }

  @Override
  public Future<FincSelectFilterToCollections> insert(
      FincSelectFilterToCollections entity, Context vertxContext) {
    Promise<FincSelectFilterToCollections> result = Promise.promise();

    if (entity.getId() == null) {
      result.fail("FincSelectFilterCollection must have an id");
    } else {
      entity.setCollectionsCount(entity.getCollectionIds().size());
      PostgresClient.getInstance(vertxContext.owner(), Constants.MODULE_TENANT)
          .upsert(
              TABLE_NAME,
              entity.getId(),
              entity,
              reply -> {
                if (reply.succeeded()) {
                  result.complete(entity);
                } else {
                  result.fail(reply.cause());
                }
              });
    }
    return result.future();
  }

  @Override
  public Future<Integer> deleteById(String id, Context vertxContext) {
    Promise<Integer> result = Promise.promise();
    Criteria idCrit =
        new Criteria().addField(ID_FIELD).setJSONB(false).setOperation("=").setVal(id);
    Criterion criterion = new Criterion(idCrit);

    PostgresClient.getInstance(vertxContext.owner(), Constants.MODULE_TENANT)
        .delete(
            TABLE_NAME,
            criterion,
            reply -> {
              if (reply.succeeded()) {
                RowSet<Row> rowSet = reply.result();
                result.complete(rowSet.rowCount());
              } else {
                result.fail(
                    "Error while deleting finc select filter to collections. " + reply.cause());
              }
            });
    return result.future();
  }
}
