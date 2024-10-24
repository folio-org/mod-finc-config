package org.folio.rest.impl;

import static org.folio.rest.impl.Messages.MSG_INTERNAL_SERVER_ERROR;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.finc.dao.MetadataSourcesTinyDAO;
import org.folio.finc.dao.MetadataSourcesTinyDAOImpl;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.resource.FincConfigTinyMetadataSources;
import org.folio.rest.persist.PostgresClient;

/** Manages tiny metadata sources for ui-finc-config */
public class FincConfigTinyMetadataSourcesAPI implements FincConfigTinyMetadataSources {

  private final MetadataSourcesTinyDAO metadataSourcesTinyDAO;

  public FincConfigTinyMetadataSourcesAPI(Vertx vertx, String tenantId) {
    PostgresClient.getInstance(vertx);
    metadataSourcesTinyDAO = new MetadataSourcesTinyDAOImpl();
  }

  @Override
  @Validate
  public void getFincConfigTinyMetadataSources(
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {

    metadataSourcesTinyDAO
        .getAll(vertxContext)
        .onComplete(
            ar -> {
              if (ar.succeeded()) {
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        GetFincConfigTinyMetadataSourcesResponse.respond200WithApplicationJson(
                            ar.result())));
              } else {
                Throwable cause = ar.cause();
                if (cause instanceof IllegalStateException) {
                  asyncResultHandler.handle(
                      Future.succeededFuture(
                          GetFincConfigTinyMetadataSourcesResponse.respond400WithTextPlain(
                              "CQL Illegal State Error for '"
                                  + ""
                                  + "': "
                                  + cause.getLocalizedMessage())));
                } else if (cause.getClass().getSimpleName().contains("CQLParseException")) {
                  asyncResultHandler.handle(
                      Future.succeededFuture(
                          GetFincConfigTinyMetadataSourcesResponse.respond400WithTextPlain(
                              "CQL Parsing Error for '"
                                  + ""
                                  + "': "
                                  + cause.getLocalizedMessage())));
                } else {
                  asyncResultHandler.handle(
                      io.vertx.core.Future.succeededFuture(
                          GetFincConfigTinyMetadataSourcesResponse.respond500WithTextPlain(
                              MSG_INTERNAL_SERVER_ERROR)));
                }
              }
            });
  }
}
