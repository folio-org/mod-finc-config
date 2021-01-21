package org.folio.rest.utils.nameresolver;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.okapi.common.XOkapiHeaders;

import java.util.Map;

public class OrganizationNameResolver {

  private static final Logger logger = LogManager.getLogger(OrganizationNameResolver.class);

  private static final String ORGANIZATION_ENDPOINT = "/organizations-storage/organizations/";

  private OrganizationNameResolver() {
    throw new IllegalStateException("Utility class");
  }

  public static Future<String> resolveName(
      String organizationId, Map<String, String> okapiHeaders, Context vertxContext) {
    Promise<String> result = Promise.promise();

    if (organizationId == null) {
      return Future.succeededFuture();
    }

    String endpoint = ORGANIZATION_ENDPOINT + organizationId;
    WebClient webClient = WebClient.create(vertxContext.owner());
    String url =
        ObjectUtils.firstNonNull(
                okapiHeaders.get(XOkapiHeaders.URL_TO), okapiHeaders.get(XOkapiHeaders.URL))
            + endpoint;
    HttpRequest<Buffer> request = webClient.requestAbs(HttpMethod.GET, url);

    okapiHeaders.forEach(request::putHeader);
    request.putHeader("accept", "application/json");

    request.send(
        ar -> {
          if (ar.succeeded()) {
            if (ar.result().statusCode() == 200) {
              JsonObject orgaJson = ar.result().bodyAsJsonObject();
              String orgaName = orgaJson.getString("name");
              logger.info("Found organization name {} for id {} ", orgaName, organizationId);
              result.complete(orgaName);
            } else {
              logger.warn(
                  "Failure while looking for organization name for id {}. Will proceed without setting orga name. {}",
                  organizationId,
                  ar.cause());
              result.complete();
            }
          } else {
            result.fail(ar.cause());
          }
        });
    return result.future();
  }
}
