package org.folio.finc.select;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.parsing.Parser;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;
import org.folio.finc.mocks.MockOrganization;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection.UsageRestricted;
import org.folio.rest.jaxrs.model.FincConfigMetadataSource;
import org.folio.rest.jaxrs.model.FincSelectMetadataSource;
import org.folio.rest.jaxrs.model.FincSelectMetadataSource.Selected;
import org.folio.rest.jaxrs.model.FincSelectMetadataSource.Status;
import org.folio.rest.jaxrs.model.Isil;
import org.folio.rest.jaxrs.model.MdSource;
import org.folio.rest.jaxrs.model.Organization;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.rest.utils.Constants;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class SelectMetadataSourcesIT {

  private static final String APPLICATION_JSON = "application/json";
  private static final String BASE_URI = "/finc-select/metadata-sources";
  private static final String TENANT_UBL = "ubl";
  private static final String TENANT_DIKU = "ubl";
  private static FincConfigMetadataSource metadatasourceSelected;
  private static Organization organizationUUID1234;
  private static Isil isil1;
  private static Vertx vertx;
  @Rule public Timeout timeout = Timeout.seconds(10);
  @Rule public WireMockRule wireMockRule = new WireMockRule(options().dynamicPort());

  @BeforeClass
  public static void setUp(TestContext context) {
    try {
      String isilStr = new String(Files.readAllBytes(Paths.get("ramls/examples/isil1.sample")));
      isil1 = Json.decodeValue(isilStr, Isil.class);

      String metadataSourcesStr =
          new String(
              Files.readAllBytes(Paths.get("ramls/examples/fincConfigMetadataSource.sample")));
      metadatasourceSelected = Json.decodeValue(metadataSourcesStr, FincConfigMetadataSource.class);

      organizationUUID1234 = new Organization();
      organizationUUID1234.setName("Organization Name 1234");
      organizationUUID1234.setId("uuid-1234");
    } catch (Exception e) {
      context.fail(e);
    }
    vertx = Vertx.vertx();
    try {
      PostgresClient.setIsEmbedded(true);
      PostgresClient instance = PostgresClient.getInstance(vertx);
      instance.startEmbeddedPostgres();
    } catch (Exception e) {
      context.fail(e);
      return;
    }

    Async async = context.async();
    int port = NetworkUtils.nextFreePort();

    RestAssured.reset();
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = port;
    RestAssured.defaultParser = Parser.JSON;

    String url = "http://localhost:" + port;
    TenantClient tenantClientFinc =
        new TenantClient(url, Constants.MODULE_TENANT, Constants.MODULE_TENANT);
    TenantClient tenantClientDiku = new TenantClient(url, TENANT_DIKU, TENANT_DIKU);
    DeploymentOptions options =
        new DeploymentOptions().setConfig(new JsonObject().put("http.port", port)).setWorker(true);

    vertx.deployVerticle(
        RestVerticle.class.getName(),
        options,
        res -> {
          try {
            tenantClientFinc.postTenant(null, postTenantRes -> async.complete());
            tenantClientDiku.postTenant(null, postTenantRes -> async.complete());
          } catch (Exception e) {
            context.fail(e);
          }
        });
  }

  @AfterClass
  public static void tearDown(TestContext context) {
    RestAssured.reset();
    Async async = context.async();
    vertx.close(
        context.asyncAssertSuccess(
            res -> {
              PostgresClient.stopEmbeddedPostgres();
              async.complete();
            }));
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
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .post("/finc-config/metadata-sources")
        .then()
        .statusCode(201)
        .body("id", equalTo(metadatasourceSelected.getId()))
        .body("label", equalTo(metadatasourceSelected.getLabel()))
        .body("description", equalTo(metadatasourceSelected.getDescription()));

    // POST isil
    given()
        .body(Json.encode(isil1))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .post("/finc-config/isils")
        .then()
        .statusCode(201)
        .body("isil", equalTo(isil1.getIsil()));

    // Get all metadata sources
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .get(BASE_URI)
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("fincSelectMetadataSources.size()", equalTo(1))
        .body("fincSelectMetadataSources[0].id", equalTo(metadatasourceSelected.getId()))
        .body("fincSelectMetadataSources[0].label", equalTo(metadatasourceSelected.getLabel()))
        .body("fincSelectMetadataSources[0].selected", equalTo(Selected.YES.toString()));

    // Get all metadata sources with failing query
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", ContentType.TEXT)
        .header("accept", APPLICATION_JSON)
        .get(BASE_URI + "?query=foo(/&=bar")
        .then()
        .statusCode(500);

    // Get a metadata source by id
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .get(BASE_URI + "/" + metadatasourceSelected.getId())
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("id", equalTo(metadatasourceSelected.getId()))
        .body("label", equalTo(metadatasourceSelected.getLabel()))
        .body("selected", equalTo(Selected.YES.toString()));

    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .delete("/finc-config/isils/" + isil1.getId())
        .then()
        .statusCode(204);
  }

  @Test
  public void selectAllCollectionsOfSource() throws InterruptedException {
    String mockedOkapiUrl = "http://localhost:" + wireMockRule.port();
    MockOrganization.mockOrganizationFound(organizationUUID1234);
    Isil isilUBL =
        new Isil()
            .withId(UUID.randomUUID().toString())
            .withLibrary("UBL")
            .withIsil("UBL-01")
            .withTenant(TENANT_UBL);
    Isil isilDiku =
        new Isil()
            .withId(UUID.randomUUID().toString())
            .withLibrary("DIKU")
            .withIsil("DIKU-01")
            .withTenant("diku");

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
            .withUsageRestricted(UsageRestricted.YES)
            .withSolrMegaCollections(Arrays.asList("Solr Mega Collection 01"))
            .withPermittedFor(Arrays.asList("UBL-01"))
            .withMdSource(mdSource);

    FincConfigMetadataCollection metadataCollectionRestrictedNotPermitted =
        new FincConfigMetadataCollection()
            .withId(UUID.randomUUID().toString())
            .withLabel("Solr Mega Collection restricted and not permitted")
            .withUsageRestricted(UsageRestricted.YES)
            .withSolrMegaCollections(Arrays.asList("Solr Mega Collection 01"))
            .withPermittedFor(Arrays.asList("DIKU-01"))
            .withMdSource(mdSource);

    FincConfigMetadataCollection metadataCollectionNotRestricted =
        new FincConfigMetadataCollection()
            .withId(UUID.randomUUID().toString())
            .withLabel("Solr Mega Collection not restricted")
            .withUsageRestricted(UsageRestricted.NO)
            .withSolrMegaCollections(Arrays.asList("Solr Mega Collection 01"))
            .withMdSource(mdSource);

    // POST isils
    given()
        .body(Json.encode(isilUBL))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .post("/finc-config/isils")
        .then()
        .statusCode(201)
        .body("isil", equalTo(isilUBL.getIsil()));

    given()
        .body(Json.encode(isilDiku))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .post("/finc-config/isils")
        .then()
        .statusCode(201)
        .body("isil", equalTo(isilDiku.getIsil()));

    // POST metadata source
    given()
        .body(Json.encode(metadataSource))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .post("/finc-config/metadata-sources")
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
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .post("/finc-config/metadata-collections")
        .then()
        .statusCode(201)
        .body("id", equalTo(metadataCollectionNotRestricted.getId()))
        .body("label", equalTo(metadataCollectionNotRestricted.getLabel()))
        .body("description", equalTo(metadataCollectionNotRestricted.getDescription()));

    given()
        .body(Json.encode(metadataCollectionRestrictedNotPermitted))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .post("/finc-config/metadata-collections")
        .then()
        .statusCode(201)
        .body("id", equalTo(metadataCollectionRestrictedNotPermitted.getId()))
        .body("label", equalTo(metadataCollectionRestrictedNotPermitted.getLabel()))
        .body("description", equalTo(metadataCollectionRestrictedNotPermitted.getDescription()));

    given()
        .body(Json.encode(metadataCollectionRestrictedPermitted))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("x-okapi-url", mockedOkapiUrl)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .post("/finc-config/metadata-collections")
        .then()
        .statusCode(201)
        .body("id", equalTo(metadataCollectionRestrictedPermitted.getId()))
        .body("label", equalTo(metadataCollectionRestrictedPermitted.getLabel()))
        .body("description", equalTo(metadataCollectionRestrictedPermitted.getDescription()));

    // Select all collections of metadata source
    JsonObject selectJson = new JsonObject().put("select", true);
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", APPLICATION_JSON)
        .header("accept", ContentType.TEXT)
        .body(Json.encode(selectJson))
        .put("/finc-select/metadata-sources/" + metadataSource.getId() + "/collections/select-all")
        .then()
        .statusCode(200);

    // Wait till all metadata collections have been selected
    Thread.sleep(2000);

    // Check that metadata collection are selected
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .get("/finc-select/metadata-collections" + "/" + metadataCollectionNotRestricted.getId())
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("id", equalTo(metadataCollectionNotRestricted.getId()))
        .body("label", equalTo(metadataCollectionNotRestricted.getLabel()))
        .body("selected", equalTo(Selected.NO.toString()));

    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .get(
            "/finc-select/metadata-collections"
                + "/"
                + metadataCollectionRestrictedPermitted.getId())
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("id", equalTo(metadataCollectionRestrictedPermitted.getId()))
        .body("label", equalTo(metadataCollectionRestrictedPermitted.getLabel()))
        .body("selected", equalTo(Selected.YES.toString()));

    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .get(
            "/finc-select/metadata-collections"
                + "/"
                + metadataCollectionRestrictedNotPermitted.getId())
        .then()
        .contentType(ContentType.JSON)
        .statusCode(200)
        .body("id", equalTo(metadataCollectionRestrictedNotPermitted.getId()))
        .body("label", equalTo(metadataCollectionRestrictedNotPermitted.getLabel()))
        .body("selected", equalTo(Selected.NO.toString()));

    // DELETE isil
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .delete("/finc-config/isils/" + isilUBL.getId())
        .then()
        .statusCode(204);

    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .delete("/finc-config/isils/" + isilDiku.getId())
        .then()
        .statusCode(204);

    // DELETE metadata source
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .delete("/finc-config/metadata-sources/" + metadataSource.getId())
        .then()
        .statusCode(204);

    // DELETE metadata collections
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .delete("/finc-config/metadata-collections/" + metadataCollectionNotRestricted.getId())
        .then()
        .statusCode(204);
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .delete(
            "/finc-config/metadata-collections/" + metadataCollectionRestrictedNotPermitted.getId())
        .then()
        .statusCode(204);
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .delete(
            "/finc-config/metadata-collections/" + metadataCollectionRestrictedPermitted.getId())
        .then()
        .statusCode(204);
  }

  @Test
  public void checkNotImplementedEndpoints() {
    FincSelectMetadataSource mdSource =
        new FincSelectMetadataSource()
            .withId(UUID.randomUUID().toString())
            .withLabel("Metadata Source 1")
            .withStatus(Status.DEACTIVATED)
            .withSourceId(1);
    // POST metadata source
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .body(Json.encode(mdSource))
        .post(BASE_URI)
        .then()
        .contentType(ContentType.JSON)
        .statusCode(501);

    // DELETE metadata source
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.TEXT)
        .delete(BASE_URI + "/uuid-1234")
        .then()
        .statusCode(501);

    // PUT metadata source
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", APPLICATION_JSON)
        .header("accept", ContentType.TEXT)
        .body(Json.encode(mdSource))
        .put(BASE_URI + "/uuid-1234")
        .then()
        .statusCode(501);

    // getFincSelectMetadataSourcesCollectionsById
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .get(BASE_URI + "/uuid-1234/collections")
        .then()
        .contentType(ContentType.JSON)
        .statusCode(501);

    // getFincSelectMetadataSourcesCollectionsSelectAllById
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", APPLICATION_JSON)
        .header("accept", APPLICATION_JSON)
        .get(BASE_URI + "/uuid-1234/collections/select-all")
        .then()
        .contentType(ContentType.JSON)
        .statusCode(501);

    // deleteFincSelectMetadataSourcesCollectionsSelectAllById
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.TEXT)
        .delete(BASE_URI + "/uuid-1234/collections/select-all")
        .then()
        .statusCode(501);
  }
}
