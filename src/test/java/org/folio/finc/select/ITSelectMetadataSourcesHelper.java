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

import org.folio.finc.ApiTestBase;
import org.folio.finc.ITTestSuiteJunit4;
import org.folio.finc.TenantUtil;
import org.folio.finc.TestUtils;
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
public class ITSelectMetadataSourcesHelper extends ApiTestBase {

  private static final Vertx vertx = TestUtils.getVertx();
  private static SelectMetadataSourcesHelper cut;
  @Rule public Timeout timeout = Timeout.seconds(10);

  @BeforeClass
  public static void setUp() throws Exception {
    TestUtils.setupTestSuite();
    cut = new SelectMetadataSourcesHelper(vertx, TENANT_UBL);
  }

  @AfterClass
  public static void teardown() throws Exception {
    TestUtils.teardownTestSuite();
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
