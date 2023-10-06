package org.folio.finc.select;

import io.restassured.RestAssured;
import io.restassured.parsing.Parser;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.folio.finc.ApiTestSuite;
import org.folio.finc.TenantUtil;
import org.folio.postgres.testing.PostgresTesterContainer;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.Select;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.rest.utils.Constants;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class SelectMetadataSourcesHelperTest {

  private static final String TENANT_UBL = "ubl";
  private static Vertx vertx;
  private static SelectMetadataSourcesHelper cut;
  @Rule public Timeout timeout = Timeout.seconds(10);

  @BeforeClass
  public static void setUp(TestContext context)
      throws InterruptedException, ExecutionException, TimeoutException {
    vertx = Vertx.vertx();
    PostgresClient.setPostgresTester(new PostgresTesterContainer());
    PostgresClient.getInstance(vertx);

    int port = NetworkUtils.nextFreePort();

    RestAssured.reset();
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = port;
    RestAssured.defaultParser = Parser.JSON;

    DeploymentOptions options =
        new DeploymentOptions().setConfig(new JsonObject().put("http.port", port));

    startVerticle(options);
    prepareTenants();
    cut = new SelectMetadataSourcesHelper(vertx, TENANT_UBL);
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

  private static void prepareTenants() {
    String url = RestAssured.baseURI + ":" + RestAssured.port;
    try {
      CompletableFuture fincFuture = new CompletableFuture();
      CompletableFuture ublFuture = new CompletableFuture();
      WebClient webClient = WebClient.create(vertx);
      TenantClient tenantClientFinc =
          new TenantClient(url, Constants.MODULE_TENANT, Constants.MODULE_TENANT, webClient);
      TenantClient tenantClientUbl = new TenantClient(url, TENANT_UBL, TENANT_UBL, webClient);
      tenantClientFinc.postTenant(
          new TenantAttributes().withModuleTo(ApiTestSuite.getModuleVersion()),
          fincFuture::complete);
      tenantClientUbl.postTenant(
          new TenantAttributes().withModuleTo(ApiTestSuite.getModuleVersion()),
          ublFuture::complete);
      fincFuture.get(30, TimeUnit.SECONDS);
      ublFuture.get(30, TimeUnit.SECONDS);
    } catch (Exception e) {
      assert false;
    }
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
    PostgresClient.stopPostgresTester();
  }

  @Test
  public void testSuccessfulSelect(TestContext context) {

    Select select = new Select().withSelect(true);

    Map<String, String> header = new HashMap<>();
    header.put("X-Okapi-Tenant", TENANT_UBL);
    header.put("content-type", "application/json");
    header.put("accept", "application/json");

    Async async = context.async();
    cut.selectAllCollectionsOfMetadataSource(
        TenantUtil.getMetadataSource2().getId(),
        select,
        header,
        ar -> {
          if (ar.succeeded()) {
            async.complete();
          } else {
            context.fail();
          }
        },
        vertx.getOrCreateContext());
  }
}
