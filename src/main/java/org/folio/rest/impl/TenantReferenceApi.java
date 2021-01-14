package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.jaxrs.model.TenantJob;
import org.folio.rest.tools.utils.TenantLoading;
import org.folio.rest.utils.Constants;

public class TenantReferenceApi extends TenantAPI {

  private static final String X_OKAPI_TENANT = "x-okapi-tenant";

  @Override
  @Validate
  public void postTenant(
      TenantAttributes entity,
      Map<String, String> headers,
      Handler<AsyncResult<Response>> handlers,
      Context context) {

    if (Boolean.TRUE.equals(entity.getPurge())
        && !Constants.MODULE_TENANT.equals(headers.get(X_OKAPI_TENANT))) {
      handlers.handle(
          Future.succeededFuture(
              PostTenantResponse.respond400WithTextPlain(
                  String.format("Cannot purge tenant %s", headers.get(X_OKAPI_TENANT)))));
    }

    headers.put(X_OKAPI_TENANT, Constants.MODULE_TENANT);
    super.postTenantSync(
        entity,
        headers,
        ar -> {
          if (ar.failed()) {
            handlers.handle(ar);
            return;
          }

          // load data here
          TenantLoading tl = new TenantLoading();
          tl.withKey("loadSample")
              .withLead("sample-data")
              .add("finc-config/metadata-sources")
              .add("finc-config/metadata-collections")
              .add("finc-config/isils")
              .perform(
                  entity,
                  headers,
                  context.owner(),
                  ar2 -> {
                    if (ar2.failed()) {
                      handlers.handle(
                          Future.succeededFuture(
                              PostTenantResponse.respond500WithTextPlain(
                                  ar2.cause().getLocalizedMessage())));
                      return;
                    }
                    handlers.handle(
                        Future.succeededFuture(
                            PostTenantResponse.respond201WithApplicationJson(
                                new TenantJob(), PostTenantResponse.headersFor201())));
                  });
        },
        context);
  }
}
