package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.finc.dao.FileDAO;
import org.folio.finc.dao.FileDAOImpl;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.resource.FincConfigFiles;

public class FincConfigFilesAPI extends FincFileHandler implements FincConfigFiles {

  private final FileDAO fileDAO;

  public FincConfigFilesAPI() {
    this.fileDAO = new FileDAOImpl();
  }

  @Override
  @Validate
  public void getFincConfigFilesById(String id, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    fileDAO.getById(id, vertxContext)
        .onComplete(
            ar ->
                handleAsyncFileReponse(
                    ar,
                    GetFincConfigFilesByIdResponse::respond200WithApplicationOctetStream,
                    GetFincConfigFilesByIdResponse::respond404WithTextPlain,
                    GetFincConfigFilesByIdResponse::respond500WithTextPlain,
                    asyncResultHandler)
        );
  }
}
