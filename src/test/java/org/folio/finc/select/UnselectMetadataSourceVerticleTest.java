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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.folio.finc.TenantUtil;
import org.folio.finc.select.verticles.UnselectMetadataSourceVerticle;
import org.folio.postgres.testing.PostgresTesterContainer;
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.rest.utils.Constants;
import org.junit.*;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class UnselectMetadataSourceVerticleTest {

  private static final String TENANT_UBL = "ubl";
  private static Vertx vertx;
  private static int port;
  private static UnselectMetadataSourceVerticle cut;
  @Rule public Timeout timeout = Timeout.seconds(10);

  @BeforeClass
  public static void setUp(TestContext context)
      throws InterruptedException, ExecutionException, TimeoutException {
    vertx = Vertx.vertx();
    PostgresClient.setPostgresTester(new PostgresTesterContainer());
    PostgresClient.getInstance(vertx);

    port = NetworkUtils.nextFreePort();

    RestAssured.reset();
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = port;
    RestAssured.defaultParser = Parser.JSON;

    DeploymentOptions options =
        new DeploymentOptions().setConfig(new JsonObject().put("http.port", port));

    startVerticle(options);
    prepareTenants(context);
    cut = new UnselectMetadataSourceVerticle(vertx, vertx.getOrCreateContext());
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

  private static void prepareTenants(TestContext context) {
    Async async = context.async();
    TenantUtil tenantUtil = new TenantUtil();
    tenantUtil
        .postFincTenant(port, vertx, context)
        .onSuccess(
            unused ->
                tenantUtil
                    .postUBLTenant(port, vertx)
                    .onSuccess(unused1 -> async.complete())
                    .onFailure(context::fail))
        .onFailure(context::fail);
    async.await();
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

  @Before
  public void before() throws InterruptedException, ExecutionException, TimeoutException {
    JsonObject cfg2 = vertx.getOrCreateContext().config();
    cfg2.put("tenantId", TENANT_UBL);
    cfg2.put("metadataSourceId", TenantUtil.getMetadataSource2().getId());
    cfg2.put("testing", true);

    CompletableFuture<String> deploymentComplete = new CompletableFuture<>();
    vertx.deployVerticle(
        cut,
        new DeploymentOptions().setConfig(cfg2),
        res -> {
          if (res.succeeded()) {
            deploymentComplete.complete(res.result());
          } else {
            deploymentComplete.completeExceptionally(res.cause());
          }
        });
    deploymentComplete.get(30, TimeUnit.SECONDS);
  }

  @Test
  public void testSuccessfulUnSelect(TestContext context) {
    Async async = context.async();
    cut.selectAllCollections(TenantUtil.getMetadataSource1().getId(), TENANT_UBL)
        .onComplete(
            aVoid -> {
              if (aVoid.succeeded()) {
                try {
                  Criteria labelCrit =
                      new Criteria()
                          .addField("'label'")
                          .setJSONB(true)
                          .setOperation("=")
                          .setVal(TenantUtil.getMetadataCollection1().getLabel());
                  Criterion criterion = new Criterion(labelCrit);
                  PostgresClient.getInstance(vertx, Constants.MODULE_TENANT)
                      .get(
                          "metadata_collections",
                          FincConfigMetadataCollection.class,
                          criterion,
                          true,
                          true,
                          ar -> {
                            if (ar.succeeded()) {
                              if (ar.result() != null) {
                                FincConfigMetadataCollection collection =
                                    ar.result().getResults().get(0);
                                if (collection == null) {
                                  context.fail("No results found.");
                                } else {
                                  context.assertFalse(collection.getSelectedBy().contains("DE-15"));
                                }
                              } else {
                                context.fail("No results found.");
                              }
                              async.complete();
                            } else {
                              context.fail(ar.cause().toString());
                            }
                          });
                } catch (Exception e) {
                  context.fail(e);
                }
              } else {
                context.fail();
              }
            });
  }
}
