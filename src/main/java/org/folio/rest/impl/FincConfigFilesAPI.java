package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.folio.finc.dao.FileDAO;
import org.folio.finc.dao.FileDAOImpl;
import org.folio.finc.model.File;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.resource.FincConfigFiles;
import org.folio.rest.jaxrs.resource.FincSelectFiles.PostFincSelectFilesResponse;
import org.folio.rest.utils.Constants;

public class FincConfigFilesAPI extends FincFileHandler implements FincConfigFiles {

  public static final String X_OKAPI_TENANT = "x-okapi-tenant";

  private final FileDAO fileDAO;

  public FincConfigFilesAPI() {
    this.fileDAO = new FileDAOImpl();
  }

  @Override
  @Validate
  public void postFincConfigFiles(String isil, InputStream entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    if (isil == null) {
      asyncResultHandler.handle(Future.succeededFuture(PostFincConfigFilesResponse
          .respond400WithTextPlain("Isil must be set as query parameter.")));
      return;
    }

    byte[] bytes = new byte[0];
    try {
      bytes = IOUtils.toByteArray(entity);
    } catch (IOException e) {
      asyncResultHandler.handle(
          Future.succeededFuture(
              PostFincSelectFilesResponse.respond404WithTextPlain("Cannot read file")));
    }
    String base64Data = Base64.getEncoder().encodeToString(bytes);
    String uuid = UUID.randomUUID().toString();
    File file = new File().withId(uuid).withIsil(isil).withData(base64Data);
    okapiHeaders.put(X_OKAPI_TENANT, Constants.MODULE_TENANT);
    fileDAO.upsert(file, uuid, vertxContext)
        .onSuccess(f ->
            asyncResultHandler.handle(
                Future.succeededFuture(PostFincConfigFilesResponse.respond200WithTextPlain(uuid)))
        )
        .onFailure(throwable ->
            asyncResultHandler.handle(Future.succeededFuture(PostFincConfigFilesResponse
                .respond500WithTextPlain("Cannot insert file. " + throwable)))
        );
  }

  @Override
  @Validate
  public void getFincConfigFilesById(String id, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    fileDAO.getById(id, vertxContext)
        .onComplete(
            ar ->
                handleAsyncFileResponse(
                    ar,
                    GetFincConfigFilesByIdResponse::respond200WithApplicationOctetStream,
                    GetFincConfigFilesByIdResponse::respond404WithTextPlain,
                    GetFincConfigFilesByIdResponse::respond500WithTextPlain,
                    asyncResultHandler)
        );
  }

  @Override
  @Validate
  public void deleteFincConfigFilesById(String id, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    okapiHeaders.put(X_OKAPI_TENANT, Constants.MODULE_TENANT);
    fileDAO.deleteById(id, vertxContext)
        .onSuccess(integer ->
            asyncResultHandler
                .handle(Future.succeededFuture(DeleteFincConfigFilesByIdResponse.respond204()))
        )
        .onFailure(throwable -> asyncResultHandler.handle(Future.succeededFuture(
            DeleteFincConfigFilesByIdResponse.respond500WithTextPlain(throwable))));
  }
}
