package org.folio.finc.config;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.http.ContentType;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.Arrays;
import java.util.UUID;
import org.folio.ApiTestBase;
import org.folio.TestUtils;
import org.folio.rest.jaxrs.model.FincSelectFilter;
import org.folio.rest.jaxrs.model.FincSelectFilter.Type;
import org.folio.rest.jaxrs.model.FincSelectFilterToCollections;
import org.folio.rest.jaxrs.model.Isil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class ConfigFiltersIT extends ApiTestBase {

  @Rule
  public Timeout timeout = Timeout.seconds(10);
  private Isil isilUBL;
  private Isil isilDiku;
  private FincSelectFilter filterUBL;
  private FincSelectFilter filterDIKU;

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
    isilDiku = loadIsilDiku();
    isilUBL = loadIsilUbl();

    filterUBL =
        new FincSelectFilter()
            .withLabel("Filter UBL")
            .withId(UUID.randomUUID().toString())
            .withType(Type.WHITELIST);
    filterDIKU =
        new FincSelectFilter()
            .withLabel("Filter Diku")
            .withId(UUID.randomUUID().toString())
            .withType(Type.BLACKLIST);
  }

  @After
  public void tearDown() {
    deleteIsil(isilDiku.getId());
    deleteIsil(isilUBL.getId());
  }

  @Test
  public void checkThatWeCanAddAndGetFilters() {
    filterUBL.setId(UUID.randomUUID().toString());
    filterDIKU.setId(UUID.randomUUID().toString());

    // POST
    given()
        .body(Json.encode(filterUBL))
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .post(FINC_CONFIG_FILTERS_ENDPOINT)
        .then()
        .statusCode(201)
        .body("id", equalTo(filterUBL.getId()))
        .body("label", equalTo(filterUBL.getLabel()))
        .body("type", equalTo(filterUBL.getType().value()));

    // POST
    given()
        .body(Json.encode(filterDIKU))
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .post(FINC_CONFIG_FILTERS_ENDPOINT)
        .then()
        .statusCode(201)
        .body("id", equalTo(filterDIKU.getId()))
        .body("label", equalTo(filterDIKU.getLabel()))
        .body("type", equalTo(filterDIKU.getType().value()));

    // GET all filters
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_CONFIG_FILTERS_ENDPOINT)
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("fincSelectFilters.size()", equalTo(2));

    // GET filter ubl
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_CONFIG_FILTERS_ENDPOINT + "/" + filterUBL.getId())
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("id", equalTo(filterUBL.getId()))
        .body("label", equalTo(filterUBL.getLabel()))
        .body("type", equalTo(filterUBL.getType().value()));

    // GET filter diku
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_CONFIG_FILTERS_ENDPOINT + "/" + filterDIKU.getId())
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("id", equalTo(filterDIKU.getId()))
        .body("label", equalTo(filterDIKU.getLabel()))
        .body("type", equalTo(filterDIKU.getType().value()));

    // PUT filter diku
    filterDIKU.setLabel("CHANGED");
    given()
        .body(Json.encode(filterDIKU))
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.TEXT)
        .put(FINC_CONFIG_FILTERS_ENDPOINT + "/" + filterDIKU.getId())
        .then()
        .statusCode(204);

    // DELETE filters
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .delete(FINC_CONFIG_FILTERS_ENDPOINT + "/" + filterUBL.getId())
        .then()
        .statusCode(204);

    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .delete(FINC_CONFIG_FILTERS_ENDPOINT + "/" + filterDIKU.getId())
        .then()
        .statusCode(204);
  }

  @Test
  public void checkThatWeCanCreateFiltersToCollectionAssociation() {
    filterDIKU.setId(UUID.randomUUID().toString());
    FincSelectFilterToCollections filterToCollections =
        new FincSelectFilterToCollections()
            .withId(filterDIKU.getId())
            .withCollectionIds(
                Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
    // POST filter
    given()
        .body(Json.encode(filterDIKU))
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .post(FINC_CONFIG_FILTERS_ENDPOINT)
        .then()
        .statusCode(201)
        .body("id", equalTo(filterDIKU.getId()))
        .body("label", equalTo(filterDIKU.getLabel()))
        .body("type", equalTo(filterDIKU.getType().value()));

    // PUT filter_to_collection
    given()
        .body(Json.encode(filterToCollections))
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .put(FINC_CONFIG_FILTERS_ENDPOINT + "/" + filterDIKU.getId() + "/collections")
        .then()
        .statusCode(200)
        .body("collectionIds.size()", equalTo(2))
        .body("collectionIds", equalTo(filterToCollections.getCollectionIds()))
        .body("collectionsCount", equalTo(filterToCollections.getCollectionIds().size()));

    // Get filter_to_collection
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_CONFIG_FILTERS_ENDPOINT + "/" + filterDIKU.getId() + "/collections")
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("collectionIds.size()", equalTo(2))
        .body("collectionIds", equalTo(filterToCollections.getCollectionIds()))
        .body("collectionsCount", equalTo(filterToCollections.getCollectionIds().size()));

    // PUT filter_to_collection in order to update
    filterToCollections.setCollectionIds(
        Arrays.asList(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString()));
    given()
        .body(Json.encode(filterToCollections))
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .put(FINC_CONFIG_FILTERS_ENDPOINT + "/" + filterDIKU.getId() + "/collections")
        .then()
        .statusCode(200)
        .body("collectionIds.size()", equalTo(3))
        .body("collectionIds", equalTo(filterToCollections.getCollectionIds()))
        .body("collectionsCount", equalTo(filterToCollections.getCollectionIds().size()));

    // DELETE filter
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .delete(FINC_CONFIG_FILTERS_ENDPOINT + "/" + filterDIKU.getId())
        .then()
        .statusCode(204);
    }


}
