package org.folio.finc.config;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.Arrays;
import java.util.UUID;
import org.folio.finc.ApiTestBase;
import org.folio.rest.jaxrs.model.Contact;
import org.folio.rest.jaxrs.model.Contact.Role;
import org.folio.rest.jaxrs.model.Contact.Type;
import org.folio.rest.jaxrs.model.FincConfigMetadataSource;
import org.folio.rest.jaxrs.model.FincConfigMetadataSource.SolrShard;
import org.folio.rest.jaxrs.model.Organization;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public abstract class AbstractMetadataSourcesIT extends ApiTestBase {

  @Rule public Timeout timeout = Timeout.seconds(10);
  @Rule public WireMockRule wireMockRule = new WireMockRule(options().dynamicPort());
  FincConfigMetadataSource metadataSource1;
  FincConfigMetadataSource metadataSource2;
  FincConfigMetadataSource metadataSource2Changed;
  Organization organizationUUID1234;
  Organization organizationUUID1235;

  @Before
  public void init() {
    Contact c1 =
        new Contact()
            .withExternalId(UUID.randomUUID().toString())
            .withName("Doe, Jane")
            .withRole(Role.LIBRARIAN)
            .withType(Type.USER);
    Contact c2 =
        new Contact()
            .withExternalId(UUID.randomUUID().toString())
            .withName("Eod, John")
            .withRole(Role.VENDOR)
            .withType(Type.CONTACT);
    metadataSource1 =
        new FincConfigMetadataSource()
            .withId(UUID.randomUUID().toString())
            .withLabel("First Metadata Source Sample")
            .withDescription("This is a metadata source for tests")
            .withStatus(FincConfigMetadataSource.Status.ACTIVE)
            .withSolrShard(SolrShard.UBL_MAIN)
            .withSourceId(1)
            .withAccessUrl("http://access.url")
            .withContacts(Arrays.asList(c1, c2));

    metadataSource2 =
        new FincConfigMetadataSource()
            .withId(UUID.randomUUID().toString())
            .withLabel("Second Metadata Source Sample")
            .withDescription("This is a second metadata source for tests")
            .withStatus(FincConfigMetadataSource.Status.ACTIVE)
            .withSourceId(2)
            .withAccessUrl("http://access.url");

    metadataSource2Changed = metadataSource2.withAccessUrl("www.changed.org");

    organizationUUID1234 =
        new Organization().withName("Organization Name 1234").withId("uuid-1234");
    organizationUUID1235 =
        new Organization().withName("Organization Name 1235").withId("uuid-1235");
  }
}
