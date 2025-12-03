package org.folio.finc;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.folio.ApiTestBase;
import org.folio.rest.jaxrs.model.Isil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/** Base class for file upload size limit tests across different endpoints */
public abstract class FileUploadSizeLimitTestBase extends ApiTestBase {

  protected Isil isilUbl;

  /** Returns the endpoint URL to use for file uploads */
  protected abstract String getUploadEndpoint();

  /** Returns the endpoint URL for file deletion (base path without ID) */
  protected abstract String getDeleteEndpoint();

  @BeforeEach
  public void init() {
    isilUbl = loadIsilUbl();
  }

  @AfterEach
  public void tearDown() {
    deleteIsil(isilUbl.getId());
  }

  protected void testUploadFileBelowSizeLimit() {
    byte[] content = new byte[1024 * 1024]; // 1 MB

    Response postResponse =
        given()
            .body(content)
            .header("X-Okapi-Tenant", TENANT_UBL)
            .header("content-type", ContentType.BINARY)
            .post(getUploadEndpoint())
            .then()
            .statusCode(200)
            .extract()
            .response();

    String id = postResponse.getBody().print();

    // Cleanup
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .delete(getDeleteEndpoint() + "/" + id)
        .then()
        .statusCode(204);
  }

  protected void testUploadFileExceedsSizeLimit() {
    byte[] content = new byte[51 * 1024 * 1024]; // 51 MB

    given()
        .body(content)
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.BINARY)
        .post(getUploadEndpoint())
        .then()
        .statusCode(413)
        .contentType(ContentType.TEXT)
        .body(containsString("File size exceeds maximum allowed size of 50 MB"));
  }

  protected void testUploadLargeFileNearLimit() {
    byte[] content = new byte[50 * 1024 * 1024]; // 50 MB exactly

    Response postResponse =
        given()
            .body(content)
            .header("X-Okapi-Tenant", TENANT_UBL)
            .header("content-type", ContentType.BINARY)
            .post(getUploadEndpoint())
            .then()
            .statusCode(200)
            .extract()
            .response();

    String id = postResponse.getBody().print();

    // Cleanup
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .delete(getDeleteEndpoint() + "/" + id)
        .then()
        .statusCode(204);
  }

  protected void testUploadFileJustOverLimit() {
    byte[] content = new byte[50 * 1024 * 1024 + 1]; // 50 MB + 1 byte

    given()
        .body(content)
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.BINARY)
        .post(getUploadEndpoint())
        .then()
        .statusCode(413)
        .contentType(ContentType.TEXT)
        .body(containsString("File size exceeds maximum allowed size of 50 MB"));
  }
}
