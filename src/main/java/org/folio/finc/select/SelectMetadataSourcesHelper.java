package org.folio.finc.select;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.finc.select.services.AbstractSelectMetadataSourceService;
import org.folio.finc.select.services.factory.SelectMetadataSourceServiceFactory;
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.Select;
import org.folio.rest.jaxrs.resource.FincSelectMetadataSources.PutFincSelectMetadataSourcesCollectionsSelectAllByIdResponse;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.TenantTool;

/** Helper class to select/unselect metadata sources for finc-select. */
public class SelectMetadataSourcesHelper {
  private final Logger logger = LogManager.getLogger(SelectMetadataSourcesHelper.class);

  public SelectMetadataSourcesHelper(Vertx vertx, String tenantId) {
    PostgresClient.getInstance(vertx);
  }

  public void selectAllCollectionsOfMetadataSource(
      String metadataSourceID,
      Select selectEntity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    try {
      vertxContext.runOnContext(
          v -> {
            String tenantId =
                TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
            String msg =
                String.format(
                    "Will (un)select metadata collections of metadata source %s for tenant %s.",
                    metadataSourceID, tenantId);
            logger.info(msg);
            executeSelectService(vertxContext, metadataSourceID, tenantId, selectEntity);
            String result = new JsonObject().put("message", msg).toString();
            asyncResultHandler.handle(
                Future.succeededFuture(
                    Response.ok(result, MediaType.APPLICATION_JSON_TYPE).build()));
          });
    } catch (Exception e) {
      asyncResultHandler.handle(
          Future.succeededFuture(
              PutFincSelectMetadataSourcesCollectionsSelectAllByIdResponse.respond500WithTextPlain(
                  e.getCause())));
    }
  }

  private void executeSelectService(
      Context context, String metadataSourceId, String tenantId, Select select) {

    AbstractSelectMetadataSourceService service =
        SelectMetadataSourceServiceFactory.create(context, select);

    service
        .selectAllCollections(metadataSourceId, tenantId)
        .onFailure(
            err ->
                logger.error(
                    String.format(
                        "Failed to (un)select collections for metadata source %s and tenant %s: %s",
                        metadataSourceId, tenantId, err.getMessage()),
                    err));
  }
}
