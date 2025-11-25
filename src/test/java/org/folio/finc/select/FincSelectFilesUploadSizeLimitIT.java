package org.folio.finc.select;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

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

@RunWith(VertxUnitRunner.class)
public class FincSelectFilesUploadSizeLimitIT extends ApiTestBase {

  @Rule public Timeout timeout = Timeout.seconds(30);
  private Isil isilUbl;

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
  }

  @After
  public void tearDown() {
    deleteIsil(isilUbl.getId());
  }

  @Test
  public void testUploadFileBelowSizeLimit() {
    byte[] content = new byte[1024 * 1024];

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

    // Cleanup
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .delete(FINC_SELECT_FILES_ENDPOINT + "/" + id)
        .then()
        .statusCode(204);
  }

  @Test
  public void testUploadFileExceedsSizeLimit() {
    byte[] content = new byte[51 * 1024 * 1024];

    given()
        .body(content)
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.BINARY)
        .post(FINC_SELECT_FILES_ENDPOINT)
        .then()
        .statusCode(413)
        .contentType(ContentType.TEXT)
        .body(containsString("File size exceeds maximum allowed size of 50 MB"));
  }

  @Test
  public void testUploadLargeFileNearLimit() {
    byte[] content = new byte[50 * 1024 * 1024];

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

    // Cleanup
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .delete(FINC_SELECT_FILES_ENDPOINT + "/" + id)
        .then()
        .statusCode(204);
  }

  @Test
  public void testUploadFileJustOverLimit() {
    byte[] content = new byte[50 * 1024 * 1024 + 1];

    given()
        .body(content)
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.BINARY)
        .post(FINC_SELECT_FILES_ENDPOINT)
        .then()
        .statusCode(413)
        .contentType(ContentType.TEXT)
        .body(containsString("File size exceeds maximum allowed size of 50 MB"));
  }
}
