package org.folio.finc;

import io.restassured.RestAssured;
import io.restassured.parsing.Parser;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.folio.finc.config.*;
import org.folio.finc.select.*;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.rest.utils.Constants;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  ConfigMetadataCollectionsIT.class,
  ConfigMetadataSourcesIT.class,
  ConfigFiltersIT.class,
  ConfigFilesIT.class,
  ConfigContactsIT.class,
  FincSelectFilesIT.class,
  FincSelectFiltersIT.class,
  IsilsIT.class,
  SelectMetadataCollectionsIT.class,
  SelectMetadataSourcesIT.class,
  TinyMetadataSourcesIT.class,
  FilterHelperTest.class,
  ConfigMetadataCollectionsIT.class,
  ConfigMetadataSourcesIT.class,
  FincSelectFilesIT.class,
  FincSelectFiltersIT.class,
  IsilsIT.class,
  SelectMetadataCollectionsIT.class,
  SelectMetadataSourcesIT.class,
  TinyMetadataSourcesIT.class,
  ConfigEZBCredentialsIT.class,
  SelectEZBCredentialsIT.class
})
public class ApiTestSuite {

  public static final String TENANT_UBL = "ubl";
  public static final String TENANT_DIKU = "diku";

  private static Vertx vertx;
  private static boolean initialised = false;

  static {
    vertx = Vertx.vertx();
  }

  @BeforeClass
  public static void before()
      throws IOException, InterruptedException, ExecutionException, TimeoutException {

    PostgresClient.setIsEmbedded(true);
    PostgresClient.setEmbeddedPort(NetworkUtils.nextFreePort());
    PostgresClient client = PostgresClient.getInstance(vertx);
    client.startEmbeddedPostgres();

    initialised = true;
    int port = NetworkUtils.nextFreePort();

    RestAssured.reset();
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = port;
    RestAssured.defaultParser = Parser.JSON;

    DeploymentOptions options = new DeploymentOptions();
    options.setConfig(new JsonObject().put("http.port", port));

    startVerticle(options);

    prepareTenants();
  }

  @AfterClass
  public static void after() throws InterruptedException, ExecutionException, TimeoutException {

    initialised = false;
    CompletableFuture<String> undeploymentComplete = new CompletableFuture<>();

    vertx.close(
        res -> {
          if (res.succeeded()) {
            undeploymentComplete.complete(null);
          } else {
            undeploymentComplete.completeExceptionally(res.cause());
          }
        });

    undeploymentComplete.get(20, TimeUnit.SECONDS);

    PostgresClient.stopEmbeddedPostgres();
  }

  public static boolean isNotInitialised() {
    return !initialised;
  }

  private static void startVerticle(DeploymentOptions options)
      throws InterruptedException, ExecutionException, TimeoutException {

    CompletableFuture<String> deploymentComplete = new CompletableFuture<>();

    vertx.deployVerticle(
        RestVerticle.class.getName(),
        options,
        res -> {
          if (res.succeeded()) {
            deploymentComplete.complete(res.result());
          } else {
            deploymentComplete.completeExceptionally(res.cause());
          }
        });

    deploymentComplete.get(30, TimeUnit.SECONDS);
  }

  private static void prepareTenants() {

    String url = RestAssured.baseURI + ":" + RestAssured.port;
    try {
      CompletableFuture fincFuture = new CompletableFuture();
      CompletableFuture ublFuture = new CompletableFuture();
      CompletableFuture dikuFuture = new CompletableFuture();
      TenantClient tenantClientFinc =
          new TenantClient(url, Constants.MODULE_TENANT, Constants.MODULE_TENANT);
      TenantClient tenantClientDiku = new TenantClient(url, TENANT_DIKU, TENANT_DIKU);
      TenantClient tenantClientUbl = new TenantClient(url, TENANT_UBL, TENANT_UBL);
      tenantClientFinc.postTenant(
          new TenantAttributes().withModuleTo(getModuleVersion()), fincFuture::complete);
      tenantClientDiku.postTenant(
          new TenantAttributes().withModuleTo(getModuleVersion()), dikuFuture::complete);
      tenantClientUbl.postTenant(
          new TenantAttributes().withModuleTo(getModuleVersion()), ublFuture::complete);
      CompletableFuture.allOf(fincFuture, dikuFuture, ublFuture).get();
    } catch (Exception e) {
      assert false;
    }
  }

  public static String getModuleVersion() throws IOException, XmlPullParserException {
    Model pom = new MavenXpp3Reader().read(new FileReader("pom.xml"));
    return String.format("%s-%s", pom.getArtifactId(), pom.getVersion());
  }
}
