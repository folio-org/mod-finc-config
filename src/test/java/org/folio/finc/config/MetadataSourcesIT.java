package org.folio.finc.config;

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

  private static final String APPLICATION_JSON = "application/json";
  private static final String BASE_URI = "/metadata-sources";
  private static final String TENANT = "diku";

  private static Vertx vertx;
  private static MetadataSource metadataSource1;
  private static MetadataSource metadataSource2;
  private static MetadataSource metadataSource2Changed;

  @Rule public Timeout timeout = Timeout.seconds(10000);

  @BeforeClass
  public static void setUp(TestContext context) {
    vertx = Vertx.vertx();

    try {
      String metadataSourceStr1 =
          new String(Files.readAllBytes(Paths.get("ramls/examples/metadataSourceConfig.sample")));
      metadataSource1 = Json.decodeValue(metadataSourceStr1, MetadataSource.class);
      String metadataSourceStr2 =
          new String(Files.readAllBytes(Paths.get("ramls/examples/metadataSourceConfig2.sample")));
      metadataSource2 = Json.decodeValue(metadataSourceStr2, MetadataSource.class);
      metadataSource2Changed =
          Json.decodeValue(metadataSourceStr2, MetadataSource.class)
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
        .body(Json.encode(metadataSource2))
        .header("X-Okapi-Tenant", TENANT)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .post(BASE_URI)
        .then()
        .statusCode(201)
        .body("id", equalTo(metadataSource2.getId()))
        .body("label", equalTo(metadataSource2.getLabel()))
        .body("description", equalTo(metadataSource2.getDescription()));

    // GET
    given()
        .header("X-Okapi-Tenant", TENANT)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .get(BASE_URI + "/" + metadataSource2.getId())
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("id", equalTo(metadataSource2.getId()))
        .body("label", equalTo(metadataSource2.getLabel()))
        .body("status", equalTo(metadataSource2.getStatus().value()))
        .body("accessUrl", equalTo(metadataSource2.getAccessUrl()));

    // PUT
    given()
        .body(Json.encode(metadataSource2Changed))
        .header("X-Okapi-Tenant", TENANT)
        .header("content-type", APPLICATION_JSON)
        .header("accept", "text/plain")
        .put(BASE_URI + "/" + metadataSource2.getId())
        .then()
        .statusCode(204);

    // GET changed resource
    given()
        .header("X-Okapi-Tenant", TENANT)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .get(BASE_URI + "/" + metadataSource2.getId())
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("id", equalTo(metadataSource2Changed.getId()))
        .body("label", equalTo(metadataSource2Changed.getLabel()))
        .body("status", equalTo(metadataSource2Changed.getStatus().value()))
        .body("accessUrl", equalTo(metadataSource2Changed.getAccessUrl()));

    // DELETE
    given()
        .header("X-Okapi-Tenant", TENANT)
        .header("content-type", APPLICATION_JSON)
        .header("accept", "text/plain")
        .delete(BASE_URI + "/" + metadataSource2.getId())
        .then()
        .statusCode(204);

    // GET again
    given()
        .header("X-Okapi-Tenant", TENANT)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .get(BASE_URI + "/" + metadataSource2.getId())
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
        .body(Json.encode(metadataSource1))
        .header("X-Okapi-Tenant", TENANT)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .post(BASE_URI)
        .then()
        .statusCode(201)
        .body("id", equalTo(metadataSource1.getId()));

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
        .body("metadataSources[0].id", equalTo(metadataSource1.getId()))
        .body("metadataSources[0].label", equalTo(metadataSource1.getLabel()))
        .body("metadataSources[0].status", equalTo(metadataSource1.getStatus().value()));

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
        .body("metadataSources[0].id", equalTo(metadataSource1.getId()))
        .body("metadataSources[0].label", equalTo(metadataSource1.getLabel()))
        .body("metadataSources[0].status", equalTo(metadataSource1.getStatus().value()))
        .body("metadataSources[0].solrShard", equalTo(metadataSource1.getSolrShard().value()));

    // DELETE
    given()
        .header("X-Okapi-Tenant", TENANT)
        .header("content-type", APPLICATION_JSON)
        .header("accept", "text/plain")
        .delete(BASE_URI + "/" + metadataSource1.getId())
        .then()
        .statusCode(204);
  }

  @Test
  public void checkThatInvalidMetadataSourceIsNotPosted() {
    MetadataSource metadataSourceInvalid =
        Json.decodeValue(Json.encode(MetadataSourcesIT.metadataSource2), MetadataSource.class)
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
