package org.folio.finc.select;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import java.util.HashMap;
import java.util.Map;

import org.folio.ApiTestBase;
import org.folio.TenantUtil;
import org.folio.TestUtils;
import org.folio.rest.jaxrs.model.Select;
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
