package org.folio.finc.select;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.jayway.restassured.http.ContentType;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.folio.finc.ApiTestBase;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection.UsageRestricted;
import org.folio.rest.jaxrs.model.FincSelectFiltersOfCollection;
import org.folio.rest.jaxrs.model.FincSelectMetadataCollection.Permitted;
import org.folio.rest.jaxrs.model.FincSelectMetadataCollection.Selected;
import org.folio.rest.jaxrs.model.Isil;
import org.folio.rest.jaxrs.model.Select;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class SelectMetadataCollectionsIT extends ApiTestBase {

  private final Select unselect = new Select().withSelect(false);
  private final Select select = new Select().withSelect(true);
  @Rule public Timeout timeout = Timeout.seconds(10);
  private FincConfigMetadataCollection metadataCollectionPermitted;
  private FincConfigMetadataCollection metadataCollectionForbidden;
  private Isil isilUBL;
  private Isil isilDiku;

  @Before
  public void init() {
    isilUBL = loadIsilUbl();
    isilDiku = loadIsilDiku();

    metadataCollectionPermitted =
        new FincConfigMetadataCollection()
            .withId(UUID.randomUUID().toString())
            .withLabel("Metadata Collection Permitted")
            .withUsageRestricted(UsageRestricted.NO)
            .withPermittedFor(Arrays.asList(isilUBL.getIsil(), isilDiku.getIsil()))
            .withSelectedBy(Arrays.asList(isilUBL.getIsil()))
            .withSolrMegaCollections(Arrays.asList("21st Century COE Program"))
            .withDescription("This is a test metadata collection permitted");
    metadataCollectionForbidden =
        new FincConfigMetadataCollection()
            .withId(UUID.randomUUID().toString())
            .withLabel("Metadata Collection Forbidden")
            .withUsageRestricted(UsageRestricted.NO)
            .withPermittedFor(Arrays.asList("ISIL-01"))
            .withSelectedBy(Arrays.asList("ISIL-01"))
            .withSolrMegaCollections(Arrays.asList("21st Century COE Program"))
            .withDescription("This is a test metadata collection forbidden");
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

    // GET
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_SELECT_METADATA_COLLECTIONS_ENDPOINT + "?query=(selected=yes AND permitted=yes)")
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("fincSelectMetadataCollections.size()", equalTo(1))
        .body("fincSelectMetadataCollections[0].id", equalTo(metadataCollectionPermitted.getId()))
        .body(
            "fincSelectMetadataCollections[0].label",
            equalTo(metadataCollectionPermitted.getLabel()))
        .body("fincSelectMetadataCollections[0].selected", equalTo(Selected.YES.toString()))
        .body("fincSelectMetadataCollections[0].permitted", equalTo(Permitted.YES.toString()));

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
  public void checkThatWeCanAddAndRemoveFilters() {
    List<String> filterIds = Arrays.asList("uuid-1234", "uuid-5678");
    FincSelectFiltersOfCollection filtersOfCollection =
        new FincSelectFiltersOfCollection().withFilters(filterIds);

    List<String> filterIdsChanged = Arrays.asList("uuid-9876", "uuid-5432");
    FincSelectFiltersOfCollection filtersOfCollectionChanged =
      new FincSelectFiltersOfCollection().withFilters(filterIdsChanged);

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
        .body("filters.size()", equalTo(0));

    // Put filter
    given()
        .body(Json.encode(filtersOfCollection))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.TEXT)
        .put(
            FINC_SELECT_METADATA_COLLECTIONS_ENDPOINT
                + "/"
                + metadataCollectionPermitted.getId()
                + "/filters")
        .then()
        .statusCode(204);

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
      .body("filters.size()", equalTo(2))
      .body("filters[0]", equalTo(filterIds.get(0)))
      .body("filters[1]", equalTo(filterIds.get(1)));

    // GET with different tenant
    given()
      .header("X-Okapi-Tenant", TENANT_DIKU)
      .header("content-type", ContentType.JSON)
      .header("accept", ContentType.JSON)
      .get(FINC_SELECT_METADATA_COLLECTIONS_ENDPOINT + "/" + metadataCollectionPermitted.getId())
      .then()
      .contentType(ContentType.JSON)
      .statusCode(200)
      .body("id", equalTo(metadataCollectionPermitted.getId()))
      .body("label", equalTo(metadataCollectionPermitted.getLabel()))
      .body("filters.size()", equalTo(0));

    // Put changed filter
    given()
      .body(Json.encode(filtersOfCollectionChanged))
      .header("X-Okapi-Tenant", TENANT_UBL)
      .header("content-type", ContentType.JSON)
      .header("accept", ContentType.TEXT)
      .put(
        FINC_SELECT_METADATA_COLLECTIONS_ENDPOINT
          + "/"
          + metadataCollectionPermitted.getId()
          + "/filters")
      .then()
      .statusCode(204);

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
      .body("filters.size()", equalTo(2))
      .body("filters[0]", equalTo(filterIdsChanged.get(0)))
      .body("filters[1]", equalTo(filterIdsChanged.get(1)));

    // DELETE
    given()
      .header("X-Okapi-Tenant", TENANT_UBL)
      .delete(
        FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT + "/" + metadataCollectionPermitted.getId())
      .then()
      .statusCode(204);
  }
}
