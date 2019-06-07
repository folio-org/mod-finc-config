package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.finc.TinyMetadataSourcesHelper;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.TinyMetadataSource;
import org.folio.rest.jaxrs.resource.FincConfigTinyMetadataSources;
import org.folio.rest.persist.PostgresClient;

public class FincConfigTinyMetadataSourcesAPI implements FincConfigTinyMetadataSources {

  private final TinyMetadataSourcesHelper tinyMetadataSourcesHelper;

  public FincConfigTinyMetadataSourcesAPI(Vertx vertx, String tenantId) {
    PostgresClient.getInstance(vertx).setIdField(FincConfigMetadataSourcesAPI.ID_FIELD);
    tinyMetadataSourcesHelper = new TinyMetadataSourcesHelper(vertx);
  }

  @Override
  @Validate
  public void getFincConfigTinyMetadataSources(
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    tinyMetadataSourcesHelper.getTinyMetadataSources(lang, asyncResultHandler, vertxContext);
  }

  @Override
  @Validate
  public void postFincConfigTinyMetadataSources(
      String lang,
      TinyMetadataSource entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    vertxContext.runOnContext(
        aVoid ->
            asyncResultHandler.handle(
                succeededFuture(PostFincConfigTinyMetadataSourcesResponse.status(501).build())));
  }
}
