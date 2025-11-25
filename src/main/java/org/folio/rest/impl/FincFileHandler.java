package org.folio.rest.impl;

import static org.folio.rest.utils.Constants.MAX_UPLOAD_FILE_SIZE;
import static org.folio.rest.utils.Constants.MAX_UPLOAD_FILE_SIZE_MB;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.function.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.finc.model.File;
import org.folio.rest.jaxrs.resource.support.ResponseDelegate;
import org.folio.rest.tools.utils.BinaryOutStream;

/** Abstract class to handle file related responses and file upload validation */
public abstract class FincFileHandler {
  private static final Logger log = LogManager.getLogger(FincFileHandler.class);

  protected void handleAsyncFileResponse(
      AsyncResult<File> ar,
      Function<BinaryOutStream, ResponseDelegate> func200,
      Function<String, ResponseDelegate> func404,
      Function<Throwable, ResponseDelegate> func500,
      io.vertx.core.Handler<io.vertx.core.AsyncResult<javax.ws.rs.core.Response>>
          asyncResultHandler) {
    if (ar.succeeded()) {
      handleFileResponse(ar.result(), func200, func404, asyncResultHandler);
    } else {
      asyncResultHandler.handle(Future.succeededFuture(func500.apply(ar.cause())));
    }
  }

  private void handleFileResponse(
      File file,
      Function<BinaryOutStream, ResponseDelegate> succeedFunc,
      Function<String, ResponseDelegate> failFunc,
      io.vertx.core.Handler<io.vertx.core.AsyncResult<javax.ws.rs.core.Response>>
          asyncResultHandler) {
    if (file == null) {
      asyncResultHandler.handle(Future.succeededFuture(failFunc.apply("Not found")));
    } else {
      String dataAsString = file.getData();
      byte[] decoded = Base64.getDecoder().decode(dataAsString);

      BinaryOutStream binaryOutStream = new BinaryOutStream();
      binaryOutStream.setData(decoded);
      asyncResultHandler.handle(Future.succeededFuture(succeedFunc.apply(binaryOutStream)));
    }
  }

  /**
   * Validates and writes new bytes to the output stream, checking file size limit
   *
   * @param baos The existing ByteArrayOutputStream
   * @param newBytes The new bytes to add
   * @param streamId The stream identifier for logging
   * @throws FileSizeExceededException if total size exceeds maximum
   * @throws IOException if writing fails
   */
  protected void validateAndWriteBytes(ByteArrayOutputStream baos, byte[] newBytes, String streamId)
      throws IOException {
    long totalSize = (long) baos.size() + newBytes.length;

    if (totalSize > MAX_UPLOAD_FILE_SIZE) {
      log.warn(
          "File upload rejected for stream {}: size {} exceeds {} MB limit",
          streamId,
          totalSize,
          MAX_UPLOAD_FILE_SIZE_MB);
      throw new FileSizeExceededException(
          String.format(
              "File size exceeds maximum allowed size of %d MB", MAX_UPLOAD_FILE_SIZE_MB));
    }

    baos.write(newBytes);
  }
}
