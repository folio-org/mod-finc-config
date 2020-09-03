package org.folio.finc.select;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.UUID;
import org.folio.finc.ApiTestBase;
import org.folio.rest.jaxrs.model.Isil;
import org.folio.rest.jaxrs.model.Isils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class IsilsIT extends ApiTestBase {

  @Rule
  public Timeout timeout = Timeout.seconds(10);
  private Isil isilUBL;

  @Before
  public void init() {
    isilUBL = loadIsilUbl();
  }

  @After
  public void tearDown() {
    deleteIsil(isilUBL.getId());
  }

  @Test
  public void testGetIsilForTenant() {
    Response ubl =
        given()
            .header("X-Okapi-Tenant", TENANT_UBL)
            .header("content-type", ContentType.JSON)
            .get(ISILS_API_ENDPOINT)
            .then()
            .contentType(ContentType.JSON.toString())
            .statusCode(200)
            .extract()
            .response();
    Isils isils = ubl.getBody().as(Isils.class);
    assertEquals(1, isils.getIsils().size());
    Isil ublIsil = isils.getIsils().get(0);
    assertEquals(isilUBL.getIsil(), ublIsil.getIsil());
  }

  @Test
  public void testCannotPostTwoIsilsForTenant() {

    Isil isilChanged = new Isil()
        .withIsil("FOO-01")
        .withTenant(isilUBL.getTenant())
        .withLibrary(isilUBL.getLibrary())
        .withId(UUID.randomUUID().toString());

    // POST
    given()
        .body(Json.encode(isilChanged))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .post(ISILS_API_ENDPOINT)
        .then()
        .statusCode(400);
  }

  @Test
  public void testCannotPutTwoIsilsForTenant() {

    Isil isilChanged = new Isil()
        .withIsil("FOO-01")
        .withTenant(isilUBL.getTenant())
        .withLibrary(isilUBL.getLibrary())
        .withId(isilUBL.getId());

    // PUT
    given()
        .body(Json.encode(isilChanged))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.TEXT)
        .put(ISILS_API_ENDPOINT + "/" + isilUBL.getId())
        .then()
        .statusCode(400);
  }

  @Test
  public void testCanPut() {

    final String newLibrary = "FooBar";

    Isil isilChanged = new Isil()
        .withIsil(isilUBL.getIsil())
        .withTenant(isilUBL.getTenant())
        .withLibrary(newLibrary)
        .withId(isilUBL.getId());

    // PUT
    given()
        .body(Json.encode(isilChanged))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.TEXT)
        .put(ISILS_API_ENDPOINT + "/" + isilUBL.getId())
        .then()
        .statusCode(204);

    // GET
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(ISILS_API_ENDPOINT + "/" + isilUBL.getId())
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("id", equalTo(isilUBL.getId()))
        .body("library", equalTo(newLibrary));
  }
}
