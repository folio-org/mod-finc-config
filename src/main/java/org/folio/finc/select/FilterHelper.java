package org.folio.finc.select;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.finc.dao.SelectFileDAO;
import org.folio.finc.dao.SelectFileDAOImpl;
import org.folio.finc.dao.SelectFilterDAO;
import org.folio.finc.dao.SelectFilterDAOImpl;
import org.folio.rest.jaxrs.model.FilterFile;
import org.folio.rest.jaxrs.model.FincSelectFilter;
import org.folio.rest.persist.PgExceptionUtil;

public class FilterHelper {

  private static final Logger logger = LogManager.getLogger(FilterHelper.class);

  private final SelectFilterDAO selectFilterDAO;
  private final SelectFileDAO selectFileDAO;

  public FilterHelper() {
    this.selectFilterDAO = new SelectFilterDAOImpl();
    this.selectFileDAO = new SelectFileDAOImpl();
  }

  public Future<Void> deleteFilesOfFilter(String filterId, String isil, Context vertxContext) {

    Promise<Void> result = Promise.promise();

    Future<FincSelectFilter> byId = selectFilterDAO.getById(filterId, isil, vertxContext);
    byId.onSuccess(
        fincSelectFilter -> {
          if (fincSelectFilter == null) {
            result.fail("Cannot find filter with id " + filterId);
          } else {
            deleteFilesOfFilter(fincSelectFilter, isil, vertxContext)
                .onComplete(
                    voidAsyncResult -> {
                      if (voidAsyncResult.succeeded()) {
                        result.complete();
                      } else {
                        result.fail(voidAsyncResult.cause());
                      }
                    });
          }
        });
    byId.onFailure(t -> result.fail("Cannot delete files of filter with id " + t));
    return result.future();
  }

  private Future<Void> deleteFilesOfFilter(
      FincSelectFilter filter, String isil, Context vertxContext) {

    Promise<Void> result = Promise.promise();
    List<FilterFile> filterFiles = filter.getFilterFiles();
    if (filterFiles == null || filterFiles.isEmpty()) {
      result.complete();
    } else {
      List<Future<Integer>> deleteFutures =
          filterFiles.stream()
              .map(
                  filterFile ->
                      selectFileDAO.deleteById(filterFile.getFileId(), isil, vertxContext))
              .collect(Collectors.toList());

      Future.all(deleteFutures)
          .onComplete(
              ar -> {
                if (ar.succeeded()) {
                  logger.info(
                      String.format(
                          "Associated files of filter %s deleted successfully.", filter.getId()));
                  result.complete();
                } else {
                  logger.error(
                      String.format(
                          "Error while deleting files of filter %s. %n %s",
                          filter.getId(), PgExceptionUtil.getMessage(ar.cause())));
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

    List<Future<Integer>> filesToDeleteFuture =
        filter.getFilterFiles().stream()
            .filter(filterFile -> filterFile.getDelete() != null && filterFile.getDelete())
            .map(filterFile -> selectFileDAO.deleteById(filterFile.getFileId(), isil, vertxContext))
            .collect(Collectors.toList());

    Promise<FincSelectFilter> result = Promise.promise();
    Future.all(filesToDeleteFuture)
        .onComplete(
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
