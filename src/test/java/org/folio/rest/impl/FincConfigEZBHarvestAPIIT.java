package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.finc.mocks.EZBServiceMock.EZB_FILE_CONTENT;
import static org.hamcrest.Matchers.containsString;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.finc.mocks.EZBServiceMock;
import org.folio.finc.periodic.AbstractEZBHarvestTest;
import org.folio.finc.periodic.ezb.EZBService;
import org.folio.rest.RestVerticle;
import org.junit.Test;

public class FincConfigEZBHarvestAPIIT extends AbstractEZBHarvestTest {

  private static final String ENDPOINT = "/finc-config/ezb-harvest";
  private static final String TENANT_WITHOUT_ISIL = "tenant-without-isil";
  private static final String EXISTING_CONTENT = "existing ezb file content";
  private static final String FILE_ID = "f81e18f1-4427-4eb7-a936-2039ca1c5023";
  private static final Date DATE =
      Date.from(LocalDate.of(1977, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC));

  @Test
  public void checkThatEZBFileIsHarvested(TestContext context) {
    harvest(new EZBServiceMock(), TENANT)
        .flatMap(
            response -> {
              assertThat(response.getStatus()).isEqualTo(204);
              return getUpdatedEZBFile();
            })
        .onComplete(context.asyncAssertSuccess(s -> assertThat(s).isEqualTo(EZB_FILE_CONTENT)));
  }

  @Test
  public void checkThatNothingIsHarvestedForTenantWithoutIsil(TestContext context) {
    insertEZBFile(EXISTING_CONTENT, FILE_ID, DATE)
        .flatMap(v -> harvest(new EZBServiceMock(), TENANT_WITHOUT_ISIL))
        .flatMap(
            response -> {
              assertThat(response.getStatus()).isEqualTo(204);
              return getUpdatedEZBFile();
            })
        .onComplete(context.asyncAssertSuccess(s -> assertThat(s).isEqualTo(EXISTING_CONTENT)));
  }

  @Test
  public void checkThatFailedHarvestIsReported(TestContext context) {
    EZBService failing = (user, password, libId, vertx) -> Future.failedFuture("ezb is down");
    harvest(failing, TENANT)
        .onComplete(
            context.asyncAssertSuccess(
                response -> {
                  assertThat(response.getStatus()).isEqualTo(500);
                  assertThat(response.getEntity()).isEqualTo("ezb is down");
                }));
  }

  @Test
  public void checkThatEndpointIsRoutedAndSkipsTenantWithoutIsil() {
    given()
        .header(RestVerticle.OKAPI_HEADER_TENANT, TENANT_WITHOUT_ISIL)
        .post(ENDPOINT)
        .then()
        .statusCode(204);
  }

  /**
   * EZB_DOWNLOAD_URL points at an unreachable port during tests, so the error message of the
   * download attempt proves that the tenant of the timer call was resolved to its isil and
   * credentials.
   */
  @Test
  public void checkThatHarvestIsAttemptedForTenantWithCredentials() {
    given()
        .header(RestVerticle.OKAPI_HEADER_TENANT, TENANT)
        .post(ENDPOINT)
        .then()
        .statusCode(500)
        .body(containsString("Failed to fetch ezb file"));
  }

  @Test
  public void checkThatTimerIsDeclaredForEndpoint() throws IOException {
    JsonObject descriptor =
        new JsonObject(Files.readString(Path.of("descriptors/ModuleDescriptor-template.json")));
    JsonObject timer =
        descriptor.getJsonArray("provides").stream()
            .map(JsonObject.class::cast)
            .filter(provided -> "_timer".equals(provided.getString("id")))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No _timer interface declared"));

    assertThat(timer.getString("interfaceType")).isEqualTo("system");

    JsonArray handlers = timer.getJsonArray("handlers");
    assertThat(handlers.size()).isEqualTo(1);

    JsonObject handler = handlers.getJsonObject(0);
    assertThat(handler.getString("pathPattern")).isEqualTo(ENDPOINT);
    assertThat(handler.getJsonArray("methods").getList()).containsExactly("POST");
    assertThat(handler.getJsonObject("schedule")).isNotNull();
    assertThat(handler.getJsonObject("schedule").getString("cron")).isEqualTo("0 1 * * *");
  }

  private Future<Response> harvest(EZBService ezbService, String tenantId) {
    Promise<Response> promise = Promise.promise();
    new FincConfigEZBHarvestAPI(ezbService)
        .postFincConfigEzbHarvest(
            Map.of(RestVerticle.OKAPI_HEADER_TENANT, tenantId), promise::handle, vertxContext);
    return promise.future();
  }
}
