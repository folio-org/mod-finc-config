package org.folio.finc.select;

import static com.jayway.restassured.RestAssured.given;

import com.jayway.restassured.http.ContentType;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.finc.ApiTestBase;
import org.folio.rest.jaxrs.model.Credential;
import org.folio.rest.jaxrs.model.Isil;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class SelectEZBCredentialsIT extends ApiTestBase {

  @Rule
  public Timeout timeout = Timeout.seconds(10);
  private Credential credUBL;
  private Credential credDiku;
  private Isil isilUBL;
  private Isil isilDiku;

  @Before
  public void init() {
    isilUBL = loadIsilUbl();
    isilDiku = loadIsilDiku();

    credUBL = new Credential()
        .withPassword("pw")
        .withUser("user")
        .withLibId("ubl");

    credDiku = new Credential()
        .withPassword("pw")
        .withUser("user")
        .withLibId("diku");
  }

  @After
  public void cleanup() {
    // DELETE credentials
    given()
        .body(Json.encode(credDiku))
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("Content-Type", ContentType.JSON)
        .header("Accept", ContentType.TEXT)
        .delete(FINC_CONFIG_EZB_CREDENTIALS_ENDPOINT + "/" + isilDiku.getIsil())
        .then()
        .statusCode(204);
  }

  @Test
  public void checkThatUserCanAddAndChangeCred() {
    // PUT
    given()
        .body(Json.encode(credDiku))
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("Content-Type", ContentType.JSON)
        .header("Accept", ContentType.JSON)
        .put(FINC_SELECT_EZB_CREDENTIALS_ENDPOINT)
        .then()
        .statusCode(200)
        .body("isil", Matchers.equalTo(isilDiku.getIsil()))
        .body("user", Matchers.equalTo(credDiku.getUser()));

    // GET
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("Content-Type", ContentType.JSON)
        .header("Accept", ContentType.JSON)
        .get(FINC_SELECT_EZB_CREDENTIALS_ENDPOINT)
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("isil", Matchers.equalTo(isilDiku.getIsil()))
        .body("user", Matchers.equalTo(credDiku.getUser()));

    // Change credential
    given()
        .body(Json.encode(credDiku.withUser("changed")))
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("Content-Type", ContentType.JSON)
        .header("Accept", ContentType.JSON)
        .put(FINC_SELECT_EZB_CREDENTIALS_ENDPOINT)
        .then()
        .statusCode(200)
        .body("isil", Matchers.equalTo(isilDiku.getIsil()))
        .body("user", Matchers.equalTo("changed"));

    // GET
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("Content-Type", ContentType.JSON)
        .header("Accept", ContentType.JSON)
        .get(FINC_SELECT_EZB_CREDENTIALS_ENDPOINT)
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("isil", Matchers.equalTo(isilDiku.getIsil()))
        .body("user", Matchers.equalTo("changed"));
  }
}