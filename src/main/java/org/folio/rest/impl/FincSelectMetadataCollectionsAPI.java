package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.finc.select.MetadataCollectionsHelper;
import org.folio.finc.select.MetadataSourcesHelper;
import org.folio.rest.jaxrs.model.FincSelectMetadataCollection;
import org.folio.rest.jaxrs.model.FincSelectMetadataCollectionsGetOrder;
import org.folio.rest.jaxrs.model.Select;
import org.folio.rest.jaxrs.resource.FincSelectMetadataCollections;

public class FincSelectMetadataCollectionsAPI implements FincSelectMetadataCollections {

  private final MetadataCollectionsHelper metadataCollectionsHelper;

  public FincSelectMetadataCollectionsAPI(Vertx vertx, String tenantId) {
    this.metadataCollectionsHelper = new MetadataCollectionsHelper(vertx, tenantId);
  }

  @Override
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
    this.metadataCollectionsHelper.getFincSelectMetadataCollections(
        query, orderBy, order, offset, limit, lang, okapiHeaders, asyncResultHandler, vertxContext);
  }

  @Override
  public void postFincSelectMetadataCollections(
      String lang,
      FincSelectMetadataCollection entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    // Posting metadata collections is not allowed via finc-select
    vertxContext.runOnContext(
        aVoid -> {
          asyncResultHandler.handle(
              succeededFuture(
                  FincSelectMetadataCollections.PostFincSelectMetadataCollectionsResponse.status(
                          501)
                      .build()));
        });
  }

  @Override
  public void getFincSelectMetadataCollectionsById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    metadataCollectionsHelper.getFincSelectMetadataCollectionsById(
        id, lang, okapiHeaders, asyncResultHandler, vertxContext);
  }

  @Override
  public void deleteFincSelectMetadataCollectionsById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    vertxContext.runOnContext(
        aVoid -> {
          asyncResultHandler.handle(
              succeededFuture(
                  FincSelectMetadataCollections
                      .DeleteFincSelectMetadataCollectionsSelectByIdResponse.status(501)
                      .build()));
        });
  }

  @Override
  public void putFincSelectMetadataCollectionsById(
      String id,
      String lang,
      FincSelectMetadataCollection entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    vertxContext.runOnContext(
        aVoid -> {
          asyncResultHandler.handle(
              succeededFuture(
                  FincSelectMetadataCollections.PutFincSelectMetadataCollectionsByIdResponse.status(
                          501)
                      .build()));
        });
  }

  @Override
  public void putFincSelectMetadataCollectionsSelectById(
      String id,
      String lang,
      Select entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    metadataCollectionsHelper.putFincSelectMetadataCollectionsSelectById(
        id, lang, entity, okapiHeaders, asyncResultHandler, vertxContext);
  }

  @Override
  public void getFincSelectMetadataCollectionsSelectById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    vertxContext.runOnContext(
        aVoid -> {
          asyncResultHandler.handle(
              succeededFuture(
                  FincSelectMetadataCollections.GetFincSelectMetadataCollectionsSelectByIdResponse
                      .status(501)
                      .build()));
        });
  }

  @Override
  public void deleteFincSelectMetadataCollectionsSelectById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    vertxContext.runOnContext(
        aVoid -> {
          asyncResultHandler.handle(
              succeededFuture(
                  FincSelectMetadataCollections
                      .DeleteFincSelectMetadataCollectionsSelectByIdResponse.status(501)
                      .build()));
        });
  }
}
