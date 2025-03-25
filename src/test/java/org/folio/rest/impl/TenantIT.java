package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static io.restassured.RestAssured.given;
import static org.folio.okapi.common.XOkapiHeaders.TENANT;
import static org.folio.okapi.common.XOkapiHeaders.URL;
import static org.folio.rest.utils.Constants.MODULE_TENANT;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.folio.ApiTestBase;
import org.folio.TestUtils;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class TenantIT extends ApiTestBase {

  @Rule public Timeout timeout = Timeout.seconds(10);
  @Rule public WireMockRule wireMockRule = new WireMockRule(options().dynamicPort());

  @BeforeClass
  public static void beforeClass() throws Exception {
    TestUtils.setupTenants();
  }

  @AfterClass
  public static void afterClass() throws Exception {
    TestUtils.teardownTenants();
  }

  @Test
  public void testDeactivateAndPurge() {
    String mockedOkapiUrl = "http://localhost:" + wireMockRule.port();

    // GET
    given()
        .header(TENANT, TENANT_UBL)
        .get(FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT)
        .then()
        .contentType(APPLICATION_JSON)
        .statusCode(200);

    // Deactivate module for tenant UBL
    TenantAttributes attributes = new TenantAttributes().withModuleTo(null).withPurge(false);
    given()
        .body(attributes)
        .header(TENANT, TENANT_UBL)
        .header(URL, mockedOkapiUrl)
        .post(TENANT_ENDPOINT)
        .then()
        .statusCode(201);

    // Try to GET resource again with different tenant.
    given()
        .header(TENANT, TENANT_DIKU)
        .get(FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT)
        .then()
        .contentType(APPLICATION_JSON)
        .statusCode(200);

    // Purge module with tenant UBL
    TenantAttributes purgeAttributes = new TenantAttributes().withModuleTo(null).withPurge(true);
    given()
        .body(purgeAttributes)
        .header(TENANT, TENANT_UBL)
        .header(URL, mockedOkapiUrl)
        .post(TENANT_ENDPOINT)
        .then()
        .statusCode(400);

    // Deactivate module with tenant finc
    given()
        .body(purgeAttributes)
        .header(TENANT, MODULE_TENANT)
        .header(URL, mockedOkapiUrl)
        .post(TENANT_ENDPOINT)
        .then()
        .statusCode(201);

    // Try to GET resource again. Should fail.
    given()
        .header(TENANT, TENANT_DIKU)
        .get(FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT)
        .then()
        .statusCode(400);
  }
}
