package org.folio.finc.config;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.jayway.restassured.http.ContentType;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.UUID;
import org.folio.finc.ApiTestBase;
import org.folio.rest.jaxrs.model.FincSelectFilter;
import org.folio.rest.jaxrs.model.FincSelectFilter.Type;
import org.folio.rest.jaxrs.model.Isil;
import org.junit.After;
import org.junit.Before;
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
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .post(FINC_SELECT_FILTERS_ENDPOINT)
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
        .post(FINC_SELECT_FILTERS_ENDPOINT)
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

    // DELETE filters
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .delete(FINC_SELECT_FILTERS_ENDPOINT + "/" + filterUBL.getId())
        .then()
        .statusCode(204);

    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .delete(FINC_SELECT_FILTERS_ENDPOINT + "/" + filterDIKU.getId())
        .then()
        .statusCode(204);

  }

  @Test
  public void checkUnimplementedEndpoints() {
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .body(Json.encode(filterUBL))
        .post(FINC_CONFIG_FILTERS_ENDPOINT)
        .then()
        .contentType(ContentType.JSON)
        .statusCode(501);

    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.TEXT)
        .body(Json.encode(filterUBL))
        .put(FINC_CONFIG_FILTERS_ENDPOINT + "/" + filterUBL.getId())
        .then()
        .contentType(ContentType.JSON)
        .statusCode(501);

    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .delete(FINC_CONFIG_FILTERS_ENDPOINT + "/" + filterUBL.getId())
        .then()
        .statusCode(501);
  }

}
