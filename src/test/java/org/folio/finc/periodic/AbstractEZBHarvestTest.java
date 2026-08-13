package org.folio.finc.periodic;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;

import freemarker.template.TemplateException;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.folio.ApiTestBase;
import org.folio.TestUtils;
import org.folio.finc.dao.FileDAOImpl;
import org.folio.finc.dao.SelectFileDAO;
import org.folio.finc.dao.SelectFileDAOImpl;
import org.folio.finc.dao.SelectFilterDAO;
import org.folio.finc.dao.SelectFilterDAOImpl;
import org.folio.finc.model.File;
import org.folio.rest.impl.TenantAPI;
import org.folio.rest.jaxrs.model.Credential;
import org.folio.rest.jaxrs.model.FilterFile;
import org.folio.rest.jaxrs.model.FincSelectFilter;
import org.folio.rest.jaxrs.model.FincSelectFilter.Type;
import org.folio.rest.jaxrs.model.FincSelectFilters;
import org.folio.rest.jaxrs.model.Isil;
import org.folio.rest.jaxrs.model.Metadata;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.PostgresClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public abstract class AbstractEZBHarvestTest extends ApiTestBase {

  private static final String QUERY = "label==\"EZB holdings\"";
  protected static Vertx vertx = TestUtils.getVertx();
  protected static Context vertxContext = vertx.getOrCreateContext();
  protected static final String TENANT = "finc";
  protected static final String ISIL = "DE-15";
  protected static String filterId = null;

  @Rule public Timeout timeout = Timeout.seconds(10);

  @Before
  public void before(TestContext context) {
    createSchema()
        .compose(v -> insertIsil())
        .compose(v -> insertFilter().onSuccess(s -> filterId = s))
        .compose(s -> insertCredential())
        .onComplete(context.asyncAssertSuccess());
  }

  @After
  public void after(TestContext context) {
    dropSchema().onComplete(context.asyncAssertSuccess());
    PostgresClient.closeAllClients(TENANT);
  }

  protected Future<FincSelectFilters> getEZBFilter() {
    return getEZBFilter(ISIL);
  }

  protected Future<FincSelectFilters> getEZBFilter(String isil) {
    return new SelectFilterDAOImpl().getAll(QUERY, 0, 1, isil, vertxContext);
  }

  protected Future<String> getUpdatedEZBFile() {
    return getEZBFilter()
        .flatMap(
            fsf -> {
              List<FincSelectFilter> filters = fsf.getFincSelectFilters();
              if (filters.size() != 1) {
                return failedFuture(
                    String.format(
                        "Expected exactly 1 EZB holdings filter, but found %s", filters.size()));
              } else {
                return succeededFuture(
                    filters.get(0).getFilterFiles().stream()
                        .filter(filterFile -> filterFile.getLabel().equals("EZB file"))
                        .toList());
              }
            })
        .flatMap(
            ezbFiles -> {
              if (ezbFiles.size() != 1) {
                return failedFuture(
                    String.format(
                        "Expected exactly 1 EZB holdings file, but found %s", ezbFiles.size()));
              } else {
                return succeededFuture(ezbFiles.get(0).getFileId());
              }
            })
        .flatMap(this::getFile);
  }

  private Future<String> getFile(String fileId) {
    return new FileDAOImpl()
        .getById(fileId, vertxContext)
        .map(
            file -> {
              String actualAsBase64 = file.getData();
              byte[] bytes = Base64.getDecoder().decode(actualAsBase64);
              return new String(bytes, StandardCharsets.UTF_8);
            });
  }

  protected Future<Void> createSchema() {
    String[] sqlFile;
    try {
      sqlFile = new TenantAPI().sqlFile(TENANT, false, null, null, null);
    } catch (IOException | TemplateException e) {
      return failedFuture(e);
    }
    return PostgresClient.getInstance(vertx).runSqlFile(String.join("\n", sqlFile));
  }

  protected Future<Void> dropSchema() {
    String[] sqlFile;
    try {
      sqlFile =
          new TenantAPI()
              .sqlFile(TENANT, false, new TenantAttributes().withPurge(true), null, null);
    } catch (IOException | TemplateException e) {
      return failedFuture(e);
    }
    return PostgresClient.getInstance(vertx).runSqlFile(String.join("\n", sqlFile));
  }

  protected Future<String> insertIsil() {
    return insertIsil(ISIL, TENANT);
  }

  protected Future<String> insertIsil(String isil, String tenantOfIsil) {
    Isil entity =
        new Isil()
            .withId(UUID.randomUUID().toString())
            .withIsil(isil)
            .withTenant(tenantOfIsil)
            .withLibrary(tenantOfIsil);
    return PostgresClient.getInstance(vertx, TENANT).save("isils", entity);
  }

  protected Future<String> insertFilter() {
    return insertFilter(ISIL);
  }

  protected Future<String> insertFilter(String isil) {
    FincSelectFilter filter =
        new FincSelectFilter().withLabel("EZB holdings").withType(Type.WHITELIST).withIsil(isil);
    return PostgresClient.getInstance(vertx, TENANT).save("filters", filter);
  }

  protected Future<String> insertCredential() {
    return insertCredential(ISIL);
  }

  protected Future<String> insertCredential(String isil) {
    Credential credential =
        new Credential()
            .withUser("user")
            .withPassword("password")
            .withLibId("libId")
            .withIsil(isil);
    return PostgresClient.getInstance(vertx, TENANT).save("ezb_credentials", credential);
  }

  protected Future<FincSelectFilter> insertEZBFile(String content, String fileId, Date date) {
    SelectFileDAO fileDAO = new SelectFileDAOImpl();
    SelectFilterDAO filterDAO = new SelectFilterDAOImpl();

    File file =
        new File()
            .withData(Base64.getEncoder().encodeToString(content.getBytes()))
            .withIsil(ISIL)
            .withId(fileId);
    return fileDAO
        .upsert(file, fileId, vertxContext)
        .flatMap(f -> filterDAO.getAll(QUERY, 0, 1, ISIL, vertxContext))
        .flatMap(
            filters -> {
              FincSelectFilter filter = filters.getFincSelectFilters().get(0);
              FilterFile ff =
                  new FilterFile()
                      .withFileId(fileId)
                      .withFilename("EZB file")
                      .withLabel("EZB file")
                      .withId(UUID.randomUUID().toString());
              filter.getFilterFiles().add(ff);
              Metadata md = new Metadata().withUpdatedDate(date).withCreatedDate(date);
              filter.setMetadata(md);
              return filterDAO.update(filter, filter.getId(), vertxContext);
            });
  }

  protected Future<Metadata> getMetadataOfFilter() {
    return new SelectFilterDAOImpl()
        .getAll(QUERY, 0, 1, ISIL, vertxContext)
        .flatMap(
            fsf -> {
              List<FincSelectFilter> filters = fsf.getFincSelectFilters();
              if (filters.size() != 1) {
                return failedFuture(
                    String.format(
                        "Expected exactly 1 EZB holdings filter, but found %s", filters.size()));
              } else {
                return succeededFuture(filters.get(0).getMetadata());
              }
            });
  }
}
