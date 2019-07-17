package org.folio.finc.select;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.parsing.Parser;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.folio.finc.mocks.MockOrganization;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.FincConfigMetadataSource;
import org.folio.rest.jaxrs.model.FincSelectMetadataSource.Selected;
import org.folio.rest.jaxrs.model.Isil;
import org.folio.rest.jaxrs.model.Organization;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.rest.utils.Constants;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class SelectMetadataSourcesIT {

  private static final String APPLICATION_JSON = "application/json";
  private static final String BASE_URI = "/finc-select/metadata-sources";
  private static final String TENANT_UBL = "ubl";
  private static final String TENANT_DIKU = "ubl";
  private static FincConfigMetadataSource metadatasourceSelected;
  private static Organization organizationUUID1234;
  private static Isil isil1;
  private static Vertx vertx;
  @Rule public Timeout timeout = Timeout.seconds(10);
  @Rule public WireMockRule wireMockRule = new WireMockRule(options().dynamicPort());

  @BeforeClass
  public static void setUp(TestContext context) {
    try {
      String isilStr = new String(Files.readAllBytes(Paths.get("ramls/examples/isil1.sample")));
      isil1 = Json.decodeValue(isilStr, Isil.class);

      String metadataSourcesStr =
          new String(
              Files.readAllBytes(Paths.get("ramls/examples/fincConfigMetadataSource.sample")));
      metadatasourceSelected = Json.decodeValue(metadataSourcesStr, FincConfigMetadataSource.class);

      organizationUUID1234 = new Organization();
      organizationUUID1234.setName("Organization Name 1234");
      organizationUUID1234.setId("uuid-1234");
    } catch (Exception e) {
      context.fail(e);
    }
    vertx = Vertx.vertx();
    try {
      PostgresClient.setIsEmbedded(true);
      PostgresClient instance = PostgresClient.getInstance(vertx);
      instance.startEmbeddedPostgres();
    } catch (Exception e) {
      context.fail(e);
      return;
    }

    Async async = context.async();
    int port = NetworkUtils.nextFreePort();

    RestAssured.reset();
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = port;
    RestAssured.defaultParser = Parser.JSON;

    String url = "http://localhost:" + port;
    TenantClient tenantClientFinc =
        new TenantClient(url, Constants.MODULE_TENANT, Constants.MODULE_TENANT);
    TenantClient tenantClientDiku = new TenantClient(url, TENANT_DIKU, TENANT_DIKU);
    DeploymentOptions options =
        new DeploymentOptions().setConfig(new JsonObject().put("http.port", port)).setWorker(true);

    vertx.deployVerticle(
        RestVerticle.class.getName(),
        options,
        res -> {
          try {
            tenantClientFinc.postTenant(null, postTenantRes -> async.complete());
            tenantClientDiku.postTenant(null, postTenantRes -> async.complete());
          } catch (Exception e) {
            context.fail(e);
          }
        });
  }

  @AfterClass
  public static void tearDown(TestContext context) {
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
  public void checkThatWeCanSelectAndUnselect() {
    String mockedOkapiUrl = "http://localhost:" + wireMockRule.port();
    MockOrganization.mockOrganizationFound(organizationUUID1234);

    // POST
    given()
        .body(Json.encode(metadatasourceSelected))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .post("/finc-config/metadata-sources")
        .then()
        .statusCode(201)
        .body("id", equalTo(metadatasourceSelected.getId()))
        .body("label", equalTo(metadatasourceSelected.getLabel()))
        .body("description", equalTo(metadatasourceSelected.getDescription()));

    // POST isil
    given()
        .body(Json.encode(isil1))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .post("/finc-config/isils")
        .then()
        .statusCode(201)
        .body("isil", equalTo(isil1.getIsil()));

    // Get all metadata sources
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .get(BASE_URI)
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("fincSelectMetadataSources.size()", equalTo(1))
        .body("fincSelectMetadataSources[0].id", equalTo(metadatasourceSelected.getId()))
        .body("fincSelectMetadataSources[0].label", equalTo(metadatasourceSelected.getLabel()))
        .body("fincSelectMetadataSources[0].selected", equalTo(Selected.YES.toString()));
  }
}
