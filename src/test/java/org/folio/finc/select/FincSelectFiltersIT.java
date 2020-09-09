package org.folio.finc.select;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

import io.restassured.http.ContentType;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.Arrays;
import java.util.UUID;
import org.folio.finc.ApiTestBase;
import org.folio.rest.jaxrs.model.FincSelectFilter;
import org.folio.rest.jaxrs.model.FincSelectFilter.Type;
import org.folio.rest.jaxrs.model.FincSelectFilterToCollections;
import org.folio.rest.jaxrs.model.Isil;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class FincSelectFiltersIT extends ApiTestBase {
  @Rule public Timeout timeout = Timeout.seconds(10);
  private Isil isilUBL;
  private Isil isilDiku;
  private FincSelectFilter filter1;
  private FincSelectFilter filter2;

  @Before
  public void init() {
    isilUBL = loadIsilUbl();
    isilDiku = loadIsilDiku();

    filter1 =
        new FincSelectFilter()
            .withLabel("Holdings 1")
            .withId(UUID.randomUUID().toString())
            .withType(Type.WHITELIST);
    filter2 =
        new FincSelectFilter()
            .withLabel("Holdings 2")
            .withId(UUID.randomUUID().toString())
            .withType(Type.BLACKLIST);
  }

  @After
  public void tearDown() {
    deleteIsil(isilDiku.getId());
    deleteIsil(isilUBL.getId());
  }

  @Test
  public void checkThatWeCanSearchForFilters() {
    filter1.setId(UUID.randomUUID().toString());
    filter2.setId(UUID.randomUUID().toString());

    // POST
    given()
        .body(Json.encode(filter1))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .post(FINC_SELECT_FILTERS_ENDPOINT)
        .then()
        .statusCode(201)
        .body("id", equalTo(filter1.getId()))
        .body("label", equalTo(filter1.getLabel()))
        .body("type", equalTo(filter1.getType().value()));

    // POST
    given()
        .body(Json.encode(filter2))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .post(FINC_SELECT_FILTERS_ENDPOINT)
        .then()
        .statusCode(201)
        .body("id", equalTo(filter2.getId()))
        .body("label", equalTo(filter2.getLabel()))
        .body("type", equalTo(filter2.getType().value()));

    // GET
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_SELECT_FILTERS_ENDPOINT + "?query=(label==Holdings 1)")
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("fincSelectFilters.size()", equalTo(1))
        .body("fincSelectFilters[0].id", equalTo(filter1.getId()))
        .body("fincSelectFilters[0].label", equalTo(filter1.getLabel()))
        .body("fincSelectFilters[0]", not(hasKey("isil")));

    // GET
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_SELECT_FILTERS_ENDPOINT + "?query=(isil==DE-15)")
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("fincSelectFilters.size()", equalTo(0));

    // GET filter not found
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.TEXT)
        .header("accept", ContentType.JSON)
        .get(FINC_SELECT_FILTERS_ENDPOINT + "/" + UUID.randomUUID().toString())
        .then()
        .statusCode(404);

    // PUT Filter
    filter1.setLabel("Holdings 1 - CHANGED");
    given()
      .body(Json.encode(filter1))
      .header("X-Okapi-Tenant", TENANT_UBL)
      .header("content-type", ContentType.JSON)
      .header("accept", ContentType.TEXT)
      .put(FINC_SELECT_FILTERS_ENDPOINT + "/" + filter1.getId())
      .then()
      .statusCode(204);

    // GET
    given()
      .header("X-Okapi-Tenant", TENANT_UBL)
      .header("content-type", ContentType.JSON)
      .header("accept", ContentType.JSON)
      .get(FINC_SELECT_FILTERS_ENDPOINT + "/" + filter1.getId())
      .then()
      .contentType(ContentType.JSON)
      .statusCode(200)
      .body("id", equalTo(filter1.getId()))
      .body("label", equalTo(filter1.getLabel()))
      .body("$", not(hasKey("isil")));

    // DELETE
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .delete(FINC_SELECT_FILTERS_ENDPOINT + "/" + filter1.getId())
        .then()
        .statusCode(204);

    // DELETE
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .delete(FINC_SELECT_FILTERS_ENDPOINT + "/" + filter2.getId())
        .then()
        .statusCode(204);
  }

  @Test
  public void checkThatWeCanCreateFiltersToCollectionAssociation() {
    filter1.setId(UUID.randomUUID().toString());
    FincSelectFilterToCollections fincSelectFilterCollections =
        new FincSelectFilterToCollections()
            .withCollectionIds(
                Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString()));

    // POST filter
    given()
        .body(Json.encode(filter1))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .post(FINC_SELECT_FILTERS_ENDPOINT)
        .then()
        .statusCode(201)
        .body("id", equalTo(filter1.getId()))
        .body("label", equalTo(filter1.getLabel()))
        .body("type", equalTo(filter1.getType().value()));

    // PUT filter_to_collection
    given()
        .body(Json.encode(fincSelectFilterCollections))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .put(FINC_SELECT_FILTERS_ENDPOINT + "/" + filter1.getId() + "/collections")
        .then()
        .statusCode(200)
        .body("collectionIds.size()", equalTo(2))
        .body("collectionIds", equalTo(fincSelectFilterCollections.getCollectionIds()))
        .body("collectionsCount", equalTo(fincSelectFilterCollections.getCollectionIds().size()));

    // Get filter_to_collection
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_SELECT_FILTERS_ENDPOINT + "/" + filter1.getId() + "/collections")
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("collectionIds.size()", equalTo(2))
        .body("collectionIds", equalTo(fincSelectFilterCollections.getCollectionIds()))
        .body("collectionsCount", equalTo(fincSelectFilterCollections.getCollectionIds().size()));

    // PUT filter_to_collection in order to update
    fincSelectFilterCollections.setCollectionIds(
        Arrays.asList(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString()));
    given()
        .body(Json.encode(fincSelectFilterCollections))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .put(FINC_SELECT_FILTERS_ENDPOINT + "/" + filter1.getId() + "/collections")
        .then()
        .statusCode(200)
        .body("collectionIds.size()", equalTo(3))
        .body("collectionIds", equalTo(fincSelectFilterCollections.getCollectionIds()))
        .body("collectionsCount", equalTo(fincSelectFilterCollections.getCollectionIds().size()));

    // DELETE filter
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .delete(FINC_SELECT_FILTERS_ENDPOINT + "/" + filter1.getId())
        .then()
        .statusCode(204);

    // Get filter_to_collection: Check that it is deleted together with the filter
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_SELECT_FILTERS_ENDPOINT + "/" + filter1.getId() + "/collections")
        .then()
        .statusCode(404);
  }

  @Test
  public void checkThatWeCannotCreateFiltersToCollectionAssociationIfFilterNotPresent() {
    FincSelectFilterToCollections fincSelectFilterCollections =
        new FincSelectFilterToCollections()
            .withCollectionIds(
                Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
    // PUT
    given()
        .body(Json.encode(fincSelectFilterCollections))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .put(FINC_SELECT_FILTERS_ENDPOINT + "/" + UUID.randomUUID().toString() + "/collections")
        .then()
        .statusCode(400);
  }
}
