package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.finc.dao.FilterToCollectionsDAO;
import org.folio.finc.dao.FilterToCollectionsDAOImpl;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.FincConfigFiltersGetOrder;
import org.folio.rest.jaxrs.model.FincSelectFilter;
import org.folio.rest.jaxrs.model.FincSelectFilterToCollections;
import org.folio.rest.jaxrs.model.FincSelectFilters;
import org.folio.rest.jaxrs.resource.FincConfigFilters;
import org.folio.rest.jaxrs.resource.FincSelectFilters.PutFincSelectFiltersCollectionsByIdResponse;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.utils.Constants;

/**
 * Manages filters for ui-finc-config
 */
public class FincConfigFiltersAPI implements FincConfigFilters {

  public static final String X_OKAPI_TENANT = "x-okapi-tenant";
  private static final String TABLE_NAME = "filters";

  private FilterToCollectionsDAO filterToCollectionsDAO;

  public FincConfigFiltersAPI() {
    this.filterToCollectionsDAO = new FilterToCollectionsDAOImpl();
  }

  @Override
  @Validate
  public void getFincConfigFilters(
      String query,
      String orderBy,
      FincConfigFiltersGetOrder order,
      String totalRecords,
      int offset,
      int limit,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    okapiHeaders.put(X_OKAPI_TENANT, Constants.MODULE_TENANT);
    PgUtil.get(
        TABLE_NAME,
        FincSelectFilter.class,
        FincSelectFilters.class,
        query,
        offset,
        limit,
        okapiHeaders,
        vertxContext,
        GetFincConfigFiltersResponse.class,
        asyncResultHandler);
  }

  @Override
  @Validate
  public void postFincConfigFilters(
      FincSelectFilter entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    okapiHeaders.put(X_OKAPI_TENANT, Constants.MODULE_TENANT);
    PgUtil.post(
        TABLE_NAME,
        entity,
        okapiHeaders,
        vertxContext,
        PostFincConfigFiltersResponse.class,
        asyncResultHandler);
  }

  @Override
  @Validate
  public void getFincConfigFiltersById(
      String id,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    okapiHeaders.put(X_OKAPI_TENANT, Constants.MODULE_TENANT);
    PgUtil.getById(
        TABLE_NAME,
        FincSelectFilter.class,
        id,
        okapiHeaders,
        vertxContext,
        GetFincConfigFiltersByIdResponse.class,
        asyncResultHandler);
  }

  @Override
  @Validate
  public void deleteFincConfigFiltersById(
      String id,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    okapiHeaders.put(X_OKAPI_TENANT, Constants.MODULE_TENANT);
    PgUtil.deleteById(
        TABLE_NAME,
        id,
        okapiHeaders,
        vertxContext,
        DeleteFincConfigFiltersByIdResponse.class,
        asyncResultHandler);
  }

  @Override
  @Validate
  public void putFincConfigFiltersById(
      String id,
      FincSelectFilter entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    okapiHeaders.put(X_OKAPI_TENANT, Constants.MODULE_TENANT);
    PgUtil.put(
        TABLE_NAME,
        entity,
        id,
        okapiHeaders,
        vertxContext,
        PutFincConfigFiltersByIdResponse.class,
        asyncResultHandler);
  }

  @Override
  @Validate
  public void getFincConfigFiltersCollectionsById(
      String id,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    okapiHeaders.put(X_OKAPI_TENANT, Constants.MODULE_TENANT);
    PgUtil.getById(
        "filter_to_collections",
        FincSelectFilterToCollections.class,
        id,
        okapiHeaders,
        vertxContext,
        GetFincConfigFiltersCollectionsByIdResponse.class,
        asyncResultHandler);
  }

  @Override
  @Validate
  public void putFincConfigFiltersCollectionsById(
      String id,
      FincSelectFilterToCollections entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {

    filterToCollectionsDAO
        .getById(id, vertxContext)
        .compose(
            fincSelectFilterToCollections -> {
              if (fincSelectFilterToCollections == null) {
                // do insert
                return filterToCollectionsDAO.insert(entity, vertxContext);
              } else {
                // do update -> delete and insert
                return filterToCollectionsDAO
                    .deleteById(entity.getId(), vertxContext)
                    .compose(integer -> filterToCollectionsDAO.insert(entity, vertxContext));
              }
            })
        .onSuccess(
            fincSelectFilterToCollections -> asyncResultHandler.handle(
                Future.succeededFuture(
                    PutFincSelectFiltersCollectionsByIdResponse.respond200WithApplicationJson(
                        fincSelectFilterToCollections))))
        .onFailure(
            throwable ->
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        PutFincSelectFiltersCollectionsByIdResponse.respond500WithTextPlain(
                            throwable.getCause()))));
  }
}
