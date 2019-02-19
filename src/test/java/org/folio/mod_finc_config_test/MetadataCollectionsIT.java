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
import org.folio.rest.jaxrs.model.MetadataCollection;
import org.folio.rest.jaxrs.model.MetadataCollection.MetadataAvailable;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.rest.utils.Constants;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class MetadataCollectionsIT {

  private static final String APPLICATION_JSON = "application/json";
  private static final String BASE_URI = "/metadata-collections";
  private static final String TENANT = "diku";

  private static Vertx vertx;
  private static MetadataCollection metadataCollection;
  private static MetadataCollection metadataCollectionChanged;

  @Rule public Timeout timeout = Timeout.seconds(10);

  @BeforeClass
  public static void setUp(TestContext context) {
    vertx = Vertx.vertx();

    try {
      String metadataCollectionStr =
          new String(Files.readAllBytes(Paths.get("ramls/examples/metadataCollection.sample")));
      metadataCollection = Json.decodeValue(metadataCollectionStr, MetadataCollection.class);
      metadataCollectionChanged =
          Json.decodeValue(metadataCollectionStr, MetadataCollection.class)
              .withMetadataAvailable(MetadataAvailable.NO);
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
            tenantClient.postTenant(null, postTenantRes -> async.complete());
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
  public void checkThatWeCanAddGetPutAndDeleteMetadataCollections() {
    // POST
    given()
        .body(Json.encode(metadataCollection))
        .header("X-Okapi-Tenant", TENANT)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .post(BASE_URI)
        .then()
        .statusCode(201)
        .body("id", equalTo(metadataCollection.getId()))
        .body("label", equalTo(metadataCollection.getLabel()))
        .body("description", equalTo(metadataCollection.getDescription()));

    // GET
    given()
        .header("X-Okapi-Tenant", TENANT)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .get(BASE_URI + "/" + metadataCollection.getId())
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("id", equalTo(metadataCollection.getId()))
        .body("label", equalTo(metadataCollection.getLabel()))
        .body("mdSource.id", equalTo(metadataCollection.getMdSource().getId()))
        .body("facetLabel", equalTo(metadataCollection.getFacetLabel()));

    // PUT
    given()
        .body(Json.encode(metadataCollectionChanged))
        .header("X-Okapi-Tenant", TENANT)
        .header("content-type", APPLICATION_JSON)
        .header("accept", "text/plain")
        .put(BASE_URI + "/" + metadataCollection.getId())
        .then()
        .statusCode(204);

    // GET changed resource
    given()
        .header("X-Okapi-Tenant", TENANT)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .get(BASE_URI + "/" + metadataCollection.getId())
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("id", equalTo(metadataCollectionChanged.getId()))
        .body("label", equalTo(metadataCollectionChanged.getLabel()))
        .body("mdSource.id", equalTo(metadataCollectionChanged.getMdSource().getId()))
        .body("facetLabel", equalTo(metadataCollectionChanged.getFacetLabel()));

    // DELETE
    given()
        .header("X-Okapi-Tenant", TENANT)
        .header("content-type", APPLICATION_JSON)
        .header("accept", "text/plain")
        .delete(BASE_URI + "/" + metadataCollectionChanged.getId())
        .then()
        .statusCode(204);

    // GET again
    given()
        .header("X-Okapi-Tenant", TENANT)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .get(BASE_URI + "/" + metadataCollectionChanged.getId())
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
        .body(Json.encode(metadataCollection))
        .header("X-Okapi-Tenant", TENANT)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .post(BASE_URI)
        .then()
        .statusCode(201)
        .body("id", equalTo(metadataCollection.getId()));

    String cql = "?query=(label=\"21st Century*\")";
    given()
        .header("X-Okapi-Tenant", TENANT)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .get(BASE_URI + cql)
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("metadataCollections.size()", equalTo(1))
        .body("metadataCollections[0].id", equalTo(metadataCollection.getId()))
        .body("metadataCollections[0].label", equalTo(metadataCollection.getLabel()))
        .body(
            "metadataCollections[0].mdSource.id",
            equalTo(metadataCollectionChanged.getMdSource().getId()))
        .body(
            "metadataCollections[0].facetLabel",
            equalTo(metadataCollectionChanged.getFacetLabel()));

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

    String cqlSolrShard = "?query=(collectionId==\"coe-123\")";
    given()
        .header("X-Okapi-Tenant", TENANT)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .get(BASE_URI + cqlSolrShard)
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("metadataCollections.size()", equalTo(1))
        .body("metadataCollections[0].id", equalTo(metadataCollectionChanged.getId()))
        .body("metadataCollections[0].label", equalTo(metadataCollectionChanged.getLabel()))
        .body(
            "metadataCollections[0].mdSource.id",
            equalTo(metadataCollectionChanged.getMdSource().getId()))
        .body(
            "metadataCollections[0].facetLabel",
            equalTo(metadataCollectionChanged.getFacetLabel()));

    // DELETE
    given()
        .header("X-Okapi-Tenant", TENANT)
        .header("content-type", APPLICATION_JSON)
        .header("accept", "text/plain")
        .delete(BASE_URI + "/" + metadataCollectionChanged.getId())
        .then()
        .statusCode(204);
  }

  @Test
  public void checkThatInvalidMetadataCollectionIsNotPosted() {
    MetadataCollection metadataCollectionInvalid =
        Json.decodeValue(
                Json.encode(MetadataCollectionsIT.metadataCollection), MetadataCollection.class)
            .withLabel(null);
    given()
        .body(Json.encode(metadataCollectionInvalid))
        .header("X-Okapi-Tenant", TENANT)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .post(BASE_URI)
        .then()
        .statusCode(422);
  }
}
