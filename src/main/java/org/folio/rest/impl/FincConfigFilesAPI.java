package org.folio.rest.impl;

import static org.folio.rest.utils.Constants.*;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;
import javax.ws.rs.core.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.finc.dao.FileDAO;
import org.folio.finc.dao.FileDAOImpl;
import org.folio.finc.model.File;
import org.folio.rest.annotations.Stream;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.resource.FincConfigFiles;
import org.folio.rest.utils.Constants;

/** Manages files for ui-finc-config */
public class FincConfigFilesAPI extends FincFileHandler implements FincConfigFiles {
  private static final Logger log = LogManager.getLogger(FincConfigFilesAPI.class);

  public static final String X_OKAPI_TENANT = "x-okapi-tenant";

  private final FileDAO fileDAO;

  public FincConfigFilesAPI() {
    this.fileDAO = new FileDAOImpl();
  }

  @Override
  @Validate
  @Stream
  public void postFincConfigFiles(
      String isil,
      InputStream entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    if (isil == null) {
      asyncResultHandler.handle(
          Future.succeededFuture(
              PostFincConfigFilesResponse.respond400WithTextPlain(
                  "Isil must be set as query parameter.")));
      return;
    }

    handleStreamUpload(
        entity,
        okapiHeaders,
        asyncResultHandler,
        vertxContext,
        new StreamUploadResponses() {
          @Override
          public Response streamAborted() {
            return PostFincConfigFilesResponse.respond400WithTextPlain("Stream aborted");
          }

          @Override
          public Response fileSizeExceeded() {
            return PostFincConfigFilesResponse.respond413WithTextPlain(
                "File size exceeds maximum allowed size of " + MAX_UPLOAD_FILE_SIZE_MB + " MB");
          }

          @Override
          public Response internalError() {
            return PostFincConfigFilesResponse.respond500WithTextPlain("Internal server error");
          }
        },
        InputStream::readAllBytes,
        (streamId, headers, handler, context) ->
            createFile(streamId, isil, headers, handler, context));
  }

  private void createFile(
      String streamId,
      String isil,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    ByteArrayOutputStream baos = getAndRemoveStream(streamId);

    if (baos == null) {
      log.error("No data found for stream {} and isil {}", streamId, isil);
      asyncResultHandler.handle(
          Future.succeededFuture(
              PostFincConfigFilesResponse.respond500WithTextPlain("No upload data found")));
      return;
    }

    byte[] bytes = baos.toByteArray();
    String base64Data = Base64.getEncoder().encodeToString(bytes);
    String uuid = UUID.randomUUID().toString();
    File file = new File().withId(uuid).withIsil(isil).withData(base64Data);
    okapiHeaders.put(X_OKAPI_TENANT, Constants.MODULE_TENANT);
    fileDAO
        .upsert(file, uuid, vertxContext)
        .onSuccess(
            f ->
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        PostFincConfigFilesResponse.respond200WithTextPlain(uuid))))
        .onFailure(
            throwable ->
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        PostFincConfigFilesResponse.respond500WithTextPlain(
                            "Cannot insert file. " + throwable))));
  }

  @Override
  @Validate
  public void getFincConfigFilesById(
      String id,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    fileDAO
        .getById(id, vertxContext)
        .onComplete(
            ar ->
                handleAsyncFileResponse(
                    ar,
                    GetFincConfigFilesByIdResponse::respond200WithApplicationOctetStream,
                    GetFincConfigFilesByIdResponse::respond404WithTextPlain,
                    GetFincConfigFilesByIdResponse::respond500WithTextPlain,
                    asyncResultHandler));
  }

  @Override
  @Validate
  public void deleteFincConfigFilesById(
      String id,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    okapiHeaders.put(X_OKAPI_TENANT, Constants.MODULE_TENANT);
    fileDAO
        .deleteById(id, vertxContext)
        .onSuccess(
            integer ->
                asyncResultHandler.handle(
                    Future.succeededFuture(DeleteFincConfigFilesByIdResponse.respond204())))
        .onFailure(
            throwable ->
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        DeleteFincConfigFilesByIdResponse.respond500WithTextPlain(throwable))));
  }
}
