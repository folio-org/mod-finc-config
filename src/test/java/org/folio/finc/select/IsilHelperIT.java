package org.folio.finc.select;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.parsing.Parser;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.Isil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.rest.utils.Constants;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class IsilHelperIT {

  private static final String APPLICATION_JSON = "application/json";
  private static final String ISIL_1_ID = "dba74989-3270-430d-8679-96416a33527c";
  private static Vertx vertx;

  @Rule public Timeout timeout = Timeout.seconds(10);

  @BeforeClass
  public static void setUp(TestContext context) {
    vertx = Vertx.vertx();

    try {
      PostgresClient.setIsEmbedded(true);
      PostgresClient instance = PostgresClient.getInstance(vertx);
      instance.startEmbeddedPostgres();
    } catch (Exception e) {
      context.fail(e);
      return;
    }

    Async async = context.async();
    int port = NetworkUtils.nextFreePort();

    RestAssured.reset();
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = port;
    RestAssured.defaultParser = Parser.JSON;

    String url = "http://localhost:" + port;
    TenantClient tenantClient =
        new TenantClient(url, Constants.MODULE_TENANT, Constants.MODULE_TENANT);
    DeploymentOptions options =
        new DeploymentOptions().setConfig(new JsonObject().put("http.port", port)).setWorker(true);

    vertx.deployVerticle(
        RestVerticle.class.getName(),
        options,
        res -> {
          try {
            tenantClient.postTenant(
                null,
                postTenantRes -> {
                  postIsils(context);
                  async.complete();
                });
          } catch (Exception e) {
            context.fail(e);
          }
        });
  }

  @AfterClass
  public static void teardown(TestContext context) {
    RestAssured.reset();
    Async async = context.async();
    vertx.close(
        context.asyncAssertSuccess(
            res -> {
              PostgresClient.stopEmbeddedPostgres();
              async.complete();
            }));
  }

  private static void postIsils(TestContext context) {
    String isil1Str;
    try {
      isil1Str = new String(Files.readAllBytes(Paths.get("ramls/examples/isil1.sample")));
    } catch (IOException e) {
      context.fail(e);
      return;
    }
    Async async = context.async();
    PostgresClient.getInstance(vertx, Constants.MODULE_TENANT)
        .save(
            "isils",
            ISIL_1_ID,
            Json.decodeValue(isil1Str, Isil.class).withId(null),
            ar -> {
              if (ar.succeeded()) {
                async.complete();
              } else {
                context.fail(ar.cause());
              }
            });
    async.await();
  }

  @Test
  public void testGetIsilForTenant(TestContext testContext) {
    IsilHelper cut = new IsilHelper(vertx, Constants.MODULE_TENANT);
    Map<String, String> headers = new HashMap<>();
    headers.put("X-Okapi-Tenant", Constants.MODULE_TENANT);
    headers.put("content-type", APPLICATION_JSON);
    headers.put("accept", APPLICATION_JSON);
    Async async = testContext.async();
    cut.getIsilForTenant("ubl", headers, vertx.getOrCreateContext())
        .setHandler(
            isilResult -> {
              if (isilResult.succeeded()) {
                String isil = isilResult.result();
                testContext.assertEquals("DE-15", isil, "Isil not as expected");
                async.complete();
              } else {
                testContext.fail("Isil request not succeeded.");
              }
            });
  }
}
