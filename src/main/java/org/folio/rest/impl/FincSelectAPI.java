package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.finc.select.MetadataCollectionsHelper;
import org.folio.rest.jaxrs.model.FincSelectMetadataCollectionsGetOrder;
import org.folio.rest.jaxrs.model.FincSelectMetadataSourcesGetOrder;
import org.folio.rest.jaxrs.model.MetadataCollectionSelect;
import org.folio.rest.jaxrs.model.MetadataSourceSelect;
import org.folio.rest.jaxrs.model.Select;
import org.folio.rest.jaxrs.resource.FincSelect;

public class FincSelectAPI implements FincSelect {

  private MetadataCollectionsHelper metadataCollectionsHelper;

  public FincSelectAPI(Vertx vertx, String tenantId) {
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
      MetadataCollectionSelect entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    // Posting metadata collections is not allowed via finc-select
    vertxContext.runOnContext(
        aVoid -> {
          asyncResultHandler.handle(
              succeededFuture(
                  FincSelect.PostFincSelectMetadataCollectionsResponse.status(501).build()));
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
                  FincSelect.GetFincSelectMetadataCollectionsSelectByIdResponse.status(501)
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
                  FincSelect.DeleteFincSelectMetadataCollectionsSelectByIdResponse.status(501)
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
                  FincSelect.DeleteFincSelectMetadataCollectionsByIdResponse.status(501).build()));
        });
  }

  @Override
  public void putFincSelectMetadataCollectionsById(
      String id,
      String lang,
      MetadataCollectionSelect entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    vertxContext.runOnContext(
        aVoid -> {
          asyncResultHandler.handle(
              succeededFuture(
                  FincSelect.PutFincSelectMetadataCollectionsByIdResponse.status(501).build()));
        });
  }

  @Override
  public void getFincSelectMetadataSources(
      String query,
      String orderBy,
      FincSelectMetadataSourcesGetOrder order,
      int offset,
      int limit,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {}

  @Override
  public void postFincSelectMetadataSources(
      String lang,
      MetadataSourceSelect entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    vertxContext.runOnContext(
        aVoid -> {
          asyncResultHandler.handle(
              succeededFuture(
                  FincSelect.PostFincSelectMetadataSourcesResponse.status(501).build()));
        });
  }

  @Override
  public void getFincSelectMetadataSourcesById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {}

  @Override
  public void deleteFincSelectMetadataSourcesById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    vertxContext.runOnContext(
        aVoid -> {
          asyncResultHandler.handle(
              succeededFuture(
                  FincSelect.DeleteFincSelectMetadataSourcesByIdResponse.status(501).build()));
        });
  }

  @Override
  public void putFincSelectMetadataSourcesById(
      String id,
      String lang,
      MetadataSourceSelect entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    vertxContext.runOnContext(
        aVoid -> {
          asyncResultHandler.handle(
              succeededFuture(
                  FincSelect.PutFincSelectMetadataSourcesByIdResponse.status(501).build()));
        });
  }

  @Override
  public void postFincSelectMetadataSourcesSelectById(
      String id,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {}
}
