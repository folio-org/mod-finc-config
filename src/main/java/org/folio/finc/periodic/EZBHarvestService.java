package org.folio.finc.periodic;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.finc.dao.SelectFileDAO;
import org.folio.finc.dao.SelectFileDAOImpl;
import org.folio.finc.dao.SelectFilterDAO;
import org.folio.finc.dao.SelectFilterDAOImpl;
import org.folio.finc.model.File;
import org.folio.finc.periodic.ezb.EZBService;
import org.folio.rest.jaxrs.model.Credential;
import org.folio.rest.jaxrs.model.FilterFile;
import org.folio.rest.jaxrs.model.FincSelectFilter;
import org.folio.rest.jaxrs.model.Metadata;

/** Service to fetch EZB holding files for a single tenant. */
public class EZBHarvestService {

  public static final String LABEL_EZB_FILE = "EZB file";
  public static final String LABEL_EZB_HOLDINGS = "EZB holdings";
  private static final Logger log = LogManager.getLogger(EZBHarvestService.class);
  private final SelectFilterDAO selectFilterDAO = new SelectFilterDAOImpl();
  private final SelectFileDAO selectFileDAO = new SelectFileDAOImpl();
  private final EZBService ezbService;

  public EZBHarvestService(EZBService ezbService) {
    this.ezbService = ezbService;
  }

  /**
   * Harvests EZB holding files for a single tenant.
   *
   * @param credential EZB credential for the tenant
   * @param context Vert.x context
   * @return Future that completes when harvest is done
   */
  public Future<Void> harvest(Credential credential, Context context) {
    String isil = credential.getIsil();
    log.info("Starting ezb harvest for isil {}", isil);

    Vertx vertx = context.owner();
    Future<String> ezbFileFuture =
        ezbService.fetchEZBFile(
            credential.getUser(), credential.getPassword(), credential.getLibId(), vertx);
    Future<FincSelectFilter> dbFilterFuture = fetchFilterFromDB(isil, context);

    return Future.all(Arrays.asList(ezbFileFuture, dbFilterFuture))
        .compose(
            compositeFuture ->
                updateEZBFileOfFilter(
                    dbFilterFuture.result(), ezbFileFuture.result(), isil, context))
        .onSuccess(v -> log.info("Successfully executed ezb updater for {}", isil))
        .onFailure(err -> log.error("Error while updating ezb file for isil {}", isil, err));
  }

  /**
   * Fetches ezb holding filter from database
   *
   * @param isil Isil of current tenant
   * @param context Vert.x context
   * @return
   */
  private Future<FincSelectFilter> fetchFilterFromDB(String isil, Context context) {
    return selectFilterDAO
        .getAll("label==\"" + LABEL_EZB_HOLDINGS + "\"", 0, 1, isil, context)
        .onFailure(err -> log.error("Cannot fetch filter from DB for isil {}", isil, err))
        .map(
            fincSelectFilters -> {
              List<FincSelectFilter> filters = fincSelectFilters.getFincSelectFilters();
              if (filters.isEmpty()) {
                return null;
              } else {
                return filters.get(0);
              }
            });
  }

