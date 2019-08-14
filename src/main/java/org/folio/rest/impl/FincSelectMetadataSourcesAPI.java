package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.cql2pgjson.exception.FieldException;
import org.folio.finc.dao.IsilDAO;
import org.folio.finc.dao.IsilDAOImpl;
import org.folio.finc.dao.SelectMetadataSourcesDAO;
import org.folio.finc.dao.SelectMetadataSourcesDAOImpl;
import org.folio.finc.select.SelectMetadataSourcesHelper;
import org.folio.rest.RestVerticle;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.FincSelectMetadataSource;
import org.folio.rest.jaxrs.model.FincSelectMetadataSourcesGetOrder;
import org.folio.rest.jaxrs.model.Select;
import org.folio.rest.jaxrs.resource.FincSelectMetadataSources;
import org.folio.rest.tools.utils.TenantTool;

public class FincSelectMetadataSourcesAPI implements FincSelectMetadataSources {

  private final SelectMetadataSourcesHelper selectMetadataSourcesHelper;
  private final SelectMetadataSourcesDAO selectMetadataSourcesDAO;
  private final IsilDAO isilDAO;

  public FincSelectMetadataSourcesAPI(Vertx vertx, String tenantId) {
    this.selectMetadataSourcesHelper = new SelectMetadataSourcesHelper(vertx, tenantId);
    this.selectMetadataSourcesDAO = new SelectMetadataSourcesDAOImpl();
    this.isilDAO = new IsilDAOImpl();
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

    String tenantId =
        TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
    isilDAO
        .getIsilForTenant(tenantId, vertxContext)
        .compose(isil -> selectMetadataSourcesDAO.getAll(query, offset, limit, isil, vertxContext))
        .setHandler(
            ar -> {
              if (ar.succeeded()) {
                org.folio.rest.jaxrs.model.FincSelectMetadataSources metadataSources = ar.result();
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        GetFincSelectMetadataSourcesResponse.respond200WithApplicationJson(
                            metadataSources)));
              } else {
                if (ar.cause() instanceof FieldException) {
                  asyncResultHandler.handle(
                      Future.succeededFuture(
                          GetFincSelectMetadataSourcesResponse.respond400WithTextPlain(
                              ar.cause())));
                } else {
                  asyncResultHandler.handle(
                      Future.succeededFuture(
                          GetFincSelectMetadataSourcesResponse.respond500WithTextPlain(
                              ar.cause())));
                }
              }
            });
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

    String tenantId =
        TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
    isilDAO
        .getIsilForTenant(tenantId, vertxContext)
        .compose(isil -> selectMetadataSourcesDAO.getById(id, isil, vertxContext))
        .setHandler(
            ar -> {
              if (ar.succeeded()) {
                FincSelectMetadataSource fincSelectMetadataSource = ar.result();
                if (fincSelectMetadataSource == null) {
                  asyncResultHandler.handle(
                      Future.succeededFuture(
                          GetFincSelectMetadataSourcesByIdResponse.respond404WithTextPlain(
                              ar.cause())));
                } else {
                  asyncResultHandler.handle(
                      Future.succeededFuture(
                          GetFincSelectMetadataSourcesByIdResponse.respond200WithApplicationJson(
                              fincSelectMetadataSource)));
                }
              } else {
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        GetFincSelectMetadataSourcesByIdResponse.respond500WithTextPlain(
                            ar.cause())));
              }
            });
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
    this.selectMetadataSourcesHelper.selectAllCollectionsOfMetadataSource(
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
