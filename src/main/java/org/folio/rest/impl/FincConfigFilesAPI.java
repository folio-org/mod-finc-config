package org.folio.rest.impl;

import static org.folio.rest.utils.Constants.*;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
  private Map<String, ByteArrayOutputStream> requestedBytes = new ConcurrentHashMap<>();
  private Map<String, Boolean> failedStreams = new ConcurrentHashMap<>();

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
    String streamId = okapiHeaders.get(STREAM_ID);
    boolean isComplete = okapiHeaders.get(STREAM_COMPLETE) != null;
    boolean isAbort = okapiHeaders.get(STREAM_ABORT) != null;

    try (InputStream bis = new BufferedInputStream(entity)) {
      if (!isComplete && !isAbort) {
        processBytesArrayFromStream(bis, streamId);
      } else if (isAbort) {
        requestedBytes.remove(streamId);
        failedStreams.remove(streamId);
        asyncResultHandler.handle(
            Future.succeededFuture(
                PostFincConfigFilesResponse.respond400WithTextPlain("Stream aborted")));
      } else {
        // Check if this stream previously failed validation
        if (failedStreams.containsKey(streamId)) {
          failedStreams.remove(streamId);
          asyncResultHandler.handle(
              Future.succeededFuture(
                  PostFincConfigFilesResponse.respond413WithTextPlain(
                      "File size exceeds maximum allowed size of " + MAX_UPLOAD_FILE_SIZE_MB + " MB")));
        } else {
          createFile(streamId, isil, okapiHeaders, asyncResultHandler, vertxContext);
        }
      }
    } catch (FileSizeExceededException e) {
      requestedBytes.remove(streamId);
      failedStreams.put(streamId, true);
      asyncResultHandler.handle(
          Future.succeededFuture(
              PostFincConfigFilesResponse.respond413WithTextPlain(e.getMessage())));
    } catch (IOException e) {
      requestedBytes.remove(streamId);
      failedStreams.remove(streamId);
      log.error("Error processing file upload for stream {} and isil {}", streamId, isil, e);
      asyncResultHandler.handle(
          Future.succeededFuture(
              PostFincConfigFilesResponse.respond500WithTextPlain("Internal server error")));
    } catch (Exception e) {
      requestedBytes.remove(streamId);
      failedStreams.remove(streamId);
      log.error("Unexpected error processing file upload for stream {} and isil {}", streamId, isil, e);
      asyncResultHandler.handle(
          Future.succeededFuture(
              PostFincConfigFilesResponse.respond500WithTextPlain("Internal server error")));
    }
  }

  private void createFile(
      String streamId,
      String isil,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    ByteArrayOutputStream baos = requestedBytes.get(streamId);
    requestedBytes.remove(streamId);

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

  private void processBytesArrayFromStream(InputStream is, String streamId) throws IOException {
    ByteArrayOutputStream baos = requestedBytes.get(streamId);
    if (baos == null) {
      baos = new ByteArrayOutputStream();
    }
    byte[] newBytes = is.readAllBytes();

    validateAndWriteBytes(baos, newBytes, streamId);
    requestedBytes.put(streamId, baos);
  }
}
