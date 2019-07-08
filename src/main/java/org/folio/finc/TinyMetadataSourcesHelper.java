package org.folio.finc;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.List;
import javax.ws.rs.core.Response;
import org.folio.rest.impl.FincConfigMetadataSourcesAPI;
import org.folio.rest.jaxrs.model.TinyMetadataSource;
import org.folio.rest.jaxrs.model.TinyMetadataSources;
import org.folio.rest.jaxrs.resource.FincConfigTinyMetadataSources.GetFincConfigTinyMetadataSourcesResponse;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.messages.MessageConsts;
import org.folio.rest.tools.messages.Messages;
import org.folio.rest.utils.Constants;

public class TinyMetadataSourcesHelper {

  private static final Logger logger = LoggerFactory.getLogger(TinyMetadataSourcesHelper.class);
  private static final String TABLE_NAME = "metadata_sources_tiny";
  private final Messages messages = Messages.getInstance();

  public TinyMetadataSourcesHelper(Vertx vertx) {
    PostgresClient.getInstance(vertx);
  }

  public void getTinyMetadataSources(
      String lang, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    logger.debug("Getting tiny metadata sources");
    try {
      vertxContext.runOnContext(
          v -> {
            String tenantId = Constants.MODULE_TENANT;
            String field = "*";
            try {
              PostgresClient.getInstance(vertxContext.owner(), tenantId)
                  .get(
                      TABLE_NAME,
                      TinyMetadataSource.class,
                      "",
                      false,
                      false,
                      reply -> {
                        try {
                          if (reply.succeeded()) {
                            TinyMetadataSources tinySourcesCollection = new TinyMetadataSources();
                            List<TinyMetadataSource> sources = reply.result().getResults();
                            tinySourcesCollection.setTinyMetadataSources(sources);
                            tinySourcesCollection.setTotalRecords(
                                reply.result().getResultInfo().getTotalRecords());
                            asyncResultHandler.handle(
                                Future.succeededFuture(
                                    GetFincConfigTinyMetadataSourcesResponse
                                        .respond200WithApplicationJson(tinySourcesCollection)));
                          } else {
                            asyncResultHandler.handle(
                                Future.succeededFuture(
                                    GetFincConfigTinyMetadataSourcesResponse
                                        .respond500WithTextPlain(
                                            messages.getMessage(
                                                lang, MessageConsts.InternalServerError))));
                          }
                        } catch (Exception e) {
                          logger.debug(e.getLocalizedMessage());
                          asyncResultHandler.handle(
                              Future.succeededFuture(
                                  GetFincConfigTinyMetadataSourcesResponse.respond500WithTextPlain(
                                      messages.getMessage(
                                          lang, MessageConsts.InternalServerError))));
                        }
                      });
            } catch (IllegalStateException e) {
              logger.debug("IllegalStateException: " + e.getLocalizedMessage());
              asyncResultHandler.handle(
                  Future.succeededFuture(
                      GetFincConfigTinyMetadataSourcesResponse.respond400WithTextPlain(
                          "CQL Illegal State Error for '" + "" + "': " + e.getLocalizedMessage())));
            } catch (Exception e) {
              Throwable cause = e;
              while (cause.getCause() != null) {
                cause = cause.getCause();
              }
              logger.debug(
                  "Got error " + cause.getClass().getSimpleName() + ": " + e.getLocalizedMessage());
              if (cause.getClass().getSimpleName().contains("CQLParseException")) {
                logger.debug("BAD CQL");
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        GetFincConfigTinyMetadataSourcesResponse.respond400WithTextPlain(
                            "CQL Parsing Error for '" + "" + "': " + cause.getLocalizedMessage())));
              } else {
                asyncResultHandler.handle(
                    io.vertx.core.Future.succeededFuture(
                        GetFincConfigTinyMetadataSourcesResponse.respond500WithTextPlain(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
              }
            }
          });
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage(), e);
      if (e.getCause() != null
          && e.getCause().getClass().getSimpleName().contains("CQLParseException")) {
        logger.debug("BAD CQL");
        asyncResultHandler.handle(
            Future.succeededFuture(
                GetFincConfigTinyMetadataSourcesResponse.respond400WithTextPlain(
                    "CQL Parsing Error for '" + "" + "': " + e.getLocalizedMessage())));
      } else {
        asyncResultHandler.handle(
            io.vertx.core.Future.succeededFuture(
              GetFincConfigTinyMetadataSourcesResponse.respond500WithTextPlain(
                    messages.getMessage(lang, MessageConsts.InternalServerError))));
      }
    }
  }
}
