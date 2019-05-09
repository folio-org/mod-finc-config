package org.folio.finc.select;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.rest.impl.IsilsAPI;
import org.folio.rest.jaxrs.model.Isil;
import org.folio.rest.jaxrs.model.Isils;
import org.folio.rest.tools.messages.Messages;

public class IsilHelper {

  private static final String ID_FIELD = "_id";
  private static final String TABLE_NAME = "isils";
  private final Messages messages = Messages.getInstance();
  private final Logger logger = LoggerFactory.getLogger(IsilHelper.class);
  private final IsilsAPI isilsAPI;

  public IsilHelper(Vertx vertx, String tenantId) {
    isilsAPI = new IsilsAPI(vertx, tenantId);
  }

  public Future<String> getIsilForTenant(
      String tenantId, Map<String, String> okapiHeaders, Context vertxContext) {
    Future<String> isilFuture = Future.future();
    String query = String.format("query=(tenant=%s)", tenantId);
    isilsAPI.getFincConfigIsils(
        query,
        0,
        1,
        "en",
        okapiHeaders,
        responseAsyncResult -> {
          if (responseAsyncResult.succeeded()) {
            Response result = responseAsyncResult.result();
            Object entity = result.getEntity();
            if (entity instanceof Isils) {
              Isils isils = (Isils) entity;
              if (isils.getIsils().size() > 0) {
                Isil isilObject = isils.getIsils().get(0);
                isilFuture.complete(isilObject.getIsil());
              } else {
                isilFuture.fail("Isil not found.");
              }
            } else {
              isilFuture.fail("Error while getting isil");
            }
          } else {
            isilFuture.fail("Error while getting isil");
          }
        },
        vertxContext);

    return isilFuture;
  }
}
