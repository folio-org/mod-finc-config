package org.folio.finc.periodic;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.folio.finc.dao.SelectFileDAO;
import org.folio.finc.dao.SelectFileDAOImpl;
import org.folio.finc.dao.SelectFilterDAO;
import org.folio.finc.dao.SelectFilterDAOImpl;
import org.folio.finc.model.File;
import org.folio.rest.jaxrs.model.FilterFile;
import org.folio.rest.jaxrs.model.FincSelectFilter;
import org.folio.rest.jaxrs.model.FincSelectFilters;

public class EZBHarvestVerticle extends AbstractVerticle {

  private static final Logger log = LoggerFactory.getLogger(EZBHarvestVerticle.class);
  public static final String LABEL_EZB_FILE = "EZB file";
  public static final String LABEL_EZB_HOLDINGS = "EZB holdings";
  private final SelectFilterDAO selectFilterDAO = new SelectFilterDAOImpl();
  private final SelectFileDAO selectFileDAO = new SelectFileDAOImpl();

  @Override
  public void start() {
    String user = config().getString("user");
    String password = config().getString("password");
    String libId = config().getString("libId");
    String isil = config().getString("isil");

    Future<String> ezbFileFuture = fetchFileFromEZB(user, password, libId);
    Future<FincSelectFilter> dbFilterFuture = fetchFilterFromDB(isil);
    CompositeFuture.all(ezbFileFuture, dbFilterFuture)
        .onComplete(compositeFuture -> {
          if (compositeFuture.succeeded()) {
            updateEZBFileOfFilter(dbFilterFuture.result(), ezbFileFuture.result(), isil);
          } else {
            log.error(String.format("Error while updating ezb file for isil %s: %s", isil,
                compositeFuture.cause()));
          }
        });
  }

  private void updateEZBFileOfFilter(FincSelectFilter filter, String ezbFileString, String isil) {
    if (filter == null) {
      // we have no filter, so we do not need to update its file
      return;
    }

    if (filter.getFilterFiles().isEmpty() || !hasEZBFile(filter)) {
      insertFileAndUpdateFilter(ezbFileString, filter, isil);
    } else {
      // compare old and new filter
      calcFilesToDelete(ezbFileString, filter, isil)
          .compose(filesToDelete ->
              deleteFiles(filter, filesToDelete, isil)
          )
          .compose(updatedFilterFiles -> {
                if (updatedFilterFiles.size() != filter.getFilterFiles().size()) {
                  // Update file and filter only if changed
                  filter.setFilterFiles(updatedFilterFiles);
                  return insertFileAndUpdateFilter(ezbFileString, filter, isil);
                } else {
                  return Future.succeededFuture();
                }
              }
          );

    }
  }

  private Future insertFileAndUpdateFilter(String ezbFile, FincSelectFilter filter, String isil) {
    Promise<Void> result = Promise.promise();
    insertEZBFile(ezbFile, isil)
        .compose(file -> {
          FilterFile ff = new FilterFile().withFileId(file.getId())
              .withLabel(LABEL_EZB_FILE);
          filter.getFilterFiles().add(ff);
          return updateFilter(filter)
              .onComplete(ar -> {
                if (ar.succeeded()) {
                  log.info(String
                      .format("Successfully executed ezb updater for %s", isil));
                  result.complete();
                } else {
                  log.error(String
                      .format("Failed executing ezb updater for %s: %s", isil,
                          ar.cause()));
                  result.fail(ar.cause());
                }
              });
        });
    return result.future();
  }

  private Future<String> fetchFileFromEZB(String user, String password, String libId) {
    Promise<String> result = Promise.promise();
    WebClient client = WebClient.create(vertx);
    String url = String.format(
        "https://rzbezb2.ur.de/ezb/export/licenselist_html.php?pack=0&bibid=%s&lang=de&output_style=kbart&todo_license=ALkbart",
        libId);
    HttpRequest<Buffer> get = client.getAbs(
        url)
        .basicAuthentication(user, password);
    get.send(ar -> {
      if (ar.succeeded()) {
        HttpResponse<Buffer> response = ar.result();
        if (ar.result().statusCode() == 200) {
          result.complete(response.bodyAsString());
        } else {
          result.fail(
              String
                  .format(
                      "Failed to fetch ezb file. Http status code: %s. Status message: %s. %s ",
                      response.statusCode(), response.statusMessage(), response.bodyAsString()));
        }
      } else {
        result.fail("Failed to fetch ezb file. " + ar.cause());
      }
    });
    return result.future();
  }

