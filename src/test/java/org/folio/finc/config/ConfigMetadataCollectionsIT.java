package org.folio.finc.config;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.jayway.restassured.http.ContentType;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.Arrays;
import java.util.UUID;
import org.folio.finc.ApiTestBase;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection.MetadataAvailable;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection.UsageRestricted;
import org.folio.rest.jaxrs.model.MdSource;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class ConfigMetadataCollectionsIT extends ApiTestBase {

  @Rule
  public Timeout timeout = Timeout.seconds(10);
  private FincConfigMetadataCollection metadataCollection;
  private FincConfigMetadataCollection metadataCollectionChanged;

  @Before
  public void init() {
    MdSource mdSource = new MdSource().withId(UUID.randomUUID().toString());
    metadataCollection =
        new FincConfigMetadataCollection()
            .withId(UUID.randomUUID().toString())
            .withLabel("Metadata Collection Test")
            .withDescription("This is a test metadata collection")
            .withUsageRestricted(UsageRestricted.NO)
            .withMetadataAvailable(MetadataAvailable.YES)
            .withSolrMegaCollections(Arrays.asList("Solr Mega Collection Test"))
            .withCollectionId("collection-123")
            .withMdSource(mdSource);

    metadataCollectionChanged = metadataCollection.withMetadataAvailable(MetadataAvailable.NO);
  }

  @Test
  public void checkThatWeCanAddGetPutAndDeleteMetadataCollections() {
    // POST
    given()
        .body(Json.encode(metadataCollection))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .post(FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT)
        .then()
        .statusCode(201)
        .body("id", equalTo(metadataCollection.getId()))
        .body("label", equalTo(metadataCollection.getLabel()))
        .body("description", equalTo(metadataCollection.getDescription()));

    // GET
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT + "/" + metadataCollection.getId())
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("id", equalTo(metadataCollection.getId()))
        .body("label", equalTo(metadataCollection.getLabel()))
        .body("mdSource.id", equalTo(metadataCollection.getMdSource().getId()));

    // PUT
    given()
        .body(Json.encode(metadataCollectionChanged))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", "text/plain")
        .put(FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT + "/" + metadataCollection.getId())
        .then()
        .statusCode(204);

    // GET changed resource
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT + "/" + metadataCollection.getId())
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("id", equalTo(metadataCollectionChanged.getId()))
        .body("label", equalTo(metadataCollectionChanged.getLabel()))
        .body("mdSource.id", equalTo(metadataCollectionChanged.getMdSource().getId()));

    // DELETE
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.TEXT)
        .delete(FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT + "/" + metadataCollectionChanged.getId())
        .then()
        .statusCode(204);

    // GET again
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT + "/" + metadataCollectionChanged.getId())
        .then()
        .statusCode(404);

    // GET all
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT)
        .then()
        .statusCode(200)
        .body("totalRecords", equalTo(0));
  }

  @Test
  public void checkThatWeCanSearchByCQL() {
    given()
        .body(Json.encode(metadataCollection))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .post(FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT)
        .then()
        .statusCode(201)
        .body("id", equalTo(metadataCollection.getId()));

    String cql = "?query=(label=\"Metadata Collection*\")";
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT + cql)
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("fincConfigMetadataCollections.size()", equalTo(1))
        .body("fincConfigMetadataCollections[0].id", equalTo(metadataCollection.getId()))
        .body("fincConfigMetadataCollections[0].label", equalTo(metadataCollection.getLabel()))
        .body(
            "fincConfigMetadataCollections[0].mdSource.id",
            equalTo(metadataCollectionChanged.getMdSource().getId()));

    String cql2 = "?query=(label=\"FOO*\")";
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT + cql2)
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("totalRecords", equalTo(0));

    String cqlCollection = "?query=(collectionId==\"collection-123\")";
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT + cqlCollection)
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("fincConfigMetadataCollections.size()", equalTo(1))
        .body("fincConfigMetadataCollections[0].id", equalTo(metadataCollectionChanged.getId()))
        .body(
            "fincConfigMetadataCollections[0].label", equalTo(metadataCollectionChanged.getLabel()))
        .body(
            "fincConfigMetadataCollections[0].mdSource.id",
            equalTo(metadataCollectionChanged.getMdSource().getId()));

    // DELETE
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", "text/plain")
        .delete(FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT + "/" + metadataCollectionChanged.getId())
        .then()
        .statusCode(204);
  }

  @Test
  public void checkThatInvalidMetadataCollectionIsNotPosted() {
    FincConfigMetadataCollection metadataCollectionInvalid =
        Json.decodeValue(Json.encode(metadataCollection), FincConfigMetadataCollection.class)
            .withLabel(null);
    given()
        .body(Json.encode(metadataCollectionInvalid))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .post(FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT)
        .then()
        .statusCode(422);
  }
}
