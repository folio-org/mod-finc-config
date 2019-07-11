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
import java.util.HashMap;
import java.util.Map;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.Select;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.rest.utils.Constants;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class SelectMetadataSourcesHelperTest extends AbstractSelectMetadataSourceVerticleTest {

  private static final String TENANT_UBL = "ubl";
  private static Vertx vertx;
  private static SelectMetadataSourcesHelper cut;
  @Rule
  public Timeout timeout = Timeout.seconds(1000);

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
    cut = new SelectMetadataSourcesHelper(vertx, TENANT_UBL);
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

  @Test
  public void testSuccessfulSelect(TestContext context) {

    Select select = new Select().withSelect(true);

    Map<String, String> header = new HashMap<>();
    header.put("X-Okapi-Tenant", TENANT_UBL);
    header.put("content-type", "application/json");
    header.put("accept", "application/json");

    Async async = context.async();
    cut.selectAllCollectionsOfMetadataSource(metadataSource2.getId(), select, header, ar -> {
      if (ar.succeeded()) {
        async.complete();
      } else {
        context.fail();
      }
    } , vertx.getOrCreateContext());
  }

}
