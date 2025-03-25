package org.folio.finc.config;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.UUID;
import org.folio.finc.ApiTestBase;
import org.folio.finc.TestUtils;
import org.folio.rest.jaxrs.model.Isil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class ConfigFilesIT extends ApiTestBase {

  private static final String TEST_CONTENT = "This is the test content!!!!";
  @Rule public Timeout timeout = Timeout.seconds(10);
  private Isil isilUbl;
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
    isilUbl = loadIsilUbl();
    isilDiku = loadIsilDiku();
  }

  @After
  public void tearDown() {
    deleteIsil(isilUbl.getId());
    deleteIsil(isilDiku.getId());
  }

  @Test
  public void checkThatWeCanGetAFile() {
    // POST File
    Response postResponse =
        given()
            .body(TEST_CONTENT.getBytes())
            .header("X-Okapi-Tenant", TENANT_UBL)
            .header("content-type", ContentType.BINARY)
            .post(FINC_CONFIG_FILES_ENDPOINT + "?isil=" + isilUbl.getIsil())
            .then()
            .statusCode(200)
            .extract()
            .response();
    String id = postResponse.getBody().print();

    // GET
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("content-type", ContentType.BINARY)
        .get(FINC_CONFIG_FILES_ENDPOINT + "/" + id)
        .then()
        .contentType(ContentType.BINARY.toString())
        .statusCode(200)
        .body(equalTo(TEST_CONTENT));

    // GET by unknown id
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("content-type", ContentType.TEXT)
        .get(FINC_CONFIG_FILES_ENDPOINT + "/" + UUID.randomUUID().toString())
        .then()
        .contentType(ContentType.TEXT)
        .statusCode(404);

    // GET by finc-select with correct tenant
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.BINARY)
        .get(FINC_SELECT_FILES_ENDPOINT + "/" + id)
        .then()
        .contentType(ContentType.BINARY.toString())
        .statusCode(200)
        .body(equalTo(TEST_CONTENT));

    // GET by finc-select with incorrect tenant
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("content-type", ContentType.TEXT)
        .get(FINC_SELECT_FILES_ENDPOINT + "/" + id)
        .then()
        .contentType(ContentType.TEXT.toString())
        .statusCode(404);

    // DELETE
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .delete(FINC_CONFIG_FILES_ENDPOINT + "/" + id)
        .then()
        .statusCode(204);
  }

  @Test
  public void checkThatWeCannotPostWithMissingIsil() {
    // POST File
    given()
        .body(TEST_CONTENT.getBytes())
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.BINARY)
        .post(FINC_CONFIG_FILES_ENDPOINT)
        .then()
        .statusCode(400);
  }
}
