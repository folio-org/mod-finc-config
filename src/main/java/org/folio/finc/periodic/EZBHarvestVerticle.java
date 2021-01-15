package org.folio.finc.periodic;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.finc.dao.SelectFileDAO;
import org.folio.finc.dao.SelectFileDAOImpl;
import org.folio.finc.dao.SelectFilterDAO;
import org.folio.finc.dao.SelectFilterDAOImpl;
import org.folio.finc.model.File;
import org.folio.finc.periodic.ezb.EZBService;
import org.folio.okapi.common.GenericCompositeFuture;
import org.folio.rest.jaxrs.model.FilterFile;
import org.folio.rest.jaxrs.model.FincSelectFilter;
import org.folio.rest.jaxrs.model.FincSelectFilters;
import org.folio.rest.jaxrs.model.Metadata;

import java.util.*;
import java.util.stream.Collectors;

/** A {@link io.vertx.core.Verticle} to fetch EZB holding files for a single tenant. */
public class EZBHarvestVerticle extends AbstractVerticle {

  public static final String LABEL_EZB_FILE = "EZB file";
  public static final String LABEL_EZB_HOLDINGS = "EZB holdings";
  private static final Logger log = LogManager.getLogger(EZBHarvestVerticle.class);
  private final SelectFilterDAO selectFilterDAO = new SelectFilterDAOImpl();
  private final SelectFileDAO selectFileDAO = new SelectFileDAOImpl();
  private final EZBService ezbService;

  public EZBHarvestVerticle(EZBService ezbService) {
    super();
    this.ezbService = ezbService;
  }

  @Override
  public void start(Promise<Void> startFuture) {
    String user = config().getString("user");
    String password = config().getString("password");
    String libId = config().getString("libId");
    String isil = config().getString("isil");

    log.info("Will start ezb harvester verticle for isil {}", isil);

    Future<String> ezbFileFuture = ezbService.fetchEZBFile(user, password, libId, vertx);
    Future<FincSelectFilter> dbFilterFuture = fetchFilterFromDB(isil);
    GenericCompositeFuture.all(Arrays.asList(ezbFileFuture, dbFilterFuture))
        .compose(
            compositeFuture ->
                updateEZBFileOfFilter(dbFilterFuture.result(), ezbFileFuture.result(), isil))
        .onComplete(
            ar -> {
              if (ar.succeeded()) {
                log.info("Successfully executed ezb updater for {}", isil);
                startFuture.complete();
              } else {
                log.error("Error while updating ezb file for isil {}: {}", isil, ar.cause());
                startFuture.fail(ar.cause());
              }
            });
  }

  /**
   * Fetches ezb holding filter from database
   *
   * @param isil Isil of current tenant
   * @return
   */
  private Future<FincSelectFilter> fetchFilterFromDB(String isil) {
    Promise<FincSelectFilter> result = Promise.promise();
    selectFilterDAO
        .getAll("label==\"" + LABEL_EZB_HOLDINGS + "\"", 0, 1, isil, vertx.getOrCreateContext())
        .onComplete(
            ar -> {
              if (ar.succeeded()) {
                FincSelectFilters fincSelectFilters = ar.result();
                List<FincSelectFilter> filters = fincSelectFilters.getFincSelectFilters();
                if (filters.isEmpty()) {
                  result.complete();
                } else {
                  result.complete(filters.get(0));
                }
              } else {
                result.fail(String.format("Failed to fetch filter from DB: %s", ar.cause()));
              }
            });
    return result.future();
  }

  /**
   * Updates given ezb holding file for the specified filter. Update only happens if content of
   * given ezb file is not equal to content of ezb file saved in the database.
   *
   * @param filter The filter
   * @param ezbFileString The ezb holding file
   * @param isil Isil of current tenant
   * @return
   */
  private Future<Void> updateEZBFileOfFilter(
      FincSelectFilter filter, String ezbFileString, String isil) {
    if (filter == null) {
      // we have no filter, so we do not need to update its file
      log.info("No ezb filter found for isil {}. Will do nothing.", isil);
      return Future.succeededFuture();
    }

    if (filter.getFilterFiles().isEmpty() || !hasEZBFile(filter)) {
      return insertFileAndUpdateFilter(ezbFileString, filter, isil);
    } else {
      // compare old and new filter
      return calcFilesToRemove(ezbFileString, filter, isil)
          .compose(filesToDelete -> removeFiles(filter, filesToDelete, isil))
          .compose(
              updatedFilterFiles -> {
                if (shouldUpdateFilter(filter, updatedFilterFiles)) {
                  // Update file and filter only if changed
                  filter.setFilterFiles(updatedFilterFiles);
                  return insertFileAndUpdateFilter(ezbFileString, filter, isil);
                } else {
                  return Future.succeededFuture();
                }
              });
    }
  }

  /**
   * Inserts given ezbFile into the database, and adds it to the given {@link FincSelectFilter}.
   *
   * @param ezbFile Given ezb file
   * @param filter The filter
   * @param isil Isil of current tenant
   * @return
   */
  private Future<Void> insertFileAndUpdateFilter(
      String ezbFile, FincSelectFilter filter, String isil) {
    Promise<Void> result = Promise.promise();
    insertEZBFile(ezbFile, isil)
        .compose(
            file -> {
              FilterFile ff = new FilterFile().withFileId(file.getId()).withLabel(LABEL_EZB_FILE);
              filter.getFilterFiles().add(ff);
              return updateFilter(filter)
                  .onComplete(
                      ar -> {
                        if (ar.succeeded()) {
                          result.complete();
                        } else {
                          result.fail(ar.cause());
                        }
                      });
            });
    return result.future();
  }

