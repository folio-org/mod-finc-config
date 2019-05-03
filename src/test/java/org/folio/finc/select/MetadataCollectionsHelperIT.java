package org.folio.finc.select;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

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
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.Isil;
import org.folio.rest.jaxrs.model.MetadataCollection;
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
public class MetadataCollectionsHelperIT {

  private static final String APPLICATION_JSON = "application/json";
  private static final String BASE_URI = "/finc-select/metadata-collections";
  private static final String TENANT_UBL = "ubl";
  private static final String TENANT_DIKU = "ubl";
  private static MetadataCollection metadataCollectionPermitted;
  private static MetadataCollection metadataCollectionForbidden;
  private static Isil isil1;
  private static Vertx vertx;
  @Rule public Timeout timeout = Timeout.seconds(10);
  private final Select unselect = new Select().withSelect(false);
  private final Select select = new Select().withSelect(true);

  @BeforeClass
  public static void setUp(TestContext context) {
    try {
      String isilStr = new String(Files.readAllBytes(Paths.get("ramls/examples/isil1.sample")));
      isil1 = Json.decodeValue(isilStr, Isil.class);

      String metadataCollectionStr =
          new String(
              Files.readAllBytes(Paths.get("ramls/examples/metadataCollectionConfig.sample")));
      metadataCollectionPermitted =
          Json.decodeValue(metadataCollectionStr, MetadataCollection.class);

      String metadataCollectionStr2 =
          new String(
              Files.readAllBytes(Paths.get("ramls/examples/metadataCollectionConfig2.sample")));
      metadataCollectionForbidden =
          Json.decodeValue(metadataCollectionStr2, MetadataCollection.class);
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
    // POST
    given()
        .body(Json.encode(metadataCollectionPermitted))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .post("/metadata-collections")
        .then()
        .statusCode(201)
        .body("id", equalTo(metadataCollectionPermitted.getId()))
        .body("label", equalTo(metadataCollectionPermitted.getLabel()))
        .body("description", equalTo(metadataCollectionPermitted.getDescription()));

    // POST
    given()
        .body(Json.encode(metadataCollectionForbidden))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .post("/metadata-collections")
        .then()
        .statusCode(201)
        .body("id", equalTo(metadataCollectionForbidden.getId()))
        .body("label", equalTo(metadataCollectionForbidden.getLabel()))
        .body("description", equalTo(metadataCollectionForbidden.getDescription()));

    // POST isil
    given()
        .body(Json.encode(isil1))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .post("/isils")
        .then()
        .statusCode(201)
        .body("isil", equalTo(isil1.getIsil()));

    // GET
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .get(BASE_URI + "/" + metadataCollectionPermitted.getId())
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("id", equalTo(metadataCollectionPermitted.getId()))
        .body("label", equalTo(metadataCollectionPermitted.getLabel()))
        .body("selected", equalTo(true))
        .body("permitted", equalTo(true));

    // GET
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .get(BASE_URI + "/" + metadataCollectionForbidden.getId())
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("id", equalTo(metadataCollectionForbidden.getId()))
        .body("label", equalTo(metadataCollectionForbidden.getLabel()))
        .body("selected", equalTo(false))
        .body("permitted", equalTo(false));

    // Unselect metadata collection
    given()
        .body(Json.encode(unselect))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", APPLICATION_JSON)
        .header("accept", "text/plain")
        .put(BASE_URI + "/" + metadataCollectionPermitted.getId() + "/select")
        .then()
        .statusCode(204);

    // Check if unselect was successful
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .get(BASE_URI + "/" + metadataCollectionPermitted.getId())
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("id", equalTo(metadataCollectionPermitted.getId()))
        .body("label", equalTo(metadataCollectionPermitted.getLabel()))
        .body("selected", equalTo(false))
        .body("permitted", equalTo(true));

    // Select metadata collection again
    given()
        .body(Json.encode(select))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", APPLICATION_JSON)
        .header("accept", "text/plain")
        .put(BASE_URI + "/" + metadataCollectionPermitted.getId() + "/select")
        .then()
        .statusCode(204);

    // Check if unselect was successful
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .get(BASE_URI + "/" + metadataCollectionPermitted.getId())
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("id", equalTo(metadataCollectionPermitted.getId()))
        .body("label", equalTo(metadataCollectionPermitted.getLabel()))
        .body("selected", equalTo(true))
        .body("permitted", equalTo(true));

    // Check that we cannot select forbidden metadata collection
    given()
        .body(Json.encode(select))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", APPLICATION_JSON)
        .header("accept", "text/plain")
        .put(BASE_URI + "/" + metadataCollectionForbidden.getId() + "/select")
        .then()
        .statusCode(404);
  }
}
