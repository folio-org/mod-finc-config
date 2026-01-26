package org.folio.rest.utils.nameresolver;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.HashMap;
import java.util.Map;
import org.folio.finc.mocks.MockOrganization;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.jaxrs.model.Organization;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@WireMockTest
@ExtendWith(VertxExtension.class)
class ITOrganizationNameResolver {

  private static final String ORGANIZATION_ID = "org-uuid-12345";
  private static final String ORGANIZATION_NAME = "Test Organization";
  private static final String TENANT_ID = "test-tenant";

  @Test
  void nullOrganizationIdReturnsNull(
      Vertx vertx, VertxTestContext testContext, WireMockRuntimeInfo wmRuntimeInfo) {
    Map<String, String> okapiHeaders = createOkapiHeaders(wmRuntimeInfo);

    OrganizationNameResolver.resolveName(null, okapiHeaders, vertx.getOrCreateContext())
        .onSuccess(result -> assertThat(result).isNull())
        .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  void emptyOrganizationIdReturnsNull(
      Vertx vertx, VertxTestContext testContext, WireMockRuntimeInfo wmRuntimeInfo) {
    MockOrganization.mockOrganizationNotFound("");

    Map<String, String> okapiHeaders = createOkapiHeaders(wmRuntimeInfo);

    OrganizationNameResolver.resolveName("", okapiHeaders, vertx.getOrCreateContext())
        .onSuccess(result -> assertThat(result).isNull())
        .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  void successfulResponseReturnsOrganizationName(
      Vertx vertx, VertxTestContext testContext, WireMockRuntimeInfo wmRuntimeInfo) {
    Organization organization =
        new Organization().withId(ORGANIZATION_ID).withName(ORGANIZATION_NAME);
    MockOrganization.mockOrganizationFound(organization);

    Map<String, String> okapiHeaders = createOkapiHeaders(wmRuntimeInfo);

    OrganizationNameResolver.resolveName(ORGANIZATION_ID, okapiHeaders, vertx.getOrCreateContext())
        .onSuccess(result -> assertThat(result).isEqualTo(ORGANIZATION_NAME))
        .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  void non200ResponseReturnsNull(
      Vertx vertx, VertxTestContext testContext, WireMockRuntimeInfo wmRuntimeInfo) {
    MockOrganization.mockOrganizationNotFound(ORGANIZATION_ID);

    Map<String, String> okapiHeaders = createOkapiHeaders(wmRuntimeInfo);

    OrganizationNameResolver.resolveName(ORGANIZATION_ID, okapiHeaders, vertx.getOrCreateContext())
        .onSuccess(result -> assertThat(result).isNull())
        .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  void okapiHeadersAreForwarded(
      Vertx vertx, VertxTestContext testContext, WireMockRuntimeInfo wmRuntimeInfo) {
    Organization organization =
        new Organization().withId(ORGANIZATION_ID).withName(ORGANIZATION_NAME);
    MockOrganization.mockOrganizationFound(organization);

    String requestId = "request-id-12345";
    String token = "test-token";

    Map<String, String> okapiHeaders = createOkapiHeaders(wmRuntimeInfo);
    okapiHeaders.put(XOkapiHeaders.REQUEST_ID, requestId);
    okapiHeaders.put(XOkapiHeaders.TOKEN, token);

    OrganizationNameResolver.resolveName(ORGANIZATION_ID, okapiHeaders, vertx.getOrCreateContext())
        .onSuccess(
            result -> {
              assertThat(result).isEqualTo(ORGANIZATION_NAME);
              wmRuntimeInfo
                  .getWireMock()
                  .verifyThat(
                      getRequestedFor(
                              urlPathEqualTo(
                                  "/organizations-storage/organizations/" + ORGANIZATION_ID))
                          .withHeader(XOkapiHeaders.TENANT, equalTo(TENANT_ID))
                          .withHeader(XOkapiHeaders.REQUEST_ID, equalTo(requestId))
                          .withHeader(XOkapiHeaders.TOKEN, equalTo(token))
                          .withHeader("accept", equalTo("application/json")));
            })
        .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  void urlConstructionPrefersOkapiUrlTo(
      Vertx vertx, VertxTestContext testContext, WireMockRuntimeInfo wmRuntimeInfo) {
    Organization organization =
        new Organization().withId(ORGANIZATION_ID).withName(ORGANIZATION_NAME);
    MockOrganization.mockOrganizationFound(organization);

    Map<String, String> okapiHeaders =
        Map.of(
            XOkapiHeaders.URL_TO,
            wmRuntimeInfo.getHttpBaseUrl(),
            XOkapiHeaders.URL,
            "http://should-not-be-used:9999",
            XOkapiHeaders.TENANT,
            TENANT_ID);

    OrganizationNameResolver.resolveName(ORGANIZATION_ID, okapiHeaders, vertx.getOrCreateContext())
        .onSuccess(
            result -> {
              assertThat(result).isEqualTo(ORGANIZATION_NAME);
              wmRuntimeInfo
                  .getWireMock()
                  .verifyThat(
                      1,
                      getRequestedFor(
                          urlPathEqualTo(
                              "/organizations-storage/organizations/" + ORGANIZATION_ID)));
            })
        .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  void urlConstructionFallsBackToOkapiUrl(
      Vertx vertx, VertxTestContext testContext, WireMockRuntimeInfo wmRuntimeInfo) {
    Organization organization =
        new Organization().withId(ORGANIZATION_ID).withName(ORGANIZATION_NAME);
    MockOrganization.mockOrganizationFound(organization);

    Map<String, String> okapiHeaders = createOkapiHeaders(wmRuntimeInfo);

    OrganizationNameResolver.resolveName(ORGANIZATION_ID, okapiHeaders, vertx.getOrCreateContext())
        .onSuccess(
            result -> {
              assertThat(result).isEqualTo(ORGANIZATION_NAME);
              wmRuntimeInfo
                  .getWireMock()
                  .verifyThat(
                      1,
                      getRequestedFor(
                          urlPathEqualTo(
                              "/organizations-storage/organizations/" + ORGANIZATION_ID)));
            })
        .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  void serverErrorReturnsNull(
      Vertx vertx, VertxTestContext testContext, WireMockRuntimeInfo wmRuntimeInfo) {
    MockOrganization.mockOrganizationServerError(ORGANIZATION_ID);

    Map<String, String> okapiHeaders = createOkapiHeaders(wmRuntimeInfo);

    OrganizationNameResolver.resolveName(ORGANIZATION_ID, okapiHeaders, vertx.getOrCreateContext())
        .onSuccess(result -> assertThat(result).isNull())
        .onComplete(testContext.succeedingThenComplete());
  }

  private static Map<String, String> createOkapiHeaders(WireMockRuntimeInfo wmRuntimeInfo) {
    Map<String, String> headers = new HashMap<>();
    headers.put(XOkapiHeaders.URL, wmRuntimeInfo.getHttpBaseUrl());
    headers.put(XOkapiHeaders.TENANT, TENANT_ID);
    return headers;
  }
}
