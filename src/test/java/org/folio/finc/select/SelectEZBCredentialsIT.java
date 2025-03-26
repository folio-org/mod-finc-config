package org.folio.finc.select;

import static io.restassured.RestAssured.given;

import io.restassured.http.ContentType;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.ApiTestBase;
import org.folio.TestUtils;
import org.folio.rest.jaxrs.model.Credential;
import org.folio.rest.jaxrs.model.Isil;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class SelectEZBCredentialsIT extends ApiTestBase {

  @Rule public Timeout timeout = Timeout.seconds(10);
  private Credential credDiku;
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
    isilDiku = loadIsilDiku();
    credDiku = new Credential().withPassword("pw").withUser("user").withLibId("diku");
  }

  @After
  public void cleanUp() {
    deleteIsil(isilDiku.getId());
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

    // DELETE
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .delete(FINC_SELECT_EZB_CREDENTIALS_ENDPOINT)
        .then()
        .statusCode(204);

    // GET
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("Content-Type", ContentType.JSON)
        .header("Accept", ContentType.JSON)
        .get(FINC_SELECT_EZB_CREDENTIALS_ENDPOINT)
        .then()
        .contentType(ContentType.TEXT)
        .statusCode(404);
  }

  @Test
  public void checkThatUserCannotAddCredWithWrongIsil() {
    Credential c =
        new Credential()
            .withLibId("libId")
            .withPassword("password")
            .withUser("username")
            .withIsil("foobar");

    // PUT
    given()
        .body(Json.encode(c))
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("Content-Type", ContentType.JSON)
        .header("Accept", ContentType.JSON)
        .put(FINC_SELECT_EZB_CREDENTIALS_ENDPOINT)
        .then()
        .statusCode(400);
  }
}
