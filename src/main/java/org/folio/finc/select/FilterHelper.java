package org.folio.finc.select;

import io.vertx.core.Context;
import io.vertx.core.Future;
import org.folio.finc.dao.FileDAO;
import org.folio.finc.dao.FileDAOImpl;
import org.folio.finc.dao.FilterFileDAO;
import org.folio.finc.dao.FilterFileDAOImpl;
import org.folio.rest.jaxrs.model.FincSelectFilterFile;

public class FilterHelper {

  private FilterFileDAO filterFileDAO;
  private FileDAO fileDAO;

  public FilterHelper() {
    this.filterFileDAO = new FilterFileDAOImpl();
    this.fileDAO = new FileDAOImpl();
  }

  public Future<Integer> deleteFileOfFilterFile(
      String filterFileId, String isil, Context vertxContext) {
    return filterFileDAO
        .getById(filterFileId, isil, vertxContext)
        .compose(
            fincSelectFilterFile -> {
              if (fincSelectFilterFile == null) {
                return Future.succeededFuture(0);
              } else {
                return fileDAO.deleteById(fincSelectFilterFile.getFile(), isil, vertxContext);
              }
            });
  }

  public Future<Integer> deleteFilterFileOfFile(String fileId, String isil, Context vertxContext) {
    String query = "(file=" + fileId + ")";
    return filterFileDAO
        .getAll(query, 0, 1, isil, vertxContext)
        .compose(
            fincSelectFilterFiles -> {
              if (fincSelectFilterFiles.getFincSelectFilterFiles().isEmpty()) {
                return Future.succeededFuture(0);
              } else {
                FincSelectFilterFile fincSelectFilterFile =
                    fincSelectFilterFiles.getFincSelectFilterFiles().get(0);
                String id = fincSelectFilterFile.getId();
                return filterFileDAO.deleteById(id, isil, vertxContext);
              }
            });
  }
}
