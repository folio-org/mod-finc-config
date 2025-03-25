package org.folio.finc.select;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.http.ContentType;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.Arrays;
import java.util.UUID;
import org.folio.finc.ApiTestBase;
import org.folio.finc.TestUtils;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection.UsageRestricted;
import org.folio.rest.jaxrs.model.FincSelectMetadataCollection.Permitted;
import org.folio.rest.jaxrs.model.FincSelectMetadataCollection.Selected;
import org.folio.rest.jaxrs.model.Isil;
import org.folio.rest.jaxrs.model.Select;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class SelectMetadataCollectionsIT extends ApiTestBase {

  private final Select unselect = new Select().withSelect(false);
  private final Select select = new Select().withSelect(true);
  @Rule
  public Timeout timeout = Timeout.seconds(10);
  private FincConfigMetadataCollection metadataCollectionPermitted;
  private FincConfigMetadataCollection metadataCollectionPermittedNotSelected;
  private FincConfigMetadataCollection metadataCollectionForbidden;
  private FincConfigMetadataCollection metadataCollectionNotRestricted;
  private Isil isilUBL;
  private Isil isilDiku;

  @BeforeClass
  public static void beforeClass() throws Exception {
    TestUtils.setupTenants();
  }

  @AfterClass
  public static void afterClass() throws Exception {
    TestUtils.teardownTenants();
  }

  @Before
  public void init() {
    isilUBL = loadIsilUbl();
    isilDiku = loadIsilDiku();

    metadataCollectionPermitted =
        new FincConfigMetadataCollection()
            .withId(UUID.randomUUID().toString())
            .withLabel("Metadata Collection Permitted")
            .withCollectionId("col1")
            .withUsageRestricted(UsageRestricted.YES)
            .withPermittedFor(Arrays.asList(isilUBL.getIsil(), isilDiku.getIsil()))
            .withSelectedBy(Arrays.asList(isilUBL.getIsil()))
            .withSolrMegaCollections(Arrays.asList("21st Century COE Program"))
            .withDescription("This is a test metadata collection permitted");
    metadataCollectionPermittedNotSelected =
        new FincConfigMetadataCollection()
            .withId(UUID.randomUUID().toString())
            .withLabel("Metadata Collection Permitted")
            .withCollectionId("col1")
            .withUsageRestricted(UsageRestricted.YES)
            .withPermittedFor(Arrays.asList(isilUBL.getIsil(), isilDiku.getIsil()))
            .withSelectedBy(Arrays.asList(isilUBL.getIsil() + "-Foo"))
            .withSolrMegaCollections(Arrays.asList("21st Century COE Program"))
            .withDescription("This is a test metadata collection permitted");
    metadataCollectionForbidden =
        new FincConfigMetadataCollection()
            .withId(UUID.randomUUID().toString())
            .withLabel("Metadata Collection Forbidden")
            .withCollectionId("col2")
            .withUsageRestricted(UsageRestricted.YES)
            .withPermittedFor(Arrays.asList("ISIL-01"))
            .withSelectedBy(Arrays.asList("ISIL-01"))
            .withSolrMegaCollections(Arrays.asList("21st Century COE Program"))
            .withDescription("This is a test metadata collection forbidden");
    metadataCollectionNotRestricted =
        new FincConfigMetadataCollection()
            .withId(UUID.randomUUID().toString())
            .withLabel("Metadata Collection Not Restricted")
            .withCollectionId("col3")
            .withUsageRestricted(UsageRestricted.NO)
            .withSelectedBy(Arrays.asList(isilUBL.getIsil()))
            .withSolrMegaCollections(Arrays.asList("21st Century COE Program"))
            .withDescription("This is a test metadata collection which usage is not restricted");
  }

  @After
  public void tearDown() {
    deleteIsil(isilUBL.getId());
    deleteIsil(isilDiku.getId());
  }

  @Test
  public void checkThatWeCanQueryForPermittedAndSelected() {
    // POST
    given()
        .body(Json.encode(metadataCollectionPermitted))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .post(FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT)
        .then()
        .statusCode(201)
        .body("id", equalTo(metadataCollectionPermitted.getId()))
        .body("label", equalTo(metadataCollectionPermitted.getLabel()))
        .body("description", equalTo(metadataCollectionPermitted.getDescription()));

    // POST
    given()
        .body(Json.encode(metadataCollectionForbidden))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .post(FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT)
        .then()
        .statusCode(201)
        .body("id", equalTo(metadataCollectionForbidden.getId()))
        .body("label", equalTo(metadataCollectionForbidden.getLabel()))
        .body("description", equalTo(metadataCollectionForbidden.getDescription()));

    // POST
    given()
        .body(Json.encode(metadataCollectionNotRestricted))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .post(FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT)
        .then()
        .statusCode(201)
        .body("id", equalTo(metadataCollectionNotRestricted.getId()))
        .body("label", equalTo(metadataCollectionNotRestricted.getLabel()))
        .body("description", equalTo(metadataCollectionNotRestricted.getDescription()));

    // GET
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_SELECT_METADATA_COLLECTIONS_ENDPOINT + "?query=(selected=yes AND permitted=yes)")
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("fincSelectMetadataCollections.size()", equalTo(2));

    // GET
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_SELECT_METADATA_COLLECTIONS_ENDPOINT + "/" + metadataCollectionPermitted.getId())
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("id", equalTo(metadataCollectionPermitted.getId()))
        .body("label", equalTo(metadataCollectionPermitted.getLabel()))
        .body("selected", equalTo(Selected.YES.toString()))
        .body("permitted", equalTo(Permitted.YES.toString()));

    // GET
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(
            FINC_SELECT_METADATA_COLLECTIONS_ENDPOINT
                + "/"
                + metadataCollectionNotRestricted.getId())
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("id", equalTo(metadataCollectionNotRestricted.getId()))
        .body("label", equalTo(metadataCollectionNotRestricted.getLabel()))
        .body("selected", equalTo(Selected.YES.toString()))
        .body("permitted", equalTo(Permitted.YES.toString()));

    // DELETE
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .delete(
            FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT + "/" + metadataCollectionPermitted.getId())
        .then()
        .statusCode(204);

    // DELETE
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .delete(
            FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT + "/" + metadataCollectionForbidden.getId())
        .then()
        .statusCode(204);

    // DELETE
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .delete(
            FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT
                + "/"
                + metadataCollectionNotRestricted.getId())
        .then()
        .statusCode(204);
  }

  @Test
  public void checkThatNotSelectedIsNotReturned() {
    // POST
    given()
        .body(Json.encode(metadataCollectionPermittedNotSelected))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .post(FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT)
        .then()
        .statusCode(201)
        .body("id", equalTo(metadataCollectionPermittedNotSelected.getId()))
        .body("label", equalTo(metadataCollectionPermittedNotSelected.getLabel()))
        .body("description", equalTo(metadataCollectionPermittedNotSelected.getDescription()));

    // GET
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_SELECT_METADATA_COLLECTIONS_ENDPOINT + "?query=(selected=yes AND permitted=yes)")
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("fincSelectMetadataCollections.size()", equalTo(0));

    // DELETE
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .delete(
            FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT + "/" + metadataCollectionPermittedNotSelected
                .getId())
        .then()
        .statusCode(204);
  }

  @Test
  public void checkThatWeCanSelectAndUnselect() {
    // POST
    given()
        .body(Json.encode(metadataCollectionPermitted))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .post(FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT)
        .then()
        .statusCode(201)
        .body("id", equalTo(metadataCollectionPermitted.getId()))
        .body("label", equalTo(metadataCollectionPermitted.getLabel()))
        .body("description", equalTo(metadataCollectionPermitted.getDescription()));

    // POST
    given()
        .body(Json.encode(metadataCollectionForbidden))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .post(FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT)
        .then()
        .statusCode(201)
        .body("id", equalTo(metadataCollectionForbidden.getId()))
        .body("label", equalTo(metadataCollectionForbidden.getLabel()))
        .body("description", equalTo(metadataCollectionForbidden.getDescription()));

    // GET
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_SELECT_METADATA_COLLECTIONS_ENDPOINT + "/" + metadataCollectionPermitted.getId())
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("id", equalTo(metadataCollectionPermitted.getId()))
        .body("label", equalTo(metadataCollectionPermitted.getLabel()))
        .body("selected", equalTo(Selected.YES.toString()))
        .body("permitted", equalTo(Permitted.YES.toString()));

    // GET
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_SELECT_METADATA_COLLECTIONS_ENDPOINT + "/" + metadataCollectionForbidden.getId())
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("id", equalTo(metadataCollectionForbidden.getId()))
        .body("label", equalTo(metadataCollectionForbidden.getLabel()))
        .body("selected", equalTo(Selected.NO.toString()))
        .body("permitted", equalTo(Permitted.NO.toString()));

    // Unselect metadata collection
    given()
        .body(Json.encode(unselect))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.TEXT)
        .put(
            FINC_SELECT_METADATA_COLLECTIONS_ENDPOINT
                + "/"
                + metadataCollectionPermitted.getId()
                + "/select")
        .then()
        .statusCode(204);

    // Check if unselect was successful
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_SELECT_METADATA_COLLECTIONS_ENDPOINT + "/" + metadataCollectionPermitted.getId())
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("id", equalTo(metadataCollectionPermitted.getId()))
        .body("label", equalTo(metadataCollectionPermitted.getLabel()))
        .body("selected", equalTo(Selected.NO.toString()))
        .body("permitted", equalTo(Permitted.YES.toString()));

    // Select metadata collection again
    given()
        .body(Json.encode(select))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.TEXT)
        .put(
            FINC_SELECT_METADATA_COLLECTIONS_ENDPOINT
                + "/"
                + metadataCollectionPermitted.getId()
                + "/select")
        .then()
        .statusCode(204);

    // Check if unselect was successful
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_SELECT_METADATA_COLLECTIONS_ENDPOINT + "/" + metadataCollectionPermitted.getId())
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("id", equalTo(metadataCollectionPermitted.getId()))
        .body("label", equalTo(metadataCollectionPermitted.getLabel()))
        .body("selected", equalTo(Selected.YES.toString()))
        .body("permitted", equalTo(Permitted.YES.toString()));

    // Check that we cannot select forbidden metadata collection
    given()
        .body(Json.encode(select))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.TEXT)
        .put(
            FINC_SELECT_METADATA_COLLECTIONS_ENDPOINT
                + "/"
                + metadataCollectionForbidden.getId()
                + "/select")
        .then()
        .statusCode(404);

    // DELETE
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .delete(
            FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT + "/" + metadataCollectionPermitted.getId())
        .then()
        .statusCode(204);

    // DELETE
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .delete(
            FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT + "/" + metadataCollectionForbidden.getId())
        .then()
        .statusCode(204);
  }

  @Test
  public void checkThatWeCanFilterForMetadataCollections() {
    // POST
    given()
        .body(Json.encode(metadataCollectionPermitted))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .post(FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT)
        .then()
        .statusCode(201)
        .body("id", equalTo(metadataCollectionPermitted.getId()))
        .body("label", equalTo(metadataCollectionPermitted.getLabel()))
        .body("description", equalTo(metadataCollectionPermitted.getDescription()));

    // POST
    given()
        .body(Json.encode(metadataCollectionForbidden))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .post(FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT)
        .then()
        .statusCode(201)
        .body("id", equalTo(metadataCollectionForbidden.getId()))
        .body("label", equalTo(metadataCollectionForbidden.getLabel()))
        .body("description", equalTo(metadataCollectionForbidden.getDescription()));

    // GET
    String queryUBL =
        "query=(selectedBy adj \""
            + isilUBL.getIsil()
            + "\") AND (permittedFor adj \""
            + isilUBL.getIsil()
            + "\")";
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_SELECT_METADATA_COLLECTIONS_ENDPOINT + "?" + queryUBL)
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("fincSelectMetadataCollections.size()", equalTo(1))
        .body("fincSelectMetadataCollections[0].id", equalTo(metadataCollectionPermitted.getId()));

    // GET
    String queryDiku =
        "query=selectedBy adj "
            + isilDiku.getIsil()
            + " AND permittedFor adj "
            + isilDiku.getIsil();

    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_SELECT_METADATA_COLLECTIONS_ENDPOINT + "?" + queryDiku)
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("fincSelectMetadataCollections.size()", equalTo(0));

    // DELETE
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .delete(
            FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT + "/" + metadataCollectionPermitted.getId())
        .then()
        .statusCode(204);

    // DELETE
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .delete(
            FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT + "/" + metadataCollectionForbidden.getId())
        .then()
        .statusCode(204);
  }
}
