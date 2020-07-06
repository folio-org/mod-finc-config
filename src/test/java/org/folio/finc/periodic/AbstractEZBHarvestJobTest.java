package org.folio.finc.periodic;


import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.junit.Timeout;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.folio.finc.dao.FileDAO;
import org.folio.finc.dao.FileDAOImpl;
import org.folio.finc.dao.SelectFilterDAO;
import org.folio.finc.dao.SelectFilterDAOImpl;
import org.folio.rest.impl.TenantAPI;
import org.folio.rest.jaxrs.model.FilterFile;
import org.folio.rest.jaxrs.model.FincSelectFilter;
import org.folio.rest.jaxrs.model.FincSelectFilters;
import org.folio.rest.jaxrs.model.Isil;
import org.folio.rest.persist.PostgresClient;
import org.junit.Rule;

public abstract class AbstractEZBHarvestJobTest {

  static Vertx vertx;
  static Context vertxContext;
  static final String tenant = "finc";

  @Rule
  public Timeout timeout = Timeout.seconds(10);

  protected Future<FincSelectFilters> getEZBFilter() {
    SelectFilterDAO selectFilterDAO = new SelectFilterDAOImpl();
    return selectFilterDAO
        .getAll("label==\"EZB holdings\"", 0, 1, EZBHarvestJobWithFilterITest.tenant,
            vertx.getOrCreateContext());
  }

  protected Future<String> getUpdatedEZBFile() {
    Promise<String> result = Promise.promise();
    getEZBFilter()
        .onComplete(filterAr -> {
          if (filterAr.succeeded()) {
            List<FincSelectFilter> filters = filterAr.result().getFincSelectFilters();
            if (filters.size() != 1) {
              result.fail(String
                  .format("Expected exactly 1 EZB holdings filter, but found %s", filters.size()));
            } else {
              List<FilterFile> filterFiles = filters.get(0).getFilterFiles();
              List<FilterFile> ezbFiles = filterFiles.stream()
                  .filter(filterFile -> filterFile.getLabel().equals("EZB file")).collect(
                      Collectors.toList());
              if (ezbFiles.size() != 1) {
                result.fail(String
                    .format("Expected exactly 1 EZB holdings file, but found %s", ezbFiles.size()));
              } else {
                String fileId = ezbFiles.get(0).getFileId();
                getFile(fileId)
                    .onComplete(fileAr -> {
                      if (fileAr.succeeded()) {
                        result.complete(fileAr.result());
                      } else {
                        result.fail(fileAr.cause());
                      }
                    });
              }
            }
          } else {
            result.fail(filterAr.cause());
          }
        });
    return result.future();
  }

  private Future<String> getFile(String fileId) {
    Promise<String> result = Promise.promise();
    FileDAO fileDAO = new FileDAOImpl();
    fileDAO.getById(fileId, vertx.getOrCreateContext())
        .onComplete(ar -> {
          if (ar.succeeded()) {
            String actualAsBase64 = ar.result().getData();
            byte[] bytes = Base64.getDecoder().decode(actualAsBase64);
            String s = new String(bytes, StandardCharsets.UTF_8);
            result.complete(s);
          } else {
            result.fail(ar.cause());
          }
        });
    return result.future();
  }

  protected Future<List<String>> createSchema(String tenant) {
    Promise<List<String>> createSchema = Promise.promise();
    try {
      String sqlFile = new TenantAPI().sqlFile(tenant, false, null, null);
      PostgresClient.getInstance(vertx)
          .runSQLFile(
              sqlFile,
              true,
              ar -> {
                if (ar.succeeded()) {
                  if (ar.result().size() == 0) {
                    createSchema.complete(ar.result());
                  } else {
                    createSchema.fail(tenant + ": " + ar.result().get(0));
                  }
                } else {
                  createSchema.fail(ar.cause());
                }
              });
    } catch (Exception e) {
      createSchema.fail(e);
    }
    return createSchema.future();
  }

  protected Future<Void> insertIsil(String tenant) {
    Promise<Void> result = Promise.promise();
    Isil isil = new Isil()
        .withId(UUID.randomUUID().toString())
        .withIsil(tenant)
        .withTenant(tenant)
        .withLibrary(tenant);
    PostgresClient.getInstance(vertx, tenant)
        .save("isils", isil, ar -> {
          if (ar.succeeded()) {
            result.complete();
          } else {
            result.fail(ar.cause());
          }
        });
    return result.future();
  }

}
