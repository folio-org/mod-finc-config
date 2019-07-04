package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.finc.select.SelectFiltersHelper;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.FincSelectFilter;
import org.folio.rest.jaxrs.model.FincSelectFilterFile;
import org.folio.rest.jaxrs.model.FincSelectFiltersGetOrder;
import org.folio.rest.jaxrs.resource.FincSelectFilters;

public class FincSelectFiltersAPI implements FincSelectFilters {

  private final SelectFiltersHelper selectFiltersHelper;

  public FincSelectFiltersAPI(Vertx vertx, String tenantId) {
    this.selectFiltersHelper = new SelectFiltersHelper(vertx, tenantId);
  }

  @Override
  @Validate
  public void getFincSelectFilters(
      String query,
      String orderBy,
      FincSelectFiltersGetOrder order,
      int offset,
      int limit,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    selectFiltersHelper.getFincSelectFilters(
        query, orderBy, order, offset, limit, lang, okapiHeaders, asyncResultHandler, vertxContext);
  }

  @Override
  @Validate
  public void postFincSelectFilters(
      String lang,
      FincSelectFilter entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    selectFiltersHelper.postFincSelectFilters(
        lang, entity, okapiHeaders, asyncResultHandler, vertxContext);
  }

  @Override
  @Validate
  public void getFincSelectFiltersById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    selectFiltersHelper.getFincSelectFiltersById(
        id, lang, okapiHeaders, asyncResultHandler, vertxContext);
  }

  @Override
  @Validate
  public void deleteFincSelectFiltersById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    selectFiltersHelper.deleteFincSelectFiltersById(
        id, lang, okapiHeaders, asyncResultHandler, vertxContext);
  }

  @Override
  @Validate
  public void putFincSelectFiltersById(
      String id,
      String lang,
      FincSelectFilter entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    selectFiltersHelper.putFincSelectFiltersById(
        id, lang, entity, okapiHeaders, asyncResultHandler, vertxContext);
  }
}