  /**
   * Updates given ezb holding file for the specified filter. Update only happens if content of
   * given ezb file is not equal to content of ezb file saved in the database.
   *
   * @param filter The filter
   * @param ezbFileString The ezb holding file
   * @param isil Isil of current tenant
   * @param context Vert.x context
   * @return
   */
  private Future<Void> updateEZBFileOfFilter(
      FincSelectFilter filter, String ezbFileString, String isil, Context context) {
    if (filter == null) {
      // we have no filter, so we do not need to update its file
      log.info("No ezb filter found for isil {}. Will do nothing.", isil);
      return Future.succeededFuture();
    }

    if (filter.getFilterFiles().isEmpty() || !hasEZBFile(filter)) {
      return insertFileAndUpdateFilter(ezbFileString, filter, isil, context);
    } else {
      // compare old and new filter
      return calcFilesToRemove(ezbFileString, filter, isil, context)
          .compose(filesToDelete -> removeFiles(filter, filesToDelete, isil, context))
          .compose(
              updatedFilterFiles -> {
                if (shouldUpdateFilter(filter, updatedFilterFiles)) {
                  // Update file and filter only if changed
                  filter.setFilterFiles(updatedFilterFiles);
                  return insertFileAndUpdateFilter(ezbFileString, filter, isil, context);
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
   * @param context Vert.x context
   * @return
   */
  private Future<Void> insertFileAndUpdateFilter(
      String ezbFile, FincSelectFilter filter, String isil, Context context) {
    return insertEZBFile(ezbFile, isil, context)
        .compose(
            file -> {
              FilterFile ff = new FilterFile().withFileId(file.getId()).withLabel(LABEL_EZB_FILE);
              filter.getFilterFiles().add(ff);
              return updateFilter(filter, context).mapEmpty();
            });
  }

  /**
   * Inserts the given ezb file as base64 into the database
   *
   * @param ezbFile The ezb file
   * @param isil Isil of current tenant
   * @param context Vert.x context
   * @return
   */
  private Future<File> insertEZBFile(String ezbFile, String isil, Context context) {
    String base64Data = Base64.getEncoder().encodeToString(ezbFile.getBytes());
    String uuid = UUID.randomUUID().toString();
    File file = new File().withData(base64Data).withId(uuid).withIsil(isil);
    return selectFileDAO
        .upsert(file, uuid, context)
        .onFailure(err -> log.error("Cannot insert EZB file for isil {}", isil, err));
  }

  /**
   * Updates the given {@link FincSelectFilter} in the database
   *
   * @param filter The filter
   * @param context Vert.x context
   * @return
   */
  private Future<FincSelectFilter> updateFilter(FincSelectFilter filter, Context context) {
    Date date = new Date();
    if (filter.getMetadata() == null) {
      Metadata md = new Metadata().withCreatedDate(date).withUpdatedDate(date);
      filter.setMetadata(md);
    } else {
      filter.getMetadata().setUpdatedDate(date);
    }
    return selectFilterDAO
        .update(filter, filter.getId(), context)
        .onFailure(err -> log.error("Cannot update filter {}", filter.getId(), err));
  }

  /**
   * Calculates a list of file ids that shall be removed from the given {@link FincSelectFilter}. If
   * given filter has more than one ezb file, all ezb files shall be removed. Else the ezb file
   * shall be removed if content from database differs from actual content from ezb server.
   *
   * @param ezbFile Content of actual ezb file
   * @param filter The filter
   * @param isil Isil of current tenant
   * @param context Vert.x context
   * @return
   */
  private Future<List<String>> calcFilesToRemove(
      String ezbFile, FincSelectFilter filter, String isil, Context context) {
    List<FilterFile> filterFiles = filter.getFilterFiles();
    List<String> ezbFileIds =
        filterFiles.stream()
            .filter(filterFile -> LABEL_EZB_FILE.equals(filterFile.getLabel()))
            .map(FilterFile::getFileId)
            .toList();
    if (ezbFileIds.size() > 1) {
      // if there is more than one file named "EZB file" we need to delete all of them
      return Future.succeededFuture(ezbFileIds);
    } else {
      String ezbFileIdInDB = ezbFileIds.get(0);
      return fetchFileFromDB(ezbFileIdInDB, isil, context)
          .map(fileFromDB -> calcFileIdsToDelete(ezbFile, ezbFileIdInDB, fileFromDB));
    }
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
   * @param context Vert.x context
   * @return
   */
  private Future<List<FilterFile>> removeFiles(
      FincSelectFilter filter, List<String> fileIds, String isil, Context context) {

    List<Future<Integer>> deleteFileFutures =
        fileIds.stream()
            .map(
                id ->
                    selectFileDAO
                        .deleteById(id, isil, context)
                        .onFailure(
                            err -> log.error("Cannot delete file {} for isil {}", id, isil, err)))
            .toList();

    return Future.all(deleteFileFutures)
        .map(
            // delete filter files with matching ids from filter, return mutable list
            cf ->
                new ArrayList<>(
                    filter.getFilterFiles().stream()
                        .filter(filterFile -> !fileIds.contains(filterFile.getFileId()))
                        .toList()));
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
   * @param context Vert.x context
   * @return
   */
  private Future<File> fetchFileFromDB(String id, String isil, Context context) {
    return selectFileDAO
        .getById(id, isil, context)
        .onFailure(err -> log.error("Cannot fetch file {} from DB for isil {}", id, isil, err));
  }
}
