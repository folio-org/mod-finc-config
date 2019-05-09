package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.tools.utils.TenantLoading;

public class TenantReferenceApi extends TenantAPI {

  @Override
  public void postTenant(
      TenantAttributes entity,
      Map<String, String> headers,
      Handler<AsyncResult<Response>> handlers,
      Context context) {
    super.postTenant(
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
                            PostTenantResponse.respond201WithApplicationJson("")));
                  });
        },
        context);
  }
}
