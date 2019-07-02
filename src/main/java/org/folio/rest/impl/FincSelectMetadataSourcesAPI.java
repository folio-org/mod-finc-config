package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.finc.select.SelectMetadataSourcesHelper;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.FincSelectMetadataSource;
import org.folio.rest.jaxrs.model.FincSelectMetadataSourcesGetOrder;
import org.folio.rest.jaxrs.model.Select;
import org.folio.rest.jaxrs.resource.FincSelectMetadataSources;

public class FincSelectMetadataSourcesAPI implements FincSelectMetadataSources {

  private final SelectMetadataSourcesHelper selectMetadataSourcesHelper;

  public FincSelectMetadataSourcesAPI(Vertx vertx, String tenantId) {
    this.selectMetadataSourcesHelper = new SelectMetadataSourcesHelper(vertx, tenantId);
  }

  @Override
  @Validate
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
    this.selectMetadataSourcesHelper.getFincSelectMetadataSources(
        query, offset, limit, lang, okapiHeaders, asyncResultHandler, vertxContext);
  }

  @Override
  @Validate
  public void postFincSelectMetadataSources(
      String lang,
      FincSelectMetadataSource entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    vertxContext.runOnContext(
        aVoid ->
            asyncResultHandler.handle(
                succeededFuture(
                    FincSelectMetadataSources.PostFincSelectMetadataSourcesResponse.status(501)
                        .build())));
  }

  @Override
  @Validate
  public void getFincSelectMetadataSourcesById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    this.selectMetadataSourcesHelper.getFincSelectMetadataSourcesById(
        id, lang, okapiHeaders, asyncResultHandler, vertxContext);
  }

  @Override
  @Validate
  public void deleteFincSelectMetadataSourcesById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    vertxContext.runOnContext(
        aVoid ->
            asyncResultHandler.handle(
                succeededFuture(
                    FincSelectMetadataSources.DeleteFincSelectMetadataSourcesByIdResponse.status(
                            501)
                        .build())));
  }

  @Override
  @Validate
  public void putFincSelectMetadataSourcesById(
      String id,
      String lang,
      FincSelectMetadataSource entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    vertxContext.runOnContext(
        aVoid ->
            asyncResultHandler.handle(
                succeededFuture(
                    FincSelectMetadataSources.PutFincSelectMetadataSourcesByIdResponse.status(501)
                        .build())));
  }

  @Override
  @Validate
  public void getFincSelectMetadataSourcesCollectionsById(
      String id,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    vertxContext.runOnContext(
        aVoid ->
            asyncResultHandler.handle(
                succeededFuture(
                    FincSelectMetadataSources.GetFincSelectMetadataSourcesResponse.status(501)
                        .build())));
  }

  @Override
  @Validate
  public void putFincSelectMetadataSourcesCollectionsSelectAllById(
      String id,
      String lang,
      Select entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    this.selectMetadataSourcesHelper.putFincSelectMetadataSourcesCollectionsSelectAllById(
        id, entity, okapiHeaders, asyncResultHandler, vertxContext);
  }

  @Override
  @Validate
  public void getFincSelectMetadataSourcesCollectionsSelectAllById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    vertxContext.runOnContext(
        aVoid ->
            asyncResultHandler.handle(
                succeededFuture(
                    FincSelectMetadataSources
                        .GetFincSelectMetadataSourcesCollectionsSelectAllByIdResponse.status(501)
                        .build())));
  }

  @Override
  @Validate
  public void deleteFincSelectMetadataSourcesCollectionsSelectAllById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    vertxContext.runOnContext(
        aVoid ->
            asyncResultHandler.handle(
                succeededFuture(
                    FincSelectMetadataSources
                        .DeleteFincSelectMetadataSourcesCollectionsSelectAllByIdResponse.status(501)
                        .build())));
  }
}
