package org.folio.finc.periodic.ezb;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

public class EZBServiceImpl implements EZBService {

  private String url =
      "https://rzbezb2.ur.de/ezb/export/licenselist_html.php?pack=0&bibid=%s&lang=de&"
          + "output_style=kbart&todo_license=ALkbart";

  public EZBServiceImpl(String url) {
    this.url = url;
  }

  public EZBServiceImpl() {}

  @Override
  public Future<String> fetchEZBFile(String user, String password, String libId, Vertx vertx) {
    Promise<String> result = Promise.promise();
    WebClient client = WebClient.create(vertx);
    String formattedUrl = String.format(url, libId);
    HttpRequest<Buffer> get =
        client.requestAbs(HttpMethod.GET, formattedUrl).basicAuthentication(user, password);
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
}
