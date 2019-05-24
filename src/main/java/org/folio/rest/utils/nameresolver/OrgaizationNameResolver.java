package org.folio.rest.utils.nameresolver;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import java.util.Map;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.okapi.common.XOkapiHeaders;

public class OrgaizationNameResolver {

  private static final Logger logger = LoggerFactory.getLogger(OrgaizationNameResolver.class);

  private static final String ORGANIZATION_ENDPOINT = "/organizations-storage/organizations/";

  private OrgaizationNameResolver() {
    throw new IllegalStateException("Utility class");
  }

  public static Future<String> resolveName(
      String organizationId, Map<String, String> okapiHeaders, Context vertxContext) {
    Future<String> future = Future.future();

    if (organizationId == null) {
      return Future.succeededFuture();
    }

    String endpoint = ORGANIZATION_ENDPOINT + organizationId;
    WebClient webClient = WebClient.create(vertxContext.owner());
    String url =
        ObjectUtils.firstNonNull(
                okapiHeaders.get(XOkapiHeaders.URL_TO), okapiHeaders.get(XOkapiHeaders.URL))
            + endpoint;
    HttpRequest<Buffer> request = webClient.getAbs(url);

    okapiHeaders.forEach(request::putHeader);
    request.putHeader("accept", "application/json");

    request.send(
        ar -> {
          if (ar.succeeded()) {
            if (ar.result().statusCode() == 200) {
              JsonObject orgaJson = ar.result().bodyAsJsonObject();
              String orgaName = orgaJson.getString("name");
              logger.info("Found organization name " + orgaName + " for id " + organizationId);
              future.complete(orgaName);
            } else {
              future.fail(
                  String.format(
                      "%s %s: %s",
                      ar.result().statusCode(),
                      ar.result().statusMessage(),
                      ar.result().bodyAsString()));
            }
          } else {
            future.fail(ar.cause());
          }
        });
    return future;
  }
}
