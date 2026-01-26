package org.folio.finc.config;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.http.ContentType;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.TestUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class TinyMetadataSourcesIT extends AbstractMetadataSourcesIT {

  @BeforeClass
  public static void beforeClass() throws Exception {
    TestUtils.setupTenants();
  }

  @AfterClass
  public static void afterClass() throws Exception {
    TestUtils.teardownTenants();
  }

  @Test
  public void checkThatWeCanGetTinyMetadataSources() {
    String mockedOkapiUrl = "http://localhost:" + wireMockRule.port();

    given()
        .body(Json.encode(metadataSource2))
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .post(FINC_CONFIG_METADATA_SOURCES_ENDPOINT)
        .then()
        .statusCode(201)
        .body("id", equalTo(metadataSource2.getId()))
        .body("label", equalTo(metadataSource2.getLabel()))
        .body("description", equalTo(metadataSource2.getDescription()));

    // GET
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_CONFIG_TINY_METADATA_SOURCES_ENDPOINT)
        .then()
        .statusCode(200)
        .body("tinyMetadataSources.size()", equalTo(1))
        .body("tinyMetadataSources[0].id", equalTo(metadataSource2.getId()))
        .body("tinyMetadataSources[0].label", equalTo(metadataSource2.getLabel()));

    // DELETE
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.TEXT)
        .delete(FINC_CONFIG_METADATA_SOURCES_ENDPOINT + "/" + metadataSource2.getId())
        .then()
        .statusCode(204);
  }
}
