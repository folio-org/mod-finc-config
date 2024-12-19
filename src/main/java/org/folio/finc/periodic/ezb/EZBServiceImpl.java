package org.folio.finc.periodic.ezb;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.net.ProxyOptions;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EZBServiceImpl implements EZBService {
  private static final Logger log = LogManager.getLogger();

  private final String url;

  public EZBServiceImpl(String url) {
    if (StringUtils.isEmpty(url)) {
      throw new IllegalArgumentException("EZB download URL cannot be null or empty");
    }
    if (StringUtils.countMatches(url, "%s") != 1) {
      throw new IllegalArgumentException(
          "EZB download URL needs to contain exactly one '%s' placeholder for the libId");
    }
    this.url = url;
  }

  private Optional<ProxyOptions> getProxyOptions(String url) {
    try {
      return ProxySelector.getDefault().select(new URI(url)).stream()
          .filter(p -> p.address() != null)
          .findFirst()
          .map(
              p -> {
                InetSocketAddress addr = (InetSocketAddress) p.address();
                return new ProxyOptions().setHost(addr.getHostName()).setPort(addr.getPort());
              });
    } catch (URISyntaxException e) {
      log.error(e.getMessage(), e);
      return Optional.empty();
    }
  }

  @Override
  public Future<String> fetchEZBFile(String user, String password, String libId, Vertx vertx) {
    Promise<String> result = Promise.promise();
    WebClient client = WebClient.create(vertx);
    String formattedUrl = String.format(url, libId);
    HttpRequest<Buffer> get =
        client.requestAbs(HttpMethod.GET, formattedUrl).basicAuthentication(user, password);
    getProxyOptions(formattedUrl).ifPresent(get::proxy);
    get.send(
        ar -> {
          if (ar.succeeded()) {
            HttpResponse<Buffer> response = ar.result();
            if (ar.result().statusCode() == 200) {
              result.complete(response.bodyAsString());
            } else {
              result.fail(
                  String.format(
                      "Failed to fetch ezb file. Status code: %s. Status message: %s. %s ",
                      response.statusCode(), response.statusMessage(), response.bodyAsString()));
            }
          } else {
            result.fail("Failed to fetch ezb file. " + ar.cause());
          }
        });
    return result.future();
  }

  public String getUrl() {
    return url;
  }
}
