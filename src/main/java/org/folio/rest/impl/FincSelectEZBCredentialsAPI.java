package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.finc.dao.EZBCredentialsDAOImpl.EZBCredentialsException;
import org.folio.finc.dao.IsilDAO;
import org.folio.finc.dao.IsilDAOImpl;
import org.folio.finc.dao.SelectEZBCredentialsDAO;
import org.folio.finc.dao.SelectEZBCredentialsDAOImpl;
import org.folio.rest.RestVerticle;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.Credential;
import org.folio.rest.jaxrs.resource.FincSelectEzbCredentials;
import org.folio.rest.jaxrs.resource.FincSelectMetadataCollections.GetFincSelectMetadataCollectionsResponse;
import org.folio.rest.tools.utils.TenantTool;

public class FincSelectEZBCredentialsAPI implements FincSelectEzbCredentials {

  private final IsilDAO isilDAO;
  private final SelectEZBCredentialsDAO selectEZBCredentialsDAO;

  public FincSelectEZBCredentialsAPI() {
    this.isilDAO = new IsilDAOImpl();
    this.selectEZBCredentialsDAO = new SelectEZBCredentialsDAOImpl();
  }

  @Override
  @Validate
  public void getFincSelectEzbCredentials(Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    String tenantId =
        TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));

    isilDAO
        .getIsilForTenant(tenantId, vertxContext)
        .compose(isil ->
            selectEZBCredentialsDAO.getByIsil(isil, vertxContext)
        )
        .setHandler(ar -> {
          if (ar.succeeded()) {
            Credential cred = ar.result();
            asyncResultHandler.handle(
                Future.succeededFuture(
                    GetFincSelectEzbCredentialsResponse.respond200WithApplicationJson(
                        cred.withId(null))));
          } else {
            asyncResultHandler.handle(
                Future.succeededFuture(
                    GetFincSelectMetadataCollectionsResponse.respond500WithTextPlain(
                        ar.cause())));
          }
        });
  }

  @Override
  @Validate
  public void putFincSelectEzbCredentials(Credential entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    String tenantId =
        TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
    isilDAO.getIsilForTenant(tenantId, vertxContext)
        .compose(isil -> {
              if (!isil.equals(entity.getIsil())) {
                return Future.failedFuture(new EZBCredentialsException(
                    "Wrong isil specified for tenant."));
              } else {
                return selectEZBCredentialsDAO
                    .upsert(entity.withIsil(isil), vertxContext);
              }
            }
        )
        .setHandler(ar -> {
          if (ar.succeeded()) {
            Credential cred = ar.result();
            asyncResultHandler.handle(Future.succeededFuture(
                PutFincSelectEzbCredentialsResponse.respond200WithApplicationJson(cred)));
          } else {
            if (ar.cause() instanceof EZBCredentialsException) {
              asyncResultHandler.handle(Future.succeededFuture(PutFincSelectEzbCredentialsResponse
                  .respond400WithTextPlain(ar.cause().getLocalizedMessage())));
            } else {
              asyncResultHandler.handle(Future.succeededFuture(
                  PutFincSelectEzbCredentialsResponse
                      .respond500WithTextPlain(ar.cause().getLocalizedMessage())));
            }
          }
        });
  }
}
