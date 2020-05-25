package org.folio.finc.select;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.parsing.Parser;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
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
import org.folio.finc.ApiTestSuite;
import org.folio.finc.select.verticles.SelectMetadataSourceVerticle;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.rest.utils.Constants;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class SelectMetadataSourceVerticleTest {

  private static final String TENANT_UBL = "ubl";
  private static Vertx vertx = Vertx.vertx();
  private static SelectMetadataSourceVerticle cut;
  @Rule
  public Timeout timeout = Timeout.seconds(1000);

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
    int port = NetworkUtils.nextFreePort();

    RestAssured.reset();
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = port;
    RestAssured.defaultParser = Parser.JSON;
    DeploymentOptions options =
        new DeploymentOptions().setConfig(new JsonObject().put("http.port", port)).setWorker(true);
    startVerticle(options);
    prepareTenants(context);
    cut = new SelectMetadataSourceVerticle(vertx, vertx.getOrCreateContext());
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
    SelectMetadataSourceVerticleTestHelper selectMetadataSourceVerticleTestHelper =
        new SelectMetadataSourceVerticleTestHelper();
    String url = RestAssured.baseURI + ":" + RestAssured.port;
    try {
      CompletableFuture fincFuture = new CompletableFuture();
      CompletableFuture ublFuture = new CompletableFuture();
      TenantClient tenantClientFinc =
          new TenantClient(url, Constants.MODULE_TENANT, Constants.MODULE_TENANT);
      TenantClient tenantClientUbl = new TenantClient(url, TENANT_UBL, TENANT_UBL);
      tenantClientFinc.postTenant(
          new TenantAttributes().withModuleTo(ApiTestSuite.getModuleVersion()),
          postTenantRes -> {
            Future<Void> future =
                selectMetadataSourceVerticleTestHelper.writeDataToDB(context, vertx);
            future.setHandler(
                ar -> {
                  fincFuture.complete(postTenantRes);
                });
          });
      tenantClientUbl.postTenant(
          new TenantAttributes().withModuleTo(ApiTestSuite.getModuleVersion()),
          ublFuture::complete
      );
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
    PostgresClient.stopEmbeddedPostgres();
  }

  @Before
  public void before() throws InterruptedException, ExecutionException, TimeoutException {
//    vertx = Vertx.vertx();
    JsonObject cfg2 = vertx.getOrCreateContext().config();
    cfg2.put("tenantId", TENANT_UBL);
    cfg2.put(
        "metadataSourceId", SelectMetadataSourceVerticleTestHelper.getMetadataSource2().getId());
    cfg2.put("testing", true);

    CompletableFuture<String> deploymentComplete = new CompletableFuture<>();
    vertx.deployVerticle(
        cut,
        new DeploymentOptions().setConfig(cfg2).setWorker(true),
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
  public void testSuccessfulSelect(TestContext context) {
    Async async = context.async();
    cut.selectAllCollections(
        SelectMetadataSourceVerticleTestHelper.getMetadataSource2().getId(), TENANT_UBL)
        .setHandler(
            aVoid -> {
              if (aVoid.succeeded()) {
                try {
                  Criteria labelCrit =
                      new Criteria()
                          .addField("'label'")
                          .setJSONB(true)
                          .setOperation("=")
                          .setVal(
                              SelectMetadataSourceVerticleTestHelper.getMetadataCollection3()
                                  .getLabel());
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
                                context.assertTrue(collection.getSelectedBy().contains("DE-15"));
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

  @Test
  public void testNoSelect(TestContext context) {
    Async async = context.async();
    cut.selectAllCollections(
        SelectMetadataSourceVerticleTestHelper.getMetadataSource2().getId(), TENANT_UBL)
        .setHandler(
            aVoid -> {
              if (aVoid.succeeded()) {
                try {
                  Criteria labelCrit =
                      new Criteria()
                          .addField("'label'")
                          .setJSONB(true)
                          .setOperation("=")
                          .setVal(
                              SelectMetadataSourceVerticleTestHelper.getMetadataCollection2()
                                  .getLabel());
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
                                context.assertFalse(collection.getSelectedBy().contains("DE-15"));
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
