package org.folio.finc.select;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.finc.ApiTestBase;
import org.folio.rest.jaxrs.model.Isil;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class FincSelectFilesIT extends ApiTestBase {

  private static final String TEST_CONTENT = "This is the test content!!!!";
  @Rule public Timeout timeout = Timeout.seconds(10);
  private Isil isilUBL;
  private Isil isilDiku;

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
}
