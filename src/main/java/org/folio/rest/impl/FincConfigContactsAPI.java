package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.finc.dao.MetadataSourcesDAO;
import org.folio.finc.dao.MetadataSourcesDAOImpl;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.Contact;
import org.folio.rest.jaxrs.resource.FincConfigContacts;
import org.folio.rest.persist.PostgresClient;

/**
 * Get contacts for ui-finc-config
 */
public class FincConfigContactsAPI implements FincConfigContacts {

  private final MetadataSourcesDAO metadataSourcesDAO;

  public FincConfigContactsAPI(Vertx vertx, String tenantId) {
    PostgresClient.getInstance(vertx);
    metadataSourcesDAO = new MetadataSourcesDAOImpl();
  }

  @Override
  @Validate
  public void getFincConfigContacts(
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    metadataSourcesDAO
        .getContacts(vertxContext)
        .onSuccess(
            contacts ->
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        GetFincConfigContactsResponse.respond200WithApplicationJson(contacts))))
        .onFailure(
            throwable ->
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        GetFincConfigContactsResponse.respond500WithTextPlain(throwable))));
  }

  @Override
  public void postFincConfigContacts(
      Contact entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    vertxContext.runOnContext(
        aVoid -> asyncResultHandler.handle(succeededFuture(Response.status(501).build())));
  }
}
