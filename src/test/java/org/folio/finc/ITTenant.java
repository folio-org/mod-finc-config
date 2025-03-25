package org.folio.finc;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static io.restassured.RestAssured.given;

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
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.postgres.testing.PostgresTesterContainer;
import org.folio.rest.RestVerticle;
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
public class ITTenant extends ApiTestBase {

  @Rule public Timeout timeout = Timeout.seconds(10);
  @Rule public WireMockRule wireMockRule = new WireMockRule(options().dynamicPort());

  private static final String FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT =
      "/finc-config/metadata-collections";
  private static final String TENANT_ENDPOINT = "/_/tenant";

  @Test
  public void testDeactivateAndPurge() {
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
    TenantAttributes attributes = new TenantAttributes().withModuleTo(null).withPurge(false);
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

    // Purge module with tenant UBL
    TenantAttributes purgeAttributes = new TenantAttributes().withModuleTo(null).withPurge(true);
    given()
        .body(purgeAttributes)
        .header("content-type", ContentType.JSON)
        .header(XOkapiHeaders.TENANT, TENANT_UBL)
        .header(XOkapiHeaders.URL, mockedOkapiUrl)
        .post(TENANT_ENDPOINT)
        .then()
        .statusCode(400);

    // Deactivate module with tenant finc
    given()
        .body(purgeAttributes)
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
        .statusCode(400);
  }
}
