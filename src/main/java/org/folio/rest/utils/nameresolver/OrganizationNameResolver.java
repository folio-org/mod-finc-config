package org.folio.rest.utils.nameresolver;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import java.util.Map;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.okapi.common.XOkapiHeaders;

public class OrganizationNameResolver {

  private static final Logger logger = LogManager.getLogger(OrganizationNameResolver.class);

  private static final String ORGANIZATION_ENDPOINT = "/organizations-storage/organizations/";

  private OrganizationNameResolver() {
    throw new IllegalStateException("Utility class");
  }

  public static Future<String> resolveName(
      String organizationId, Map<String, String> okapiHeaders, Context vertxContext) {
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

    return request
        .send()
        .map(
            response -> {
              if (response.statusCode() == 200) {
                JsonObject orgaJson = response.bodyAsJsonObject();
                String orgaName = orgaJson.getString("name");
                logger.info("Found organization name {} for id {} ", orgaName, organizationId);
                return orgaName;
              } else {
                logger.warn(
                    "Failure while looking for organization name for id {}. Will proceed without"
                        + " setting orga name.",
                    organizationId);
                return null;
              }
            });
  }
}
