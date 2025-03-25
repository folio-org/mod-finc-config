package org.folio.finc.select;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.restassured.http.ContentType;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.Arrays;
import java.util.UUID;
import org.folio.ApiTestBase;
import org.folio.TestUtils;
import org.folio.finc.mocks.MockOrganization;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection.UsageRestricted;
import org.folio.rest.jaxrs.model.FincConfigMetadataSource;
import org.folio.rest.jaxrs.model.FincSelectMetadataCollection;
import org.folio.rest.jaxrs.model.FincSelectMetadataSource;
import org.folio.rest.jaxrs.model.FincSelectMetadataSource.Selected;
import org.folio.rest.jaxrs.model.Isil;
import org.folio.rest.jaxrs.model.MdSource;
import org.folio.rest.jaxrs.model.Organization;
import org.folio.rest.jaxrs.model.SelectedBy;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class SelectMetadataSourcesIT extends ApiTestBase {

  @Rule public Timeout timeout = Timeout.seconds(10);
  @Rule public WireMockRule wireMockRule = new WireMockRule(options().dynamicPort());
  private FincConfigMetadataSource metadatasourceSelected;
  private Organization organizationUUID1234;
  private Isil isilUBL;
  private Isil isilDiku;

  @BeforeClass
  public static void beforeClass() throws Exception {
    TestUtils.setupTenants();
  }

  @AfterClass
  public static void afterClass() throws Exception {
    TestUtils.teardownTenants();
  }

  @Before
  public void init() {
    isilUBL = loadIsilUbl();
    isilDiku = loadIsilDiku();
    organizationUUID1234 =
        new Organization().withName("Organization Name 1234").withId("uuid-1234");
    metadatasourceSelected =
        new FincConfigMetadataSource()
            .withId(UUID.randomUUID().toString())
            .withLabel("Metadata Source Sample")
            .withDescription("This is a metadata source for tests")
            .withStatus(FincConfigMetadataSource.Status.ACTIVE)
            .withSourceId(1)
            .withSelectedBy(
                Arrays.asList(
                    new SelectedBy().withIsil("DE-15").withSelected(SelectedBy.Selected.ALL)));
  }

  @After
  public void tearDown() {
    deleteIsil(isilUBL.getId());
    deleteIsil(isilDiku.getId());
  }

  @Test
  public void checkThatWeCanSelectAndUnselect() {
    String mockedOkapiUrl = "http://localhost:" + wireMockRule.port();
    MockOrganization.mockOrganizationFound(organizationUUID1234);

    // POST
    given()
        .body(Json.encode(metadatasourceSelected))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .post(FINC_CONFIG_METADATA_SOURCES_ENDPOINT)
        .then()
        .statusCode(201)
        .body("id", equalTo(metadatasourceSelected.getId()))
        .body("label", equalTo(metadatasourceSelected.getLabel()))
        .body("description", equalTo(metadatasourceSelected.getDescription()));

    // Get all metadata sources
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_SELECT_METADATA_SOURCES_ENDPOINT)
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("fincSelectMetadataSources.size()", equalTo(1))
        .body("fincSelectMetadataSources[0].id", equalTo(metadatasourceSelected.getId()))
        .body("fincSelectMetadataSources[0].label", equalTo(metadatasourceSelected.getLabel()))
        .body("fincSelectMetadataSources[0].selected", equalTo(Selected.ALL.toString()));

    // Get all metadata sources with failing query
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", ContentType.TEXT)
        .header("accept", ContentType.JSON)
        .get(FINC_SELECT_METADATA_SOURCES_ENDPOINT + "?query=foo(/&=bar")
        .then()
        .statusCode(500);

    // Get a metadata source by id
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_SELECT_METADATA_SOURCES_ENDPOINT + "/" + metadatasourceSelected.getId())
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("id", equalTo(metadatasourceSelected.getId()))
        .body("label", equalTo(metadatasourceSelected.getLabel()))
        .body("selected", equalTo(Selected.ALL.toString()));

    // Get a metadata source by id not found
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", ContentType.TEXT)
        .header("accept", ContentType.JSON)
        .get(FINC_SELECT_METADATA_SOURCES_ENDPOINT + "/" + UUID.randomUUID().toString())
        .then()
        .statusCode(404);

    // DELETE
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.TEXT)
        .delete(FINC_CONFIG_METADATA_SOURCES_ENDPOINT + "/" + metadatasourceSelected.getId())
        .then()
        .statusCode(204);
  }

  @Test
  public void selectAllCollectionsOfSource() throws InterruptedException {
    String mockedOkapiUrl = "http://localhost:" + wireMockRule.port();
    MockOrganization.mockOrganizationFound(organizationUUID1234);

    FincConfigMetadataSource metadataSource =
        new FincConfigMetadataSource()
            .withId(UUID.randomUUID().toString())
            .withLabel("Metadata Source")
            .withStatus(FincConfigMetadataSource.Status.ACTIVE)
            .withSourceId(1);
    MdSource mdSource = new MdSource().withId(metadataSource.getId());

    FincConfigMetadataCollection metadataCollectionRestrictedPermitted =
        new FincConfigMetadataCollection()
            .withId(UUID.randomUUID().toString())
            .withLabel("Solr Mega Collection restricted and permitted")
            .withCollectionId("collID3")
            .withUsageRestricted(UsageRestricted.YES)
            .withSolrMegaCollections(Arrays.asList("Solr Mega Collection 01"))
            .withPermittedFor(Arrays.asList(isilUBL.getIsil()))
            .withMdSource(mdSource);

    FincConfigMetadataCollection metadataCollectionRestrictedNotPermitted =
        new FincConfigMetadataCollection()
            .withId(UUID.randomUUID().toString())
            .withLabel("Solr Mega Collection restricted and not permitted")
            .withCollectionId("collID2")
            .withUsageRestricted(UsageRestricted.YES)
            .withSolrMegaCollections(Arrays.asList("Solr Mega Collection 01"))
            .withPermittedFor(Arrays.asList(isilDiku.getIsil()))
            .withMdSource(mdSource);

    FincConfigMetadataCollection metadataCollectionNotRestricted =
        new FincConfigMetadataCollection()
            .withId(UUID.randomUUID().toString())
            .withLabel("Solr Mega Collection not restricted")
            .withCollectionId("collID1")
            .withUsageRestricted(UsageRestricted.NO)
            .withSolrMegaCollections(Arrays.asList("Solr Mega Collection 01"))
            .withMdSource(mdSource);

    // POST metadata source
    given()
        .body(Json.encode(metadataSource))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .post(FINC_CONFIG_METADATA_SOURCES_ENDPOINT)
        .then()
        .statusCode(201)
        .body("id", equalTo(metadataSource.getId()))
        .body("label", equalTo(metadataSource.getLabel()))
        .body("description", equalTo(metadataSource.getDescription()));

    // POST metadata collections
    given()
        .body(Json.encode(metadataCollectionNotRestricted))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .post(FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT)
        .then()
        .statusCode(201)
        .body("id", equalTo(metadataCollectionNotRestricted.getId()))
        .body("label", equalTo(metadataCollectionNotRestricted.getLabel()))
        .body("description", equalTo(metadataCollectionNotRestricted.getDescription()));

    given()
        .body(Json.encode(metadataCollectionRestrictedNotPermitted))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .post(FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT)
        .then()
        .statusCode(201)
        .body("id", equalTo(metadataCollectionRestrictedNotPermitted.getId()))
        .body("label", equalTo(metadataCollectionRestrictedNotPermitted.getLabel()))
        .body("description", equalTo(metadataCollectionRestrictedNotPermitted.getDescription()));

    given()
        .body(Json.encode(metadataCollectionRestrictedPermitted))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .post(FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT)
        .then()
        .statusCode(201)
        .body("id", equalTo(metadataCollectionRestrictedPermitted.getId()))
        .body("label", equalTo(metadataCollectionRestrictedPermitted.getLabel()))
        .body("description", equalTo(metadataCollectionRestrictedPermitted.getDescription()));

    // Select all collections of metadata source
    JsonObject selectJson = new JsonObject().put("select", true);
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.TEXT)
        .body(Json.encode(selectJson))
        .put(
            FINC_SELECT_METADATA_SOURCES_ENDPOINT
                + "/"
                + metadataSource.getId()
                + "/collections/select-all")
        .then()
        .statusCode(200);

    // Wait till all metadata collections have been selected
    Thread.sleep(2000);

    // Check that metadata collection are selected
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(
            FINC_SELECT_METADATA_COLLECTIONS_ENDPOINT
                + "/"
                + metadataCollectionNotRestricted.getId())
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("id", equalTo(metadataCollectionNotRestricted.getId()))
        .body("label", equalTo(metadataCollectionNotRestricted.getLabel()))
        .body("selected", equalTo(FincSelectMetadataCollection.Selected.YES.value()));

    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(
            FINC_SELECT_METADATA_COLLECTIONS_ENDPOINT
                + "/"
                + metadataCollectionRestrictedPermitted.getId())
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("id", equalTo(metadataCollectionRestrictedPermitted.getId()))
        .body("label", equalTo(metadataCollectionRestrictedPermitted.getLabel()))
        .body("selected", equalTo(FincSelectMetadataCollection.Selected.YES.value()));

    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(
            FINC_SELECT_METADATA_COLLECTIONS_ENDPOINT
                + "/"
                + metadataCollectionRestrictedNotPermitted.getId())
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("id", equalTo(metadataCollectionRestrictedNotPermitted.getId()))
        .body("label", equalTo(metadataCollectionRestrictedNotPermitted.getLabel()))
        .body("selected", equalTo(FincSelectMetadataCollection.Selected.NO.value()));

    // Check that selected=some in MetadataSource for tenant ubl
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_SELECT_METADATA_SOURCES_ENDPOINT + "/" + metadataSource.getId())
        .andReturn()
        .then()
        .statusCode(200)
        .body("id", equalTo(metadataSource.getId()))
        .body("label", equalTo(metadataSource.getLabel()))
        .body("selected", equalTo(FincSelectMetadataSource.Selected.SOME.value()));

    // Check that selected=none in MetadataSource for tenant diku
    given()
        .header("X-Okapi-Tenant", TENANT_DIKU)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .get(FINC_SELECT_METADATA_SOURCES_ENDPOINT + "/" + metadataSource.getId())
        .andReturn()
        .then()
        .statusCode(200)
        .body("id", equalTo(metadataSource.getId()))
        .body("label", equalTo(metadataSource.getLabel()))
        .body("selected", equalTo(Selected.NONE.value()));

    // DELETE metadata source
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .delete(FINC_CONFIG_METADATA_SOURCES_ENDPOINT + "/" + metadataSource.getId())
        .then()
        .statusCode(204);

    // DELETE metadata collections
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .delete(
            FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT
                + "/"
                + metadataCollectionNotRestricted.getId())
        .then()
        .statusCode(204);
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .delete(
            FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT
                + "/"
                + metadataCollectionRestrictedNotPermitted.getId())
        .then()
        .statusCode(204);
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .delete(
            FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT
                + "/"
                + metadataCollectionRestrictedPermitted.getId())
        .then()
        .statusCode(204);
  }
}
