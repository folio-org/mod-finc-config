package org.folio.finc;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.rest.utils.Constants;
import org.junit.*;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static io.restassured.RestAssured.given;

@RunWith(VertxUnitRunner.class)
public class TenantITest {

  @Rule public Timeout timeout = Timeout.seconds(10);
  @Rule public WireMockRule wireMockRule = new WireMockRule(options().dynamicPort());

  private static final String TENANT_UBL = "ubl";
  private static final String TENANT_DIKU = "diku";
  private static final String FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT =
      "/finc-config/metadata-collections";
  private static final String TENANT_ENDPOINT = "/_/tenant";

  private static Vertx vertx;
  private static int port;

  @BeforeClass
  public static void setUp(TestContext context)
      throws InterruptedException, ExecutionException, TimeoutException {
    vertx = Vertx.vertx();
    try {
      PostgresClient.setIsEmbedded(true);
      PostgresClient instance = PostgresClient.getInstance(vertx);
      instance.startEmbeddedPostgres();
    } catch (Exception e) {
      context.fail(e);
      return;
    }
    port = NetworkUtils.nextFreePort();

    RestAssured.reset();
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = port;
    RestAssured.defaultParser = Parser.JSON;
    DeploymentOptions options =
        new DeploymentOptions().setConfig(new JsonObject().put("http.port", port));
    startVerticle(options);
    // prepareTenants(context);
  }

  private static void startVerticle(DeploymentOptions options)
      throws InterruptedException, ExecutionException, TimeoutException {

    CompletableFuture<String> deploymentComplete = new CompletableFuture<>();

    vertx.deployVerticle(
        RestVerticle.class.getName(),
        options,
        res -> {
          if (res.succeeded()) {
            deploymentComplete.complete(res.result());
          } else {
            deploymentComplete.completeExceptionally(res.cause());
          }
        });

    deploymentComplete.get(30, TimeUnit.SECONDS);
  }

  @Before
  public void prepareTenants(TestContext context) throws InterruptedException {
    Async async = context.async();
    TenantUtil tenantUtil = new TenantUtil();
    tenantUtil
        .postFincTenant(port, vertx, context)
        .onSuccess(unused -> tenantUtil.postUBLTenant(port, vertx))
        .onFailure(context::fail)
        .onSuccess(
            unused ->
                tenantUtil
                    .postDikuTenant(port, vertx)
                    .onSuccess(unused1 -> async.complete())
                    .onFailure(throwable -> context.fail(throwable)))
        .onFailure(context::fail);
    async.awaitSuccess();
  }

  @AfterClass
  public static void teardown() throws InterruptedException, ExecutionException, TimeoutException {
    RestAssured.reset();
    CompletableFuture<String> undeploymentComplete = new CompletableFuture<>();
    vertx.close(
        res -> {
          if (res.succeeded()) {
            undeploymentComplete.complete(null);
          } else {
            undeploymentComplete.completeExceptionally(res.cause());
          }
        });
    undeploymentComplete.get(20, TimeUnit.SECONDS);
    PostgresClient.stopEmbeddedPostgres();
  }

  @Test
  public void testNoDeletionWithoutPurge() throws IOException, XmlPullParserException {
    String mockedOkapiUrl = "http://localhost:" + wireMockRule.port();

    // GET
    given()
        .header(XOkapiHeaders.TENANT, TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT)
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200);

    // Deactivate module for tenant UBL
    TenantAttributes attributes =
        new TenantAttributes()
            .withModuleTo(null)
            .withModuleFrom(ApiTestSuite.getModuleVersion())
            .withPurge(false);
    given()
        .body(attributes)
        .header("content-type", ContentType.JSON)
        .header(XOkapiHeaders.TENANT, TENANT_UBL)
        .header(XOkapiHeaders.URL, mockedOkapiUrl)
        .post(TENANT_ENDPOINT)
        .then()
        .statusCode(201);

    // Try to GET resource again with different tenant.
    given()
        .header(XOkapiHeaders.TENANT, TENANT_DIKU)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT)
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200);
  }

  @Test
  public void testOnlyFincCanPurge() throws IOException, XmlPullParserException {
    String mockedOkapiUrl = "http://localhost:" + wireMockRule.port();

    // GET
    given()
        .header(XOkapiHeaders.TENANT, TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT)
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200);

    // Purge module with tenant UBL
    TenantAttributes attributes =
        new TenantAttributes()
            .withModuleTo(null)
            .withModuleFrom(ApiTestSuite.getModuleVersion())
            .withPurge(true);
    given()
        .body(attributes)
        .header("content-type", ContentType.JSON)
        .header(XOkapiHeaders.TENANT, TENANT_UBL)
        .header(XOkapiHeaders.URL, mockedOkapiUrl)
        .post(TENANT_ENDPOINT)
        .then()
        .statusCode(400);

    // Deactivate module with tenant finc
    given()
        .body(attributes)
        .header("content-type", ContentType.JSON)
        .header(XOkapiHeaders.TENANT, Constants.MODULE_TENANT)
        .header(XOkapiHeaders.URL, mockedOkapiUrl)
        .post(TENANT_ENDPOINT)
        .then()
        .statusCode(201);

    // Try to GET resource again. Should fail.
    given()
        .header(XOkapiHeaders.TENANT, TENANT_DIKU)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT)
        .then()
        .statusCode(500);
  }
}