  private Future<FincSelectFilter> fetchFilterFromDB(String isil) {
    Promise<FincSelectFilter> result = Promise.promise();
    selectFilterDAO
        .getAll("label=\"" + LABEL_EZB_HOLDINGS + "\"", 0, 1, isil, vertx.getOrCreateContext())
        .onComplete(ar -> {
          if (ar.succeeded()) {
            FincSelectFilters fincSelectFilters = ar.result();
            List<FincSelectFilter> filters = fincSelectFilters.getFincSelectFilters();
            if (filters.isEmpty()) {
              result.complete();
            } else {
              result.complete(filters.get(0));
            }
          } else {
            result.fail("Failed to fetch filter from DB: " + ar.cause());
          }
        });
    return result.future();
  }

  private boolean hasEZBFile(FincSelectFilter filter) {
    return filter.getFilterFiles().stream()
        .anyMatch(filterFile -> LABEL_EZB_FILE.equals(filterFile.getLabel()));
  }

  private Future<File> fetchFileFromDB(String id, String isil) {
    Promise<File> result = Promise.promise();
    selectFileDAO.getById(id, isil, vertx.getOrCreateContext())
        .onComplete(ar -> {
          if (ar.succeeded()) {
            result.complete(ar.result());
          } else {
            result.fail("Failed to fetch file from DB: " + ar.cause());
          }
        });
    return result.future();
  }

  private Future<List<String>> calcFilesToDelete(String ezbFile, FincSelectFilter filter,
      String isil) {
    Promise<List<String>> result = Promise.promise();
    List<FilterFile> filterFiles = filter.getFilterFiles();
    List<String> fileIds = filterFiles.stream()
        .filter(filterFile -> LABEL_EZB_FILE.equals(filterFile.getLabel()))
        .map(FilterFile::getFileId).collect(
            Collectors.toList());
    if (fileIds.size() > 1) {
      result.complete(fileIds);
    } else {
      fetchFileFromDB(fileIds.get(0), isil)
          .onComplete(ar -> {
            if (ar.succeeded()) {
              File file = ar.result();
              if (file == null) {
                log.info("Will update ezb file. Old file not found.");
                result.complete(Collections.singletonList(fileIds.get(0)));
              } else {
                String fromDBAsBase64 = file.getData();
                String ezbAsBase64 = Base64.getEncoder().encodeToString(ezbFile.getBytes());
                if (fromDBAsBase64.equals(ezbAsBase64)) {
                  log.info("Will not update ezb file. Content of new and old file is equal.");
                  result.complete(Collections.emptyList());
                } else {
                  log.info("Will update ezb file. Content of new and old file is not equal.");
                  result.complete(Collections.singletonList(file.getId()));
                }
              }
            } else {
              result.fail(ar.cause());
            }
          });
    }
    return result.future();
  }

  private Future<List<FilterFile>> deleteFiles(FincSelectFilter filter, List<String> fileIds,
      String isil) {
    Promise<List<FilterFile>> result = Promise.promise();

    List<Future> futures = fileIds.stream().map(id ->
        selectFileDAO.deleteById(id, isil, vertx.getOrCreateContext())
    ).collect(Collectors.toList());

    CompositeFuture.all(futures).onComplete(ar -> {
      if (ar.succeeded()) {
        // delete filter files with matching ids
        List<FilterFile> filteredFileIds = filter.getFilterFiles().stream()
            .filter(filterFile -> !fileIds.contains(filterFile.getFileId()))
            .collect(Collectors.toList());
        // filter.setFilterFiles(filteredFileIds);
        result.complete(filteredFileIds);
      } else {
        result.fail(ar.cause());
      }
    });
    return result.future();
  }

  private Future<File> insertEZBFile(String ezbFile, String isil) {
    String base64Data = Base64.getEncoder().encodeToString(ezbFile.getBytes());
    String uuid = UUID.randomUUID().toString();
    File file = new File().withData(base64Data).withId(uuid).withIsil(isil);
    return selectFileDAO.upsert(file, uuid, vertx.getOrCreateContext());
  }

  private Future<FincSelectFilter> updateFilter(FincSelectFilter filter) {
    filter.getMetadata().setUpdatedDate(new Date());
    return selectFilterDAO.update(filter, filter.getId(), vertx.getOrCreateContext());
  }

}
