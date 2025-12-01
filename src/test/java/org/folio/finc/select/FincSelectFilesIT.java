package org.folio.finc.select;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.ApiTestBase;
import org.folio.TestUtils;
import org.folio.rest.jaxrs.model.Isil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

@RunWith(VertxUnitRunner.class)
public class FincSelectFilesIT extends ApiTestBase {

  private static final String TEST_CONTENT = "This is the test content!!!!";
  @Rule public Timeout timeout = Timeout.seconds(10);
  private Isil isilUBL;
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
    isilUBL = loadIsilUbl();
    isilDiku = loadIsilDiku();
  }

  @After
  public void tearDown() {
    deleteIsil(isilDiku.getId());
    deleteIsil(isilUBL.getId());
  }

  @Test
  public void checkThatWeCanAddGetAndDeleteFiles() {
    // POST File
    Response postResponse =
        given()
            .body(TEST_CONTENT.getBytes())
            .header("X-Okapi-Tenant", TENANT_UBL)
            .header("content-type", ContentType.BINARY)
            .post(FINC_SELECT_FILES_ENDPOINT)
            .then()
            .statusCode(200)
            .extract()
            .response();
    String id = postResponse.getBody().print();

    // GET
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.BINARY)
        .get(FINC_SELECT_FILES_ENDPOINT + "/" + id)
        .then()
        .contentType(ContentType.BINARY.toString())
        .statusCode(200)
        .body(equalTo(TEST_CONTENT));

    // DELETE
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .delete(FINC_SELECT_FILES_ENDPOINT + "/" + id)
        .then()
        .statusCode(204);
  }

  @Test
  public void checkThatGetNonExistentFileReturns404() {
    given()
      .header("X-Okapi-Tenant", TENANT_UBL)
      .get(FINC_SELECT_FILES_ENDPOINT + "/" + UUID.randomUUID())
      .then()
      .statusCode(404);
  }

  @Test
  public void checkThatDeletedFileCannotBeRetrieved() {
    Response postResponse =
      given()
        .body(TEST_CONTENT.getBytes())
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.BINARY)
        .post(FINC_SELECT_FILES_ENDPOINT)
        .then()
        .statusCode(200)
        .extract()
        .response();

    String id = postResponse.getBody().print();

    // DELETE
    given()
      .header("X-Okapi-Tenant", TENANT_UBL)
      .delete(FINC_SELECT_FILES_ENDPOINT + "/" + id)
      .then()
      .statusCode(204);

    // Try to GET deleted file
    given()
      .header("X-Okapi-Tenant", TENANT_UBL)
      .get(FINC_SELECT_FILES_ENDPOINT + "/" + id)
      .then()
      .statusCode(404);
  }

  @Test
  public void checkThatWeCanUploadMediumSizedFile() {
    byte[] content = new byte[5 * 1024 * 1024]; // 5 MB

    Response postResponse =
      given()
        .body(content)
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.BINARY)
        .post(FINC_SELECT_FILES_ENDPOINT)
        .then()
        .statusCode(200)
        .extract()
        .response();

    String id = postResponse.getBody().print();

    // DELETE
    given()
      .header("X-Okapi-Tenant", TENANT_UBL)
      .delete(FINC_SELECT_FILES_ENDPOINT + "/" + id)
      .then()
      .statusCode(204);
  }

  @Test
  public void checkThatTenantWithoutIsilCannotAccessFiles() {
    // Upload file as UBL tenant
    Response postResponse =
      given()
        .body(TEST_CONTENT.getBytes())
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.BINARY)
        .post(FINC_SELECT_FILES_ENDPOINT)
        .then()
        .statusCode(200)
        .extract()
        .response();

    String id = postResponse.getBody().print();

    // Try to access with different tenant (DIKU) - should not find it
    given()
      .header("X-Okapi-Tenant", TENANT_DIKU)
      .get(FINC_SELECT_FILES_ENDPOINT + "/" + id)
      .then()
      .statusCode(404);

    // Cleanup
    given()
      .header("X-Okapi-Tenant", TENANT_UBL)
      .delete(FINC_SELECT_FILES_ENDPOINT + "/" + id)
      .then()
      .statusCode(204);
  }

}
