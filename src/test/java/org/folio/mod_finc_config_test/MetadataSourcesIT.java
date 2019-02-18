package org.folio.mod_finc_config_test;

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
import org.folio.rest.jaxrs.model.MetadataSource;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.rest.utils.Constants;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class MetadataSourcesIT {

  public static final String APPLICATION_JSON = "application/json";
  public static final String BASE_URI = "/metadata-sources";
  private static final String TENANT = "diku";

  private static Vertx vertx;
  private static MetadataSource metadataSource;
  private static MetadataSource metadataSourceChanged;

  @Rule public Timeout timeout = Timeout.seconds(10);

  @BeforeClass
  public static void setUp(TestContext context) {
    vertx = Vertx.vertx();

    try {
      String metadataSourceStr =
          new String(Files.readAllBytes(Paths.get("ramls/examples/metadataSource.sample")));
      metadataSource = Json.decodeValue(metadataSourceStr, MetadataSource.class);
      metadataSourceChanged =
          Json.decodeValue(metadataSourceStr, MetadataSource.class)
              .withAccessUrl("www.changed.org");
    } catch (Exception e) {
      context.fail(e);
    }

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
    TenantClient tenantClient =
        new TenantClient(url, Constants.MODULE_TENANT, Constants.MODULE_TENANT);
    DeploymentOptions options =
        new DeploymentOptions().setConfig(new JsonObject().put("http.port", port)).setWorker(true);

    vertx.deployVerticle(
        RestVerticle.class.getName(),
        options,
        res -> {
          try {
            tenantClient.postTenant(
                null,
                postTenantRes -> {
                  async.complete();
                });
          } catch (Exception e) {
            context.fail(e);
          }
        });
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
  public void checkThatWeCanAddGetPutAndDeleteMetadataSources() {
    // POST
    given()
        .body(Json.encode(metadataSource))
        .header("X-Okapi-Tenant", TENANT)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .post(BASE_URI)
        .then()
        .statusCode(201)
        .body("id", equalTo(metadataSource.getId()))
        .body("label", equalTo(metadataSource.getLabel()))
        .body("description", equalTo(metadataSource.getDescription()));

    // GET
    given()
        .header("X-Okapi-Tenant", TENANT)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .get(BASE_URI + "/" + metadataSource.getId())
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("id", equalTo(metadataSource.getId()))
        .body("label", equalTo(metadataSource.getLabel()))
        .body("status", equalTo(metadataSource.getStatus().value()))
        .body("accessUrl", equalTo(metadataSource.getAccessUrl()));

    // PUT
    given()
        .body(Json.encode(metadataSourceChanged))
        .header("X-Okapi-Tenant", TENANT)
        .header("content-type", APPLICATION_JSON)
        .header("accept", "text/plain")
        .put(BASE_URI + "/" + metadataSource.getId())
        .then()
        .statusCode(204);

    // GET changed resource
    given()
        .header("X-Okapi-Tenant", TENANT)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .get(BASE_URI + "/" + metadataSource.getId())
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("id", equalTo(metadataSourceChanged.getId()))
        .body("label", equalTo(metadataSourceChanged.getLabel()))
        .body("status", equalTo(metadataSourceChanged.getStatus().value()))
        .body("accessUrl", equalTo(metadataSourceChanged.getAccessUrl()));

    // DELETE
    given()
        .header("X-Okapi-Tenant", TENANT)
        .header("content-type", APPLICATION_JSON)
        .header("accept", "text/plain")
        .delete(BASE_URI + "/" + metadataSource.getId())
        .then()
        .statusCode(204);

    // GET again
    given()
        .header("X-Okapi-Tenant", TENANT)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .get(BASE_URI + "/" + metadataSource.getId())
        .then()
        .statusCode(404);

    // GET all
    given()
        .header("X-Okapi-Tenant", TENANT)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .get(BASE_URI)
        .then()
        .statusCode(200)
        .body("totalRecords", equalTo(0));
  }

  @Test
  public void checkThatWeCanSearchByCQL() {
    given()
        .body(Json.encode(metadataSource))
        .header("X-Okapi-Tenant", TENANT)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .post(BASE_URI)
        .then()
        .statusCode(201)
        .body("id", equalTo(metadataSource.getId()));

    String cql = "?query=(label=\"Cambridge*\")";
    given()
        .header("X-Okapi-Tenant", TENANT)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .get(BASE_URI + cql)
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("metadataSources.size()", equalTo(1))
        .body("metadataSources[0].id", equalTo(metadataSource.getId()))
        .body("metadataSources[0].label", equalTo(metadataSource.getLabel()))
        .body("metadataSources[0].status", equalTo(metadataSource.getStatus().value()));

    String cql2 = "?query=(label=\"FOO*\")";
    given()
        .header("X-Okapi-Tenant", TENANT)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .get(BASE_URI + cql2)
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("totalRecords", equalTo(0));

    String cqlSolrShard = "?query=(solrShard==\"UBL main\")";
    given()
        .header("X-Okapi-Tenant", TENANT)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .get(BASE_URI + cqlSolrShard)
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("metadataSources.size()", equalTo(1))
        .body("metadataSources[0].id", equalTo(metadataSource.getId()))
        .body("metadataSources[0].label", equalTo(metadataSource.getLabel()))
        .body("metadataSources[0].status", equalTo(metadataSource.getStatus().value()))
        .body("metadataSources[0].solrShard", equalTo(metadataSource.getSolrShard().value()));

    // DELETE
    given()
        .header("X-Okapi-Tenant", TENANT)
        .header("content-type", APPLICATION_JSON)
        .header("accept", "text/plain")
        .delete(BASE_URI + "/" + metadataSource.getId())
        .then()
        .statusCode(204);
  }

  @Test
  public void checkThatInvalidMetadataSourceIsNotPosted() {
    MetadataSource metadataSourceInvalid =
        Json.decodeValue(Json.encode(MetadataSourcesIT.metadataSource), MetadataSource.class)
            .withLabel(null);
    given()
        .body(Json.encode(metadataSourceInvalid))
        .header("X-Okapi-Tenant", TENANT)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .post(BASE_URI)
        .then()
        .statusCode(422);
  }
}
