package org.folio.finc.config;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.http.ContentType;
import io.vertx.core.json.Json;
import org.folio.TestUtils;
import org.folio.finc.mocks.MockOrganization;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ConfigContactsIT extends AbstractMetadataSourcesIT {

  @BeforeClass
  public static void beforeClass() throws Exception {
    TestUtils.setupTenants();
  }

  @AfterClass
  public static void afterClass() throws Exception {
    TestUtils.teardownTenants();
  }

  @Test
  public void checkThatWeCanGetContacts() {
    String mockedOkapiUrl = "http://localhost:" + wireMockRule.port();

    MockOrganization.mockOrganizationFound(organizationUUID1234);
    given()
        .body(Json.encode(metadataSource1))
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .post(FINC_CONFIG_METADATA_SOURCES_ENDPOINT)
        .then()
        .statusCode(201)
        .body("id", equalTo(metadataSource1.getId()));

    // GET
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_CONFIG_CONTACTS_ENDPOINT)
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("contacts.size()", equalTo(2));

    // DELETE
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.TEXT)
        .delete(FINC_CONFIG_METADATA_SOURCES_ENDPOINT + "/" + metadataSource1.getId())
        .then()
        .statusCode(204);
  }
}
