package org.folio.finc.periodic;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;

import freemarker.template.TemplateException;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.folio.finc.dao.FileDAOImpl;
import org.folio.finc.dao.SelectFileDAO;
import org.folio.finc.dao.SelectFileDAOImpl;
import org.folio.finc.dao.SelectFilterDAO;
import org.folio.finc.dao.SelectFilterDAOImpl;
import org.folio.finc.model.File;
import org.folio.postgres.testing.PostgresTesterContainer;
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
import org.folio.rest.tools.utils.VertxUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;

public abstract class AbstractEZBHarvestJobTest {

  private static final String QUERY = "label==\"EZB holdings\"";
  static Vertx vertx = VertxUtils.getVertxFromContextOrNew();
  static Context vertxContext = vertx.getOrCreateContext();
  static final String tenant = "finc";
  static String filterId = null;

  @Rule public Timeout timeout = Timeout.seconds(10);

  @BeforeClass
  public static void beforeClass() {
    PostgresClient.setPostgresTester(new PostgresTesterContainer());
  }

  @Before
  public void before(TestContext context) {
    createSchema()
        .compose(s -> insertIsil())
        .compose(v -> insertFilter().onSuccess(s -> filterId = s))
        .compose(s -> insertCredential())
        .onComplete(context.asyncAssertSuccess());
  }

  @After
  public void after(TestContext context) {
    dropSchema().onComplete(context.asyncAssertSuccess());
    PostgresClient.closeAllClients(tenant);
  }

  protected Future<FincSelectFilters> getEZBFilter() {
    return new SelectFilterDAOImpl()
        .getAll(QUERY, 0, 1, EZBHarvestJobWithFilterITest.tenant, vertxContext);
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
                        .collect(Collectors.toList()));
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

  protected Future<List<String>> createSchema() {
    String[] sqlFile;
    try {
      sqlFile = new TenantAPI().sqlFile(tenant, false, null, null);
    } catch (IOException | TemplateException e) {
      return failedFuture(e);
    }
    return PostgresClient.getInstance(vertx).runSQLFile(String.join("\n", sqlFile), true);
  }

  protected Future<List<String>> dropSchema() {
    String[] sqlFile;
    try {
      sqlFile =
          new TenantAPI().sqlFile(tenant, false, new TenantAttributes().withPurge(true), null);
    } catch (IOException | TemplateException e) {
      return failedFuture(e);
    }
    return PostgresClient.getInstance(vertx).runSQLFile(String.join("\n", sqlFile), true);
  }

  protected Future<String> insertIsil() {
    Isil isil =
        new Isil()
            .withId(UUID.randomUUID().toString())
            .withIsil(tenant)
            .withTenant(tenant)
            .withLibrary(tenant);
    return PostgresClient.getInstance(vertx, tenant).save("isils", isil);
  }

  protected Future<String> insertFilter() {
    FincSelectFilter filter =
        new FincSelectFilter().withLabel("EZB holdings").withType(Type.WHITELIST).withIsil(tenant);
    return PostgresClient.getInstance(vertx, tenant).save("filters", filter);
  }

  protected Future<String> insertCredential() {
    Credential credential =
        new Credential()
            .withUser("user")
            .withPassword("password")
            .withLibId("libId")
            .withIsil(tenant);
    return PostgresClient.getInstance(vertx, tenant).save("ezb_credentials", credential);
  }

  protected Future<FincSelectFilter> insertEZBFile(String content, String fileId, Date date) {
    SelectFileDAO fileDAO = new SelectFileDAOImpl();
    SelectFilterDAO filterDAO = new SelectFilterDAOImpl();

    File file =
        new File()
            .withData(Base64.getEncoder().encodeToString(content.getBytes()))
            .withIsil(tenant)
            .withId(fileId);
    return fileDAO
        .upsert(file, fileId, vertxContext)
        .flatMap(f -> filterDAO.getAll(QUERY, 0, 1, tenant, vertxContext))
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
        .getAll(QUERY, 0, 1, tenant, vertxContext)
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
