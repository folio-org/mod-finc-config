package org.folio.finc;

import static org.folio.rest.utils.Constants.MODULE_TENANT;

import io.restassured.RestAssured;
import io.restassured.parsing.Parser;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.folio.postgres.testing.PostgresTesterContainer;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.ModuleName;
import org.folio.rest.tools.utils.NetworkUtils;

public class TestUtils {

  public static final String TENANT_UBL = "ubl";
  public static final String TENANT_DIKU = "diku";
  private static final Vertx vertx = Vertx.vertx();
  static final WebClient WEB_CLIENT = WebClient.create(vertx);
  private static final int verticlePort = NetworkUtils.nextFreePort();
  private static final String BASE_URI = "http://localhost";
  private static final String URL = BASE_URI + ":" + verticlePort;
  private static String deploymentId;
  private static boolean isTestSuiteRunning = false;

  public static void setupTestSuite() throws Exception {
    setupRestAssured();
    setupPostgres();
    deployRestVerticle();
    setupTenants();
    isTestSuiteRunning = true;
  }

  public static void teardownTestSuite() throws Exception {
    teardownTenants();
    undeployRestVerticle();
    teardownPostgres();
    resetRestAssured();
    isTestSuiteRunning = false;
  }

  public static boolean isIsTestSuiteRunning() {
    return isTestSuiteRunning;
  }

  public static Vertx getVertx() {
    return vertx;
  }

  public static void setupPostgres() {
    PostgresClient.setPostgresTester(new PostgresTesterContainer());
    PostgresClient client = PostgresClient.getInstance(vertx);
    client.startPostgresTester();
  }

  public static void teardownPostgres() {
    PostgresClient.stopPostgresTester();
  }

  public static void deployRestVerticle()
      throws ExecutionException, InterruptedException, TimeoutException {
    DeploymentOptions options = new DeploymentOptions();
    options.setConfig(new JsonObject().put("http.port", verticlePort).put("testing", true));
    deploymentId =
        vertx
            .deployVerticle(RestVerticle.class.getName(), options)
            .toCompletionStage()
            .toCompletableFuture()
            .get(30, TimeUnit.SECONDS);
  }

  public static void undeployRestVerticle() throws ExecutionException, InterruptedException {
    vertx.undeploy(deploymentId).toCompletionStage().toCompletableFuture().get();
    deploymentId = null;
  }

  public static void setupTenants() throws ExecutionException, InterruptedException {
    TenantAttributes tenantAttributes =
        new TenantAttributes().withModuleTo(ModuleName.getModuleVersion());
    Future.all(
            Stream.of(MODULE_TENANT, TENANT_UBL, TENANT_DIKU)
                .map(tenant -> new TenantClient(URL, tenant, tenant, WEB_CLIENT))
                .map(tc -> tc.postTenant(tenantAttributes))
                .toList())
        .toCompletionStage()
        .toCompletableFuture()
        .get();
  }

  public static void teardownTenants() throws ExecutionException, InterruptedException {
    TenantAttributes tenantAttributes = new TenantAttributes().withModuleTo(null).withPurge(true);
    Future.all(
            Stream.of(MODULE_TENANT, TENANT_UBL, TENANT_DIKU)
                .map(tenant -> new TenantClient(URL, tenant, tenant, WEB_CLIENT))
                .map(tc -> tc.postTenant(tenantAttributes))
                .toList())
        .toCompletionStage()
        .toCompletableFuture()
        .get();
  }

  public static void setupRestAssured() {
    resetRestAssured();
    RestAssured.baseURI = BASE_URI;
    RestAssured.port = verticlePort;
    RestAssured.defaultParser = Parser.JSON;
  }

  public static void resetRestAssured() {
    RestAssured.reset();
  }
}
