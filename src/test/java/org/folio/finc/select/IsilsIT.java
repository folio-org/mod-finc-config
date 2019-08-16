package org.folio.finc.select;

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
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

  @Rule public Timeout timeout = Timeout.seconds(10);
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
}
