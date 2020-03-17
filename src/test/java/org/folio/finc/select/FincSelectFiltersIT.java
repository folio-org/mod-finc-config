package org.folio.finc.select;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.Arrays;
import java.util.UUID;
import org.folio.finc.ApiTestBase;
import org.folio.rest.jaxrs.model.FilterFile;
import org.folio.rest.jaxrs.model.FincSelectFilter;
import org.folio.rest.jaxrs.model.FincSelectFilter.Type;
import org.folio.rest.jaxrs.model.FincSelectFilterToCollections;
import org.folio.rest.jaxrs.model.Isil;
import org.junit.After;
import org.junit.Assert;
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
  private UUID collectionId1;
  private UUID collectionId2;

  @Before
  public void init() {
    isilUBL = loadIsilUbl();
    isilDiku = loadIsilDiku();
    collectionId1 = UUID.randomUUID();
    collectionId2 = UUID.randomUUID();

    filter1 =
        new FincSelectFilter()
            .withLabel("Holdings 1")
            .withId(UUID.randomUUID().toString())
            .withType(Type.WHITELIST)
            .withCollectionIds(Arrays.asList(collectionId1.toString(), collectionId2.toString()));
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
  public void checkThatWeCanAddGetPutAndDeleteFilters() {

    // POST File 1
    Response firstPostFileResp =
        given()
            .body("foobar".getBytes())
            .header("X-Okapi-Tenant", TENANT_UBL)
            .header("content-type", ContentType.BINARY)
            .post(FINC_SELECT_FILES_ENDPOINT)
            .then()
            .statusCode(200)
            .extract()
            .response();

    // POST File 2
    Response secondPostFileResp =
        given()
            .body("foobar2".getBytes())
            .header("X-Okapi-Tenant", TENANT_UBL)
            .header("content-type", ContentType.BINARY)
            .post(FINC_SELECT_FILES_ENDPOINT)
            .then()
            .statusCode(200)
            .extract()
            .response();

    // Add posted files to filter's filterFiles
    String firstFileId = firstPostFileResp.getBody().print();
    String secondFileId = secondPostFileResp.getBody().print();
    FilterFile firstFilterFile =
        new FilterFile()
            .withId(UUID.randomUUID().toString())
            .withLabel("First FilterFile")
            .withFileId(firstFileId);
    FilterFile secondFilterFile =
        new FilterFile()
            .withId(UUID.randomUUID().toString())
            .withLabel("Second FilterFile")
            .withFileId(secondFileId);
    filter1.setFilterFiles(Arrays.asList(firstFilterFile, secondFilterFile));

    // POST filter
    Response resp =
        given()
            .body(Json.encode(filter1))
            .header("X-Okapi-Tenant", TENANT_UBL)
            .header("content-type", ContentType.JSON)
            .header("accept", ContentType.JSON)
            .post(FINC_SELECT_FILTERS_ENDPOINT)
            .then()
            .statusCode(201)
            .extract()
            .response();

    FincSelectFilter postedFilter = resp.getBody().as(FincSelectFilter.class);
    Assert.assertNotNull(postedFilter.getId());
    Assert.assertEquals(filter1.getLabel(), postedFilter.getLabel());
    Assert.assertEquals(filter1.getType(), postedFilter.getType());
    Assert.assertEquals(
        Arrays.asList(collectionId1.toString(), collectionId2.toString()),
        postedFilter.getCollectionIds());

    // GET filter
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_SELECT_FILTERS_ENDPOINT + "/" + postedFilter.getId())
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("id", equalTo(postedFilter.getId()))
        .body("label", equalTo(filter1.getLabel()))
        .body("type", equalTo(filter1.getType().value()))
        .body("$", not(hasKey("isil")));

    // PUT filter and define second filter file to be deleted
    FilterFile secondFilterFileToDelete = secondFilterFile.withDelete(true);
    FincSelectFilter changed =
        postedFilter
            .withLabel("CHANGED")
            .withFilterFiles(Arrays.asList(firstFilterFile, secondFilterFileToDelete))
            .withCollectionIds(Arrays.asList(collectionId1.toString()));

    given()
        .body(Json.encode(changed))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.TEXT)
        .put(FINC_SELECT_FILTERS_ENDPOINT + "/" + postedFilter.getId())
        .then()
        .statusCode(204);

    // GET: check that second file is deleted
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.BINARY)
        .get("/finc-select/files/" + secondFileId)
        .then()
        .statusCode(404);

    // GET changed filter
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_SELECT_FILTERS_ENDPOINT)
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("fincSelectFilters.size()", equalTo(1))
        .body("fincSelectFilters[0].id", equalTo(changed.getId()))
        .body("fincSelectFilters[0].label", equalTo(changed.getLabel()))
        .body("fincSelectFilters[0].filterFiles.size()", equalTo(1))
        .body("fincSelectFilters[0].collectionIds.size()", equalTo(1))
        .body("fincSelectFilters[0].collectionIds[0]", equalTo(collectionId1.toString()))
        .body("$", not(hasKey("isil")));

    // GET - Different tenant
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_SELECT_FILTERS_ENDPOINT)
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("fincSelectFilters.size()", equalTo(0));

    // DELETE filter
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .delete(FINC_SELECT_FILTERS_ENDPOINT + "/" + postedFilter.getId())
        .then()
        .statusCode(204);

    // GET first file and check that it was deleted
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.BINARY)
        .get("/finc-select/files/" + firstFileId)
        .then()
        .statusCode(404);
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

    // GEt filter_to_collection
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

    // DELETE filter
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .delete(FINC_SELECT_FILTERS_ENDPOINT + "/" + filter1.getId())
        .then()
        .statusCode(204);
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
