package org.folio.finc.mocks;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.givenThat;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import io.vertx.core.json.Json;
import org.folio.rest.jaxrs.model.Organization;

public class MockOrganization {

  private static final String ORGANIZATION_URL = "/organizations-storage/organizations/";

  public static void mockOrganizationFound(Organization organization) {
    String orgaId = organization.getId();
    String orgaUrl = ORGANIZATION_URL + orgaId;
    givenThat(
        get(urlPathEqualTo(orgaUrl))
            .willReturn(
                aResponse()
                    .withHeader("Content-type", "application/json")
                    .withBody(Json.encode(organization))
                    .withStatus(200)));
  }
}
