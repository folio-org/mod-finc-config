package org.folio.finc.config;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.jayway.restassured.http.ContentType;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.finc.mocks.MockOrganization;
import org.folio.rest.jaxrs.model.FincConfigMetadataSource;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class ConfigMetadataSourcesIT extends AbstractMetadataSourcesIT {

  @Test
  public void checkThatWeCanAddGetPutAndDeleteMetadataSources() {
    String mockedOkapiUrl = "http://localhost:" + wireMockRule.port();
    MockOrganization.mockOrganizationFound(organizationUUID1235);
    // POST
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
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_CONFIG_METADATA_SOURCES_ENDPOINT + "/" + metadataSource2.getId())
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("id", equalTo(metadataSource2.getId()))
        .body("label", equalTo(metadataSource2.getLabel()))
        .body("status", equalTo(metadataSource2.getStatus().value()))
        .body("accessUrl", equalTo(metadataSource2.getAccessUrl()));

    // PUT
    given()
        .body(Json.encode(metadataSource2Changed))
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.TEXT)
        .put(FINC_CONFIG_METADATA_SOURCES_ENDPOINT + "/" + metadataSource2.getId())
        .then()
        .statusCode(204);

    // GET changed resource
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_CONFIG_METADATA_SOURCES_ENDPOINT + "/" + metadataSource2.getId())
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("id", equalTo(metadataSource2Changed.getId()))
        .body("label", equalTo(metadataSource2Changed.getLabel()))
        .body("status", equalTo(metadataSource2Changed.getStatus().value()))
        .body("accessUrl", equalTo(metadataSource2Changed.getAccessUrl()));

    // DELETE
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.TEXT)
        .delete(FINC_CONFIG_METADATA_SOURCES_ENDPOINT + "/" + metadataSource2.getId())
        .then()
        .statusCode(204);

    // GET again
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_CONFIG_METADATA_SOURCES_ENDPOINT + "/" + metadataSource2.getId())
        .then()
        .statusCode(404);

    // GET all
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_CONFIG_METADATA_SOURCES_ENDPOINT)
        .then()
        .statusCode(200)
        .body("totalRecords", equalTo(0));
  }

  @Test
  public void checkThatWeCanSearchByCQL() {
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

    String cql = "?query=(label=\"First Metadata Source*\")";
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_CONFIG_METADATA_SOURCES_ENDPOINT + cql)
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("fincConfigMetadataSources.size()", equalTo(1))
        .body("fincConfigMetadataSources[0].id", equalTo(metadataSource1.getId()))
        .body("fincConfigMetadataSources[0].label", equalTo(metadataSource1.getLabel()))
        .body("fincConfigMetadataSources[0].status", equalTo(metadataSource1.getStatus().value()));

    String cql2 = "?query=(label=\"FOO*\")";
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_CONFIG_METADATA_SOURCES_ENDPOINT + cql2)
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("totalRecords", equalTo(0));

    String cqlSolrShard = "?query=(solrShard==\"UBL main\")";
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_CONFIG_METADATA_SOURCES_ENDPOINT + cqlSolrShard)
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("fincConfigMetadataSources.size()", equalTo(1))
        .body("fincConfigMetadataSources[0].id", equalTo(metadataSource1.getId()))
        .body("fincConfigMetadataSources[0].label", equalTo(metadataSource1.getLabel()))
        .body("fincConfigMetadataSources[0].status", equalTo(metadataSource1.getStatus().value()))
        .body(
            "fincConfigMetadataSources[0].solrShard",
            equalTo(metadataSource1.getSolrShard().value()));

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

  @Test
  public void checkThatInvalidMetadataSourceIsNotPosted() {
    FincConfigMetadataSource metadataSourceInvalid =
        Json.decodeValue(
                Json.encode(metadataSource2), FincConfigMetadataSource.class)
            .withLabel(null);
    given()
        .body(Json.encode(metadataSourceInvalid))
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .post(FINC_CONFIG_METADATA_SOURCES_ENDPOINT)
        .then()
        .statusCode(422);
  }
}