  /**
   * Inserts the given ezb file as base64 into the database
   *
   * @param ezbFile The ezb file
   * @param isil Isil of current tenant
   * @return
   */
  private Future<File> insertEZBFile(String ezbFile, String isil) {
    String base64Data = Base64.getEncoder().encodeToString(ezbFile.getBytes());
    String uuid = UUID.randomUUID().toString();
    File file = new File().withData(base64Data).withId(uuid).withIsil(isil);
    return selectFileDAO.upsert(file, uuid, vertx.getOrCreateContext());
  }

  /**
   * Updates the given {@link FincSelectFilter} in the database
   *
   * @param filter The filter
   * @return
   */
  private Future<FincSelectFilter> updateFilter(FincSelectFilter filter) {
    Date date = new Date();
    if (filter.getMetadata() == null) {
      Metadata md = new Metadata().withCreatedDate(date).withUpdatedDate(date);
      filter.setMetadata(md);
    } else {
      filter.getMetadata().setUpdatedDate(date);
    }
    return selectFilterDAO.update(filter, filter.getId(), vertx.getOrCreateContext());
  }

  /**
   * Calculates a list of file ids that shall be removed from the given {@link FincSelectFilter}. If
   * given filter has more than one ezb file, all ezb files shall be removed. Else the ezb file
   * shall be removed if content from database differs from actual content from ezb server.
   *
   * @param ezbFile Content of actual ezb file
   * @param filter The filter
   * @param isil Isil of current tenant
   * @return
   */
  private Future<List<String>> calcFilesToRemove(
      String ezbFile, FincSelectFilter filter, String isil) {
    Promise<List<String>> result = Promise.promise();
    List<FilterFile> filterFiles = filter.getFilterFiles();
    List<String> ezbFileIds =
        filterFiles.stream()
            .filter(filterFile -> LABEL_EZB_FILE.equals(filterFile.getLabel()))
            .map(FilterFile::getFileId)
            .collect(Collectors.toList());
    if (ezbFileIds.size() > 1) {
      // if there is more than one file named "EZB file" we need to delete all of them
      result.complete(ezbFileIds);
    } else {
      String ezbFileIdInDB = ezbFileIds.get(0);
      fetchFileFromDB(ezbFileIdInDB, isil)
          .onComplete(
              ar -> {
                if (ar.succeeded()) {
                  File fileFromDB = ar.result();
                  result.complete(calcFileIdsToDelete(ezbFile, ezbFileIdInDB, fileFromDB));
                } else {
                  result.fail(ar.cause());
                }
              });
    }
    return result.future();
  }

  private List<String> calcFileIdsToDelete(
      String ezbFileContent, String ezbFileIdInDB, File fileFromDB) {
    if (fileFromDB == null) {
      log.info("Will update ezb file. Old file not found.");
      return Collections.singletonList(ezbFileIdInDB);
    } else {
      String fromDBAsBase64 = fileFromDB.getData();
      String ezbAsBase64 = Base64.getEncoder().encodeToString(ezbFileContent.getBytes());
      if (fromDBAsBase64.equals(ezbAsBase64)) {
        log.info("Will not update ezb file. Content of new and old file is equal.");
        return Collections.emptyList();
      } else {
        log.info("Will update ezb file. Content of new and old file is not equal.");
        return Collections.singletonList(fileFromDB.getId());
      }
    }
  }

  /**
   * Removes files with given fileIds from database and from filter files of given {@link
   * FincSelectFilter}
   *
   * @param filter The filter
   * @param fileIds Ids of files that will be removed
   * @param isil Isil of current tenant
   * @return
   */
  private Future<List<FilterFile>> removeFiles(
      FincSelectFilter filter, List<String> fileIds, String isil) {
    Promise<List<FilterFile>> result = Promise.promise();

    List<Future> deleteFileFutures =
        fileIds.stream()
            .map(id -> selectFileDAO.deleteById(id, isil, vertx.getOrCreateContext()))
            .collect(Collectors.toList());

    GenericCompositeFuture.all(deleteFileFutures)
        .onComplete(
            ar -> {
              if (ar.succeeded()) {
                // delete filter files with matching ids from filter
                List<FilterFile> filteredFileIds =
                    filter.getFilterFiles().stream()
                        .filter(filterFile -> !fileIds.contains(filterFile.getFileId()))
                        .collect(Collectors.toList());
                result.complete(filteredFileIds);
              } else {
                result.fail(ar.cause());
              }
            });
    return result.future();
  }

  /**
   * Determines if the given {@link FincSelectFilter} shall be updated with given {@link
   * FilterFile}s.
   *
   * @param filter The filter
   * @param updatedFilterFiles List of updated {@link FilterFile}s
   * @return
   */
  private boolean shouldUpdateFilter(FincSelectFilter filter, List<FilterFile> updatedFilterFiles) {
    return updatedFilterFiles.size() != filter.getFilterFiles().size();
  }

  /**
   * Determines if {@link FincSelectFilter} has at least one ezb file in its filter files
   *
   * @param filter The filter
   * @return
   */
  private boolean hasEZBFile(FincSelectFilter filter) {
    return filter.getFilterFiles().stream()
        .anyMatch(filterFile -> LABEL_EZB_FILE.equals(filterFile.getLabel()));
  }

  /**
   * Fetches a {@link File} from database
   *
   * @param id Id of the file
   * @param isil Isil of current tenant
   * @return
   */
  private Future<File> fetchFileFromDB(String id, String isil) {
    Promise<File> result = Promise.promise();
    selectFileDAO
        .getById(id, isil, vertx.getOrCreateContext())
        .onComplete(
            ar -> {
              if (ar.succeeded()) {
                result.complete(ar.result());
              } else {
                result.fail(String.format("Failed to fetch file from DB: %s", ar.cause()));
              }
            });
    return result.future();
  }
}
