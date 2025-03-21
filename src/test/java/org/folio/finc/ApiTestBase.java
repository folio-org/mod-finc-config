package org.folio.finc;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static com.google.common.net.MediaType.OCTET_STREAM;
import static io.restassured.RestAssured.given;
import static org.folio.okapi.common.XOkapiHeaders.TENANT;

import io.restassured.http.ContentType;
import io.vertx.core.json.Json;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.folio.rest.jaxrs.model.Isil;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class ApiTestBase {

  protected static final String TENANT_UBL = "ubl";
  protected static final String TENANT_DIKU = "diku";

  protected static final String ISILS_API_ENDPOINT = "/finc-config/isils";
  protected static final String FINC_SELECT_FILES_ENDPOINT = "/finc-select/files";
  protected static final String FINC_SELECT_FILTERS_ENDPOINT = "/finc-select/filters";
  protected static final String FINC_SELECT_METADATA_COLLECTIONS_ENDPOINT =
      "/finc-select/metadata-collections";
  protected static final String FINC_SELECT_METADATA_SOURCES_ENDPOINT =
      "/finc-select/metadata-sources";
  protected static final String FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT =
      "/finc-config/metadata-collections";
  protected static final String FINC_CONFIG_METADATA_SOURCES_ENDPOINT =
      "/finc-config/metadata-sources";
  protected static final String FINC_CONFIG_CONTACTS_ENDPOINT = "/finc-config/contacts";
  protected static final String FINC_CONFIG_TINY_METADATA_SOURCES_ENDPOINT =
      "/finc-config/tiny-metadata-sources";
  protected static final String FINC_CONFIG_FILTERS_ENDPOINT = "/finc-config/filters";
  protected static final String FINC_CONFIG_FILES_ENDPOINT = "/finc-config/files";
  protected static final String FINC_CONFIG_EZB_CREDENTIALS_ENDPOINT =
      "/finc-config/ezb-credentials";
  protected static final String FINC_SELECT_EZB_CREDENTIALS_ENDPOINT =
      "/finc-select/ezb-credentials";

  private static boolean runningOnOwn;

  @BeforeClass
  public static void before() throws Exception {
    if (ITTestSuiteJunit4.isNotInitialised()) {
      System.out.println("Running test on own, initialising suite manually");
      runningOnOwn = true;
      ITTestSuiteJunit4.before();
    }
  }

  @BeforeAll
  static void beforeAll() throws Exception {
    before();
  }

  @AfterClass
  public static void after() throws InterruptedException, ExecutionException, TimeoutException {
    if (runningOnOwn) {
      System.out.println("Running test on own, un-initialising suite manually");
      ITTestSuiteJunit4.after();
    }
  }

  @AfterAll
  static void afterAll() throws ExecutionException, InterruptedException, TimeoutException {
    after();
  }

  public static Isil loadIsilUbl() {
    Isil isil =
        new Isil()
            .withId(UUID.randomUUID().toString())
            .withLibrary("UB Leipzig")
            .withIsil("DE-15")
            .withTenant("ubl");
    return loadIsil(isil);
  }

  public static Isil loadIsilDiku() {
    Isil isil =
        new Isil()
            .withId(UUID.randomUUID().toString())
            .withLibrary("DIKU")
            .withIsil("DIKU-01")
            .withTenant("diku");
    return loadIsil(isil);
  }

  public static Isil loadIsil(Isil isil) {
    Isil isilResp =
        given()
            .body(Json.encode(isil))
            .header("X-Okapi-Tenant", TENANT_UBL)
            .header("content-type", ContentType.JSON)
            .header("accept", ContentType.JSON)
            .post(ISILS_API_ENDPOINT)
            .then()
            .statusCode(201)
            .extract()
            .response()
            .as(Isil.class);
    Assert.assertEquals(isil.getIsil(), isilResp.getIsil());
    return isilResp;
  }

  public void deleteIsil(String isilId) {
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .delete(ISILS_API_ENDPOINT + "/" + isilId)
        .then()
        .statusCode(204);
  }

  public static String post(String endpoint, Object entity, String tenantId) {
    return given()
        .body(Json.encode(entity))
        .header(TENANT, tenantId)
        .header(CONTENT_TYPE, JSON_UTF_8.withoutParameters().toString())
        .post(endpoint)
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getString("id");
  }

  public static void put(String endpoint, Object entity, String tenantId) {
    given()
        .body(Json.encode(entity))
        .header(TENANT, tenantId)
        .header(CONTENT_TYPE, JSON_UTF_8.withoutParameters().toString())
        .put(endpoint)
        .then()
        .statusCode(200);
  }

  public static String postFile(String content, String tenantId) {
    return given()
        .body(content.getBytes())
        .header(TENANT, tenantId)
        .header(CONTENT_TYPE, OCTET_STREAM.toString())
        .post(FINC_SELECT_FILES_ENDPOINT)
        .then()
        .statusCode(200)
        .extract()
        .asString();
  }
}
