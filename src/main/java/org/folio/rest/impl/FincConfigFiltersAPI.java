package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.FincConfigFiltersGetOrder;
import org.folio.rest.jaxrs.model.FincSelectFilter;
import org.folio.rest.jaxrs.model.FincSelectFilterToCollections;
import org.folio.rest.jaxrs.model.FincSelectFilters;
import org.folio.rest.jaxrs.resource.FincConfigFilters;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.utils.Constants;

public class FincConfigFiltersAPI implements FincConfigFilters {

  private static final String TABLE_NAME = "filters";
  public static final String X_OKAPI_TENANT = "x-okapi-tenant";

  @Override
  @Validate
  public void getFincConfigFilters(String query, String orderBy, FincConfigFiltersGetOrder order,
      int offset, int limit, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    okapiHeaders.put(X_OKAPI_TENANT, Constants.MODULE_TENANT);
    PgUtil.get(TABLE_NAME, FincSelectFilter.class, FincSelectFilters.class, query, offset, limit,
        okapiHeaders, vertxContext, GetFincConfigFiltersResponse.class, asyncResultHandler);
  }

  @Override
  public void postFincConfigFilters(String lang, FincSelectFilter entity,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    vertxContext.runOnContext(
        aVoid -> asyncResultHandler.handle(succeededFuture(Response.status(501).build())));
  }

  @Override
  @Validate
  public void getFincConfigFiltersById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    okapiHeaders.put(X_OKAPI_TENANT, Constants.MODULE_TENANT);
    PgUtil.getById(TABLE_NAME, FincSelectFilter.class, id, okapiHeaders, vertxContext,
        GetFincConfigFiltersByIdResponse.class, asyncResultHandler);
  }

  @Override
  public void deleteFincConfigFiltersById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    vertxContext.runOnContext(
        aVoid -> asyncResultHandler.handle(succeededFuture(Response.status(501).build())));
  }

  @Override
  public void putFincConfigFiltersById(String id, String lang, FincSelectFilter entity,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    vertxContext.runOnContext(
        aVoid -> asyncResultHandler.handle(succeededFuture(Response.status(501).build())));
  }

  @Override
  @Validate
  public void getFincConfigFiltersCollectionsById(String id, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    okapiHeaders.put(X_OKAPI_TENANT, Constants.MODULE_TENANT);
    PgUtil.getById("filter_to_collections", FincSelectFilterToCollections.class, id, okapiHeaders,
        vertxContext, GetFincConfigFiltersCollectionsByIdResponse.class, asyncResultHandler);
  }
}
