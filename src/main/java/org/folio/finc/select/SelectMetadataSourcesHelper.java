package org.folio.finc.select;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.finc.select.verticles.AbstractSelectMetadataSourceVerticle;
import org.folio.finc.select.verticles.factory.SelectMetadataSourceVerticleFactory;
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.Select;
import org.folio.rest.jaxrs.resource.FincSelectMetadataSources.PutFincSelectMetadataSourcesCollectionsSelectAllByIdResponse;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.TenantTool;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

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
            deploySelectSourceVerticle(
                vertxContext.owner(), metadataSourceID, tenantId, selectEntity);
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

  private void deploySelectSourceVerticle(
      Vertx vertx, String metadataSourceId, String tenantId, Select select) {

    AbstractSelectMetadataSourceVerticle verticle =
        SelectMetadataSourceVerticleFactory.create(vertx, vertx.getOrCreateContext(), select);

    vertx = Vertx.vertx();
    JsonObject cfg = vertx.getOrCreateContext().config();
    cfg.put("tenantId", tenantId);
    cfg.put("metadataSourceId", metadataSourceId);
    vertx.deployVerticle(
        verticle,
        new DeploymentOptions().setConfig(cfg),
        stringAsyncResult -> {
          if (stringAsyncResult.failed()) {
            logger.error(
                String.format(
                    "Failed to deploy SelectVerticle for metadata source %s and for tenant %s: %s",
                    metadataSourceId, tenantId, stringAsyncResult.cause().getMessage()),
                stringAsyncResult.cause());
          }
        });
  }
}
