package org.folio.finc.select;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.okapi.common.XOkapiHeaders.TENANT;

import java.util.List;
import org.folio.ApiTestBase;
import org.folio.TestUtils;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.Isil;
import org.folio.rest.jaxrs.model.Isils;
import org.folio.rest.jaxrs.model.Parameter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.restassured.http.Method;

public class IsilsIT extends ApiTestBase {

  private static final String UNIQUE_ERROR = "value already exists in table";
  private static final String LENGTH_ERROR = "size must be between";
  private static final String[] IGNORE_FIELDS = {"metadata"};
  private static Isil isilUBL;
  private static Isil isilDiku;

  @BeforeAll
  public static void beforeAll() throws Exception {
    TestUtils.setupTenants();
  }

  @AfterAll
  public static void afterAll() throws Exception {
    TestUtils.teardownTenants();
  }

  @BeforeEach
  void setUp() {
    isilUBL = loadIsilUbl();
    isilDiku = loadIsilDiku();
  }

  @AfterEach
  void tearDown() {
    deleteIsil(isilUBL.getId());
    deleteIsil(isilDiku.getId());
  }

  @Test
  void testGetFincConfigIsils() {
    assertThat(
            given()
                .header(TENANT, TENANT_UBL)
                .header(CONTENT_TYPE, APPLICATION_JSON)
                .get(ISILS_API_ENDPOINT)
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(200)
                .extract()
                .as(Isils.class)
                .getIsils())
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields(IGNORE_FIELDS)
        .containsExactlyInAnyOrder(isilUBL, isilDiku);
  }

  @Test
  void testGetFincConfigIsilById() {
    assertThat(
            getById(ISILS_API_ENDPOINT, TENANT_UBL, isilUBL.getId())
                .then()
                .statusCode(200)
                .extract()
                .as(Isil.class))
        .usingRecursiveComparison()
        .ignoringFields(IGNORE_FIELDS)
        .isEqualTo(isilUBL);
  }

  @Test
  void testPutFincConfigIsil() {
    Isil isilChanged = TestUtils.clone(isilUBL).withLibrary("FooBar");

    putById(ISILS_API_ENDPOINT, TENANT_UBL, isilUBL.getId(), isilChanged).then().statusCode(204);

    Isil getByIdResult =
      getById(ISILS_API_ENDPOINT, TENANT_UBL, isilUBL.getId())
        .then()
        .contentType(APPLICATION_JSON)
        .statusCode(200)
        .extract()
        .as(Isil.class);
    assertThat(getByIdResult)
      .usingRecursiveComparison()
      .ignoringFields(IGNORE_FIELDS)
      .isEqualTo(isilChanged);
  }

  @ParameterizedTest
  @CsvSource(value = {"PUT", "POST"})
  void testThatAttributeIsilIsUnique(Method method) {
    String endpoint =
        method == Method.PUT ? ISILS_API_ENDPOINT + "/" + isilDiku.getId() : ISILS_API_ENDPOINT;
    Isil newIsil =
        new Isil().withIsil(isilUBL.getIsil()).withLibrary("New Library").withTenant("test-tenant");

    List<Error> errors =
        sendEntity(endpoint, TENANT_UBL, method, newIsil)
            .then()
            .statusCode(422)
            .extract()
            .as(Errors.class)
            .getErrors();
    assertThat(errors)
        .hasSize(1)
        .first()
        .satisfies(error -> assertThat(error.getMessage()).contains(UNIQUE_ERROR));
  }

  @ParameterizedTest
  @CsvSource(value = {"PUT", "POST"})
  void testThatAttributeTenantIsUnique(Method method) {
    String endpoint =
        method == Method.PUT ? ISILS_API_ENDPOINT + "/" + isilDiku.getId() : ISILS_API_ENDPOINT;
    Isil newIsil =
        new Isil().withIsil("abc-123").withLibrary("New Library").withTenant(isilUBL.getTenant());

    List<Error> errors =
        sendEntity(endpoint, TENANT_UBL, method, newIsil)
            .then()
            .statusCode(422)
            .extract()
            .as(Errors.class)
            .getErrors();
    assertThat(errors)
        .isNotEmpty()
        .allSatisfy(error -> assertThat(error.getMessage()).contains(UNIQUE_ERROR));
  }

  @ParameterizedTest
  @CsvSource(value = {"PUT", "POST"})
  void testThatAttributesAreNotEmpty(Method method) {
    String endpoint =
        method == Method.PUT ? ISILS_API_ENDPOINT + "/" + isilDiku.getId() : ISILS_API_ENDPOINT;
    Isil newIsil = new Isil().withIsil("").withLibrary("").withTenant("");

    List<Error> errors =
        sendEntity(endpoint, TENANT_UBL, method, newIsil)
            .then()
            .statusCode(422)
            .extract()
            .as(Errors.class)
            .getErrors();
    assertThat(errors)
        .isNotEmpty()
        .allSatisfy(error -> assertThat(error.getMessage()).contains(LENGTH_ERROR))
        .flatExtracting(Error::getParameters)
        .extracting(Parameter::getKey)
        .containsExactlyInAnyOrder("isil", "library", "tenant");
  }
}
