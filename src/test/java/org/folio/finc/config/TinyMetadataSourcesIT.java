package org.folio.finc.config;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.jayway.restassured.http.ContentType;
import io.vertx.core.json.Json;
import org.folio.finc.mocks.MockOrganization;
import org.junit.Test;

public class TinyMetadataSourcesIT extends AbstractMetadataSourcesIT {

  @Test
  public void checkThatWeCanGetTinyMetadataSources() {
    String mockedOkapiUrl = "http://localhost:" + wireMockRule.port();
    MockOrganization.mockOrganizationFound(organizationUUID1235);
    given()
        .body(Json.encode(metadataSource2))
        .header("X-Okapi-Tenant", TENANT)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .post(METADATA_SOURCES_URL)
        .then()
        .statusCode(201)
        .body("id", equalTo(metadataSource2.getId()))
        .body("label", equalTo(metadataSource2.getLabel()))
        .body("description", equalTo(metadataSource2.getDescription()));

    // GET
    given()
        .header("X-Okapi-Tenant", TENANT)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .get(TINY_MDS_URL)
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("tinyMetadataSources.size()", equalTo(1))
        .body("tinyMetadataSources[0].id", equalTo(metadataSource2.getId()))
        .body("tinyMetadataSources[0].label", equalTo(metadataSource2.getLabel()));
  }
}
