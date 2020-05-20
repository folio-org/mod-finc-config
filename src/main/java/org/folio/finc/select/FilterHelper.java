package org.folio.finc.select;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.List;
import java.util.stream.Collectors;
import org.folio.finc.dao.SelectFileDAO;
import org.folio.finc.dao.SelectFileDAOImpl;
import org.folio.finc.dao.SelectFilterDAO;
import org.folio.finc.dao.SelectFilterDAOImpl;
import org.folio.rest.jaxrs.model.FilterFile;
import org.folio.rest.jaxrs.model.FincSelectFilter;

public class FilterHelper {

  private static final Logger logger = LoggerFactory.getLogger(FilterHelper.class);

  private final SelectFilterDAO selectFilterDAO;
  private final SelectFileDAO selectFileDAO;

  public FilterHelper() {
    this.selectFilterDAO = new SelectFilterDAOImpl();
    this.selectFileDAO = new SelectFileDAOImpl();
  }

  public Future<Void> deleteFilesOfFilter(String filterId, String isil, Context vertxContext) {

    Promise<Void> result = Promise.promise();

    Future<FincSelectFilter> byId = selectFilterDAO.getById(filterId, isil, vertxContext);
    byId.compose(fincSelectFilter -> deleteFilesOfFilter(fincSelectFilter, isil, vertxContext))
        .setHandler(
            voidAsyncResult -> {
              if (voidAsyncResult.succeeded()) {
                result.complete();
              } else {
                result.fail(voidAsyncResult.cause());
              }
            });
    return result.future();
  }

  private Future<Void> deleteFilesOfFilter(
      FincSelectFilter filter, String isil, Context vertxContext) {

    Promise<Void> result = Promise.promise();
    List<FilterFile> filterFiles = filter.getFilterFiles();
    if (filterFiles == null || filterFiles.isEmpty()) {
      result.complete();
    } else {
      List<Future> deleteFutures =
          filterFiles.stream()
              .map(filterFile -> selectFileDAO.deleteById(filterFile.getFileId(), isil, vertxContext))
              .collect(Collectors.toList());

      CompositeFuture.all(deleteFutures)
          .setHandler(
              ar -> {
                if (ar.succeeded()) {
                  logger.info(
                      "Associated files of filter " + filter.getId() + " deleted successfully.");
                  result.complete();
                } else {
                  logger.error(
                      "Error while deleting files of filter "
                          + filter.getId()
                          + ". \n"
                          + ar.cause().getMessage());
                  result.fail(ar.cause());
                }
              });
    }
    return result.future();
  }

  public Future<FincSelectFilter> removeFilesToDelete(
      FincSelectFilter filter, String isil, Context vertxContext) {

    List<FilterFile> remainingFiles =
        filter.getFilterFiles().stream()
            .filter(filterFile -> filterFile.getDelete() == null || !filterFile.getDelete())
            .collect(Collectors.toList());

    List<Future> filesToDeleteFuture =
        filter.getFilterFiles().stream()
            .filter(filterFile -> filterFile.getDelete() != null && filterFile.getDelete())
            .map(filterFile -> selectFileDAO.deleteById(filterFile.getFileId(), isil, vertxContext))
            .collect(Collectors.toList());

    Promise<FincSelectFilter> result = Promise.promise();
    CompositeFuture.all(filesToDeleteFuture)
        .setHandler(
            ar -> {
              if (ar.succeeded()) {
                result.complete(filter.withFilterFiles(remainingFiles));
              } else {
                result.fail(ar.cause());
              }
            });
    return result.future();
  }
}
