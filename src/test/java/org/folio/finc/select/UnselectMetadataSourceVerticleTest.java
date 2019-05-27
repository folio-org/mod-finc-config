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
import org.folio.finc.select.verticles.UnselectMetadataSourceVerticle;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.rest.utils.Constants;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

public class UnselectMetadataSourceVerticleTest extends AbstractSelectMetadataSourceVerticleTest {

  private static final String TENANT_UBL = "ubl";
  private static Vertx vertx;
  private static UnselectMetadataSourceVerticle cut;
  @Rule public Timeout timeout = Timeout.seconds(1000);

  @BeforeClass
  public static void setUp(TestContext context) {
    readData(context);
    vertx = Vertx.vertx();
    try {
      PostgresClient.setIsEmbedded(true);
      PostgresClient instance = PostgresClient.getInstance(vertx);
      instance.startEmbeddedPostgres();
    } catch (Exception e) {
      context.fail(e);
      return;
    }

    Async async = context.async(2);
    int port = NetworkUtils.nextFreePort();

    RestAssured.reset();
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = port;
    RestAssured.defaultParser = Parser.JSON;

    String url = "http://localhost:" + port;
    TenantClient tenantClientFinc =
        new TenantClient(url, Constants.MODULE_TENANT, Constants.MODULE_TENANT);
    TenantClient tenantClientUBL = new TenantClient(url, TENANT_UBL, TENANT_UBL);
    DeploymentOptions options =
        new DeploymentOptions().setConfig(new JsonObject().put("http.port", port)).setWorker(true);

    vertx.deployVerticle(
        RestVerticle.class.getName(),
        options,
        res -> {
          try {
            tenantClientFinc.postTenant(null, postTenantRes -> async.countDown());
            tenantClientUBL.postTenant(
                null,
                postTenantRes -> {
                  Future<Void> future = writeDataToDB(context, vertx);
                  future.setHandler(
                      ar -> {
                        if (ar.succeeded()) async.countDown();
                      });
                });
          } catch (Exception e) {
            context.fail(e);
          }
        });
    cut = new UnselectMetadataSourceVerticle(vertx, vertx.getOrCreateContext());
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

  @Before
  public void before(TestContext context) {
    Async async = context.async();
    vertx = Vertx.vertx();
    JsonObject cfg2 = vertx.getOrCreateContext().config();
    cfg2.put("tenantId", TENANT_UBL);
    cfg2.put("metadataSourceId", metadataSource2.getId());
    cfg2.put("testing", true);
    vertx.deployVerticle(
        cut,
        new DeploymentOptions().setConfig(cfg2).setWorker(true),
        context.asyncAssertSuccess(
            h -> {
              async.complete();
            }));
  }

  @Test
  public void testSuccessfulUnSelect(TestContext context) {
    Async async = context.async();
    cut.selectAllCollections(metadataSource1.getId(), TENANT_UBL)
        .setHandler(
            aVoid -> {
              if (aVoid.succeeded()) {
                try {
                  String where =
                      String.format(
                          " WHERE (jsonb->>'label' = '%s')", metadataCollection1.getLabel());
                  PostgresClient.getInstance(vertx, Constants.MODULE_TENANT)
                      .get(
                          "metadata_collections",
                          FincConfigMetadataCollection.class,
                          where,
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
