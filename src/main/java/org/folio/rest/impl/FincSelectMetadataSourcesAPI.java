package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.finc.select.MetadataSourcesHelper;
import org.folio.rest.jaxrs.model.FincSelectMetadataSource;
import org.folio.rest.jaxrs.model.FincSelectMetadataSourcesGetOrder;
import org.folio.rest.jaxrs.resource.FincSelectMetadataSources;

public class FincSelectMetadataSourcesAPI implements FincSelectMetadataSources {

  private final MetadataSourcesHelper metadataSourcesHelper;

  public FincSelectMetadataSourcesAPI(Vertx vertx, String tenantId) {
    this.metadataSourcesHelper = new MetadataSourcesHelper(vertx, tenantId);
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
      Context vertxContext) {
    this.metadataSourcesHelper.getFincSelectMetadataSources(
        query, orderBy, order, offset, limit, lang, okapiHeaders, asyncResultHandler, vertxContext);
  }

  @Override
  public void postFincSelectMetadataSources(
      String lang,
      FincSelectMetadataSource entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    vertxContext.runOnContext(
        aVoid -> {
          asyncResultHandler.handle(
              succeededFuture(
                  FincSelectMetadataSources.PostFincSelectMetadataSourcesResponse.status(501)
                      .build()));
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
                  FincSelectMetadataSources.DeleteFincSelectMetadataSourcesByIdResponse.status(501)
                      .build()));
        });
  }

  @Override
  public void putFincSelectMetadataSourcesById(
      String id,
      String lang,
      FincSelectMetadataSource entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    vertxContext.runOnContext(
        aVoid -> {
          asyncResultHandler.handle(
              succeededFuture(
                  FincSelectMetadataSources.PutFincSelectMetadataSourcesByIdResponse.status(501)
                      .build()));
        });
  }

  @Override
  public void getFincSelectMetadataSourcesCollectionsById(
      String id,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {}

  @Override
  public void putFincSelectMetadataSourcesCollectionsSelectAllById(
      String id,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {}
}
