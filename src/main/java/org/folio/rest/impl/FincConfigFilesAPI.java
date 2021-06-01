package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import org.apache.commons.lang3.ArrayUtils;
import org.folio.finc.dao.FileDAO;
import org.folio.finc.dao.FileDAOImpl;
import org.folio.finc.model.File;
import org.folio.rest.annotations.Stream;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.resource.FincConfigFiles;
import org.folio.rest.utils.Constants;

import javax.ws.rs.core.Response;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.folio.rest.utils.Constants.*;

public class FincConfigFilesAPI extends FincFileHandler implements FincConfigFiles {

  public static final String X_OKAPI_TENANT = "x-okapi-tenant";

  private final FileDAO fileDAO;
  private Map<String, byte[]> requestedBytes = new HashMap<>();

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
    try (InputStream bis = new BufferedInputStream(entity)) {
      if (Objects.isNull(okapiHeaders.get(STREAM_COMPLETE))) {
        processBytesArrayFromStream(bis, streamId);
      } else if (Objects.nonNull(okapiHeaders.get(STREAM_ABORT))) {
        asyncResultHandler.handle(
            Future.succeededFuture(
                PostFincConfigFilesResponse.respond400WithTextPlain("Stream aborted")));
      } else {
        // stream is completed
        createFile(streamId, isil, okapiHeaders, asyncResultHandler, vertxContext);
      }
    } catch (IOException e) {
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
    byte[] bytes = requestedBytes.get(streamId);
    requestedBytes.remove(streamId);
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
    byte[] requestBytesArray = requestedBytes.get(streamId);
    if (requestBytesArray == null) {
      requestBytesArray = new byte[0];
    }
    requestBytesArray = ArrayUtils.addAll(requestBytesArray, is.readAllBytes());
    requestedBytes.put(streamId, requestBytesArray);
  }
}
