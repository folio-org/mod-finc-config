package org.folio.finc.select;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.Arrays;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.rest.impl.IsilsAPI;
import org.folio.rest.jaxrs.model.Isil;
import org.folio.rest.jaxrs.model.Isils;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.tools.messages.Messages;
import org.z3950.zing.cql.cql2pgjson.CQL2PgJSON;
import org.z3950.zing.cql.cql2pgjson.FieldException;

public class IsilHelper {

  private static final String ID_FIELD = "_id";
  private static final String TABLE_NAME = "isils";
  private final Messages messages = Messages.getInstance();
  private final Logger logger = LoggerFactory.getLogger(IsilHelper.class);
  private IsilsAPI isilsAPI;

  public IsilHelper(Vertx vertx, String tenantId) {
    //    PostgresClient.getInstance(vertx).setIdField(ID_FIELD);
    isilsAPI = new IsilsAPI(vertx, tenantId);
  }

  public Future<String> getIsilForTenant(
      String tenantId, Map<String, String> okapiHeaders, Context vertxContext) {
    Future<String> isilFuture = Future.future();
    String query = String.format("query=(tenant=%s)", tenantId);
    isilsAPI.getIsils(
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
              Isil isilObject = isils.getIsils().get(0);
              isilFuture.complete(isilObject.getIsil());
            } else {
              isilFuture.fail("Result is not of type Isils.");
            }
          } else {
            logger.error("Unable to get isils");
          }
        },
        vertxContext);

    return isilFuture;
  }
}
