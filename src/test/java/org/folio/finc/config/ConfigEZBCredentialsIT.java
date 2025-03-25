package org.folio.finc.config;

import static io.restassured.RestAssured.given;

import io.restassured.http.ContentType;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.ApiTestBase;
import org.folio.TestUtils;
import org.folio.rest.jaxrs.model.Credential;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class ConfigEZBCredentialsIT extends ApiTestBase {

  @Rule
  public Timeout timeout = Timeout.seconds(10);
  private Credential credential;
  private Credential credentialChanged;

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
    credential = new Credential()
        .withIsil("DIKU-01")
        .withLibId("diku01")
        .withPassword("password01")
        .withUser("user01");

    credentialChanged = credential.withPassword("password01CHANGED"); // TODO: fix this
  }

  @Test
  public void checkThatWeCanPostPutGetAndDeleteCredential() {
    // POST
    given()
        .body(Json.encode(credential))
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("Content-Type", ContentType.JSON)
        .header("Accept", ContentType.JSON)
        .post(FINC_CONFIG_EZB_CREDENTIALS_ENDPOINT)
        .then()
        .statusCode(201)
        .body("isil", Matchers.equalTo(credential.getIsil()))
        .body("user", Matchers.equalTo(credential.getUser()));

    // GET
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("Content-Type", ContentType.JSON)
        .header("Accept", ContentType.JSON)
        .get(FINC_CONFIG_EZB_CREDENTIALS_ENDPOINT + "/" + credential.getIsil())
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("isil", Matchers.equalTo(credential.getIsil()))
        .body("user", Matchers.equalTo(credential.getUser()));

    // PUT
    given()
        .body(Json.encode(credentialChanged))
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("Content-Type", ContentType.JSON)
        .header("Accept", ContentType.TEXT)
        .put(FINC_CONFIG_EZB_CREDENTIALS_ENDPOINT + "/" + credential.getIsil())
        .then()
        .statusCode(204);

    // GET changed cred
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("Content-Type", ContentType.JSON)
        .header("Accept", ContentType.JSON)
        .get(FINC_CONFIG_EZB_CREDENTIALS_ENDPOINT + "/" + credential.getIsil())
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("isil", Matchers.equalTo(credential.getIsil()))
        .body("user", Matchers.equalTo(credential.getUser()))
        .body("password", Matchers.equalTo(credentialChanged.getPassword()));

    // GET all creds, check that there is only 1
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("Content-Type", ContentType.JSON)
        .header("Accept", ContentType.JSON)
        .get(FINC_CONFIG_EZB_CREDENTIALS_ENDPOINT)
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("totalRecords", Matchers.equalTo(1));

    // DELETE
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("Content-Type", ContentType.JSON)
        .header("Accept", ContentType.TEXT)
        .delete(FINC_CONFIG_EZB_CREDENTIALS_ENDPOINT + "/" + credential.getIsil())
        .then()
        .statusCode(204);

    // GET all creds, check that there is only 1
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("Content-Type", ContentType.JSON)
        .header("Accept", ContentType.JSON)
        .get(FINC_CONFIG_EZB_CREDENTIALS_ENDPOINT)
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("totalRecords", Matchers.equalTo(0));

    // GET deleted cred, check 404
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("Content-Type", ContentType.JSON)
        .header("Accept", ContentType.TEXT)
        .get(FINC_CONFIG_EZB_CREDENTIALS_ENDPOINT + "/" + credential.getIsil())
        .then()
        .statusCode(404);
  }

  @Test
  public void checkThatWeCannotPostCredsWithSameIsil() {
    // POST
    given()
        .body(Json.encode(credential))
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("Content-Type", ContentType.JSON)
        .header("Accept", ContentType.JSON)
        .post(FINC_CONFIG_EZB_CREDENTIALS_ENDPOINT)
        .then()
        .statusCode(201)
        .body("isil", Matchers.equalTo(credential.getIsil()))
        .body("user", Matchers.equalTo(credential.getUser()));

    // POST
    given()
        .body(Json.encode(credentialChanged))
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("Content-Type", ContentType.JSON)
        .header("Accept", ContentType.JSON)
        .post(FINC_CONFIG_EZB_CREDENTIALS_ENDPOINT)
        .then()
        .statusCode(400);

    // DELETE
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("Content-Type", ContentType.JSON)
        .header("Accept", ContentType.TEXT)
        .delete(FINC_CONFIG_EZB_CREDENTIALS_ENDPOINT + "/" + credential.getIsil())
        .then()
        .statusCode(204);
  }

  @Test
  public void checkThatWeCannotPostCredsWithWrongIsil() {
    // POST
    given()
        .body(Json.encode(credential))
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("Content-Type", ContentType.JSON)
        .header("Accept", ContentType.JSON)
        .post(FINC_CONFIG_EZB_CREDENTIALS_ENDPOINT)
        .then()
        .statusCode(201)
        .body("isil", Matchers.equalTo(credential.getIsil()))
        .body("user", Matchers.equalTo(credential.getUser()));

    // PUT
    given()
        .body(Json.encode(credential))
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("Content-Type", ContentType.JSON)
        .header("Accept", ContentType.TEXT)
        .put(FINC_CONFIG_EZB_CREDENTIALS_ENDPOINT + "/" + "fake-isil")
        .then()
        .statusCode(400);

    // DELETE
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("Content-Type", ContentType.JSON)
        .header("Accept", ContentType.TEXT)
        .delete(FINC_CONFIG_EZB_CREDENTIALS_ENDPOINT + "/" + credential.getIsil())
        .then()
        .statusCode(204);
  }


}
