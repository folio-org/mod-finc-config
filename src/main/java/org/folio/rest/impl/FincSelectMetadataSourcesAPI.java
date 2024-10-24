package org.folio.rest.impl;

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

/**
 * Manages metadata sources for ui-finc-select, hence depends on isil/tenant.
 */
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
      String totalRecords,
      int offset,
      int limit,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {

    String tenantId =
        TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
    isilDAO
        .getIsilForTenant(tenantId, vertxContext)
        .compose(isil -> selectMetadataSourcesDAO.getAll(query, offset, limit, isil, vertxContext))
        .onComplete(
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
  public void getFincSelectMetadataSourcesById(
      String id,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {

    String tenantId =
        TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
    isilDAO
        .getIsilForTenant(tenantId, vertxContext)
        .compose(isil -> selectMetadataSourcesDAO.getById(id, isil, vertxContext))
        .onComplete(
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
  public void putFincSelectMetadataSourcesCollectionsSelectAllById(
      String id,
      Select entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    this.selectMetadataSourcesHelper.selectAllCollectionsOfMetadataSource(
        id, entity, okapiHeaders, asyncResultHandler, vertxContext);
  }
}
