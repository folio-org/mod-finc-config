package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.finc.select.SelectMetadataCollectionsHelper;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.FincSelectMetadataCollection;
import org.folio.rest.jaxrs.model.FincSelectMetadataCollectionsGetOrder;
import org.folio.rest.jaxrs.model.Select;
import org.folio.rest.jaxrs.resource.FincSelectMetadataCollections;

public class FincSelectMetadataCollectionsAPI implements FincSelectMetadataCollections {

  private final SelectMetadataCollectionsHelper selectMetadataCollectionsHelper;

  public FincSelectMetadataCollectionsAPI(Vertx vertx, String tenantId) {
    this.selectMetadataCollectionsHelper = new SelectMetadataCollectionsHelper(vertx, tenantId);
  }

  @Override
  @Validate
  public void getFincSelectMetadataCollections(
      String query,
      String orderBy,
      FincSelectMetadataCollectionsGetOrder order,
      int offset,
      int limit,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    this.selectMetadataCollectionsHelper.getFincSelectMetadataCollections(
        query, orderBy, order, offset, limit, lang, okapiHeaders, asyncResultHandler, vertxContext);
  }

  @Override
  @Validate
  public void postFincSelectMetadataCollections(
      String lang,
      FincSelectMetadataCollection entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    // Posting metadata collections is not allowed via finc-select
    vertxContext.runOnContext(
        aVoid ->
            asyncResultHandler.handle(
                succeededFuture(
                    FincSelectMetadataCollections.PostFincSelectMetadataCollectionsResponse.status(
                            501)
                        .build())));
  }

  @Override
  @Validate
  public void getFincSelectMetadataCollectionsById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    selectMetadataCollectionsHelper.getFincSelectMetadataCollectionsById(
        id, lang, okapiHeaders, asyncResultHandler, vertxContext);
  }

  @Override
  @Validate
  public void deleteFincSelectMetadataCollectionsById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    vertxContext.runOnContext(
        aVoid ->
            asyncResultHandler.handle(
                succeededFuture(
                    FincSelectMetadataCollections
                        .DeleteFincSelectMetadataCollectionsSelectByIdResponse.status(501)
                        .build())));
  }

  @Override
  @Validate
  public void putFincSelectMetadataCollectionsById(
      String id,
      String lang,
      FincSelectMetadataCollection entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    vertxContext.runOnContext(
        aVoid ->
            asyncResultHandler.handle(
                succeededFuture(
                    FincSelectMetadataCollections.PutFincSelectMetadataCollectionsByIdResponse
                        .status(501)
                        .build())));
  }

  @Override
  @Validate
  public void putFincSelectMetadataCollectionsSelectById(
      String id,
      String lang,
      Select entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    selectMetadataCollectionsHelper.putFincSelectMetadataCollectionsSelectById(
        id, lang, entity, okapiHeaders, asyncResultHandler, vertxContext);
  }

  @Override
  @Validate
  public void getFincSelectMetadataCollectionsSelectById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    vertxContext.runOnContext(
        aVoid ->
            asyncResultHandler.handle(
                succeededFuture(
                    FincSelectMetadataCollections.GetFincSelectMetadataCollectionsSelectByIdResponse
                        .status(501)
                        .build())));
  }

  @Override
  @Validate
  public void deleteFincSelectMetadataCollectionsSelectById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    vertxContext.runOnContext(
        aVoid ->
            asyncResultHandler.handle(
                succeededFuture(
                    FincSelectMetadataCollections
                        .DeleteFincSelectMetadataCollectionsSelectByIdResponse.status(501)
                        .build())));
  }
}
