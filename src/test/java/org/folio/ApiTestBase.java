package org.folio;

import static io.restassured.RestAssured.given;
import static org.folio.okapi.common.XOkapiHeaders.TENANT;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.jackson.DatabindCodec;
import java.util.UUID;
import org.folio.dbschema.ObjectMapperTool;
import org.folio.rest.jaxrs.model.Isil;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class ApiTestBase {

  static {
    // Configure Jackson with increased string length limit to support 50MB file uploads in tests
    StreamReadConstraints constraints =
        StreamReadConstraints.builder().maxStringLength(70 * 1024 * 1024).build();

    try {
      DatabindCodec.mapper().getFactory().setStreamReadConstraints(constraints);
      ObjectMapperTool.getMapper().getFactory().setStreamReadConstraints(constraints);
    } catch (Exception e) {
      System.err.println("WARNING: Failed to configure Jackson constraints in tests: " + e.getMessage());
    }
  }

  public static final String CONTENT_TYPE = HttpHeaders.CONTENT_TYPE;
  public static final String APPLICATION_JSON = MediaType.JSON_UTF_8.withoutParameters().toString();
  public static final String OCTET_STREAM = MediaType.OCTET_STREAM.toString();

  protected static final String TENANT_UBL = TestUtils.TENANT_UBL;
  protected static final String TENANT_DIKU = TestUtils.TENANT_DIKU;
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
  protected static final String TENANT_ENDPOINT = "/_/tenant";
  protected static final Vertx vertx = TestUtils.getVertx();

  private static boolean isRunningOnOwn = false;

  @BeforeClass
  public static void before() throws Exception {
    if (!TestUtils.isIsTestSuiteRunning()) {
      System.out.println("Running test on own, initialising suite manually");
      TestUtils.setupTestSuite();
      isRunningOnOwn = true;
    }
  }

  @BeforeAll
  static void beforeAll() throws Exception {
    before();
  }

  @AfterClass
  public static void after() throws Exception {
    if (isRunningOnOwn) {
      System.out.println("Running test on own, un-initialising suite manually");
      TestUtils.teardownTestSuite();
    }
  }

  @AfterAll
  static void afterAll() throws Exception {
    after();
  }

  protected static Isil loadIsilUbl() {
    Isil isil =
        new Isil()
            .withId(UUID.randomUUID().toString())
            .withLibrary("UB Leipzig")
            .withIsil("DE-15")
            .withTenant("ubl");
    return loadIsil(isil);
  }

  protected static Isil loadIsilDiku() {
    Isil isil =
        new Isil()
            .withId(UUID.randomUUID().toString())
            .withLibrary("DIKU")
            .withIsil("DIKU-01")
            .withTenant("diku");
    return loadIsil(isil);
  }

  protected static Isil loadIsil(Isil isil) {
    Isil isilResp =
        given()
            .body(Json.encode(isil))
            .header(TENANT, TENANT_UBL)
            .post(ISILS_API_ENDPOINT)
            .then()
            .statusCode(201)
            .extract()
            .response()
            .as(Isil.class);
    Assert.assertEquals(isil.getIsil(), isilResp.getIsil());
    return isilResp.withMetadata(null);
  }

  protected static Response sendEntity(
      String endpoint, String tenantId, Method method, Object entity) {
    return given().body(Json.encode(entity)).header(TENANT, tenantId).request(method, endpoint);
  }

  protected static String postAndGetId(String endpoint, Object entity, String tenantId) {
    return post(endpoint, tenantId, entity)
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getString("id");
  }

  protected static Response post(String endpoint, String tenantId, Object entity) {
    return given().body(Json.encode(entity)).header(TENANT, tenantId).post(endpoint);
  }

  protected static void put(String endpoint, String tenantId, Object entity) {
    given().body(Json.encode(entity)).header(TENANT, tenantId).put(endpoint).then().statusCode(200);
  }

  protected static Response putById(String endpoint, String tenantId, String id, Object entity) {
    return given().body(Json.encode(entity)).header(TENANT, tenantId).put(endpoint + "/" + id);
  }

  protected static Response getById(String endpoint, String tenantId, String id) {
    return given().header(TENANT, tenantId).get(endpoint + "/" + id);
  }

  protected static String postFile(String content, String tenantId) {
    return given()
        .body(content.getBytes())
        .header(TENANT, tenantId)
        .header(CONTENT_TYPE, OCTET_STREAM)
        .post(FINC_SELECT_FILES_ENDPOINT)
        .then()
        .statusCode(200)
        .extract()
        .asString();
  }

  protected void deleteIsil(String isilId) {
    given()
        .header(TENANT, TENANT_UBL)
        .delete(ISILS_API_ENDPOINT + "/" + isilId)
        .then()
        .statusCode(204);
  }
}
