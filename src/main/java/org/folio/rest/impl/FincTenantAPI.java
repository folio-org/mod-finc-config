package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.jaxrs.resource.FincTenant;
import org.folio.rest.jaxrs.resource.Tenant.DeleteTenantResponse;
import org.folio.rest.utils.Constants;

public class FincTenantAPI implements FincTenant {

  private final TenantAPI tenantAPI;

  public FincTenantAPI() {
    this.tenantAPI = new TenantReferenceApi();
  }

  @Override
  @Validate
  public void postFincTenant(
      TenantAttributes entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    okapiHeaders.put("x-okapi-tenant", Constants.MODULE_TENANT);
    this.tenantAPI.postTenant(entity, okapiHeaders, asyncResultHandler, vertxContext);
  }

  @Override
  @Validate
  public void deleteFincTenant(
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    String currentTenant = okapiHeaders.get("x-okapi-tenant");
    if (Constants.MODULE_TENANT.equals(currentTenant)) {
      this.tenantAPI.deleteTenant(okapiHeaders, asyncResultHandler, vertxContext);
    } else {
      asyncResultHandler.handle(
          Future.succeededFuture(
              DeleteTenantResponse.respond400WithTextPlain(
                  "You cannot delete tenant "
                      + currentTenant
                      + ". You can only delete tenant "
                      + Constants.MODULE_TENANT)));
    }
  }

  @Override
  @Validate
  public void getFincTenant(
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    this.tenantAPI.getTenant(okapiHeaders, asyncResultHandler, vertxContext);
  }
}
