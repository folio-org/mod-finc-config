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
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import javax.ws.rs.core.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.finc.model.File;
import org.folio.rest.jaxrs.resource.support.ResponseDelegate;
import org.folio.rest.tools.utils.BinaryOutStream;

/** Abstract class to handle file related responses and file upload validation */
public abstract class FincFileHandler {
  private static final Logger log = LogManager.getLogger(FincFileHandler.class);

  private static final long STREAM_MAX_AGE_MS = 5L * 60 * 1000;

  protected final Map<String, ByteArrayOutputStream> requestedBytes = new ConcurrentHashMap<>();
  protected final Map<String, Boolean> failedStreams = new ConcurrentHashMap<>();
  protected final Map<String, Long> streamTimestamps = new ConcurrentHashMap<>();

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

  /**
   * Handles file upload stream processing with common logic for size validation and error handling
   *
   * @param entity The input stream
   * @param okapiHeaders The okapi headers containing stream metadata
   * @param asyncResultHandler The response handler
   * @param vertxContext The Vert.x context
   * @param responses Response factory for creating appropriate responses
   * @param streamReader Function to read bytes from the input stream
   * @param fileCreator Function to create the file when stream is complete
   */
  protected void handleStreamUpload(
      InputStream entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext,
      StreamUploadResponses responses,
      StreamReader streamReader,
      FileCreator fileCreator) {

    cleanupAbandonedStreams();

    String streamId = okapiHeaders.get(STREAM_ID);
    boolean isComplete = okapiHeaders.get(STREAM_COMPLETE) != null;
    boolean isAbort = okapiHeaders.get(STREAM_ABORT) != null;

    try (InputStream bis = new BufferedInputStream(entity)) {
      if (!isComplete && !isAbort) {
        processBytesArrayFromStream(bis, streamId, streamReader);
      } else if (isAbort) {
        cleanupStream(streamId);
        asyncResultHandler.handle(Future.succeededFuture(responses.streamAborted()));
      } else {
        // Check if this stream previously failed validation
        if (failedStreams.containsKey(streamId)) {
          failedStreams.remove(streamId);
          asyncResultHandler.handle(Future.succeededFuture(responses.fileSizeExceeded()));
        } else {
          fileCreator.createFile(streamId, okapiHeaders, asyncResultHandler, vertxContext);
        }
      }
    } catch (FileSizeExceededException e) {
      requestedBytes.remove(streamId);
      failedStreams.put(streamId, true);
      asyncResultHandler.handle(Future.succeededFuture(responses.fileSizeExceeded(e.getMessage())));
    } catch (IOException e) {
      cleanupStream(streamId);
      log.error("Error processing file upload for stream {}", streamId, e);
      asyncResultHandler.handle(Future.succeededFuture(responses.internalError()));
    } catch (Exception e) {
      cleanupStream(streamId);
      log.error("Unexpected error processing file upload for stream {}", streamId, e);
      asyncResultHandler.handle(Future.succeededFuture(responses.internalError()));
    }
  }

  /**
   * Gets the byte array output stream for a completed upload and removes it from the map
   *
   * @param streamId The stream identifier
   * @return The ByteArrayOutputStream or null if not found
   */
  protected ByteArrayOutputStream getAndRemoveStream(String streamId) {
    streamTimestamps.remove(streamId);
    failedStreams.remove(streamId);
    return requestedBytes.remove(streamId);
  }

  private void processBytesArrayFromStream(
      InputStream is, String streamId, StreamReader streamReader) throws IOException {
    ByteArrayOutputStream baos = requestedBytes.get(streamId);
    if (baos == null) {
      baos = new ByteArrayOutputStream();
    }
    byte[] newBytes = streamReader.readBytes(is);

    validateAndWriteBytes(baos, newBytes, streamId);
    requestedBytes.put(streamId, baos);
    streamTimestamps.put(streamId, System.currentTimeMillis());
  }

  private void cleanupStream(String streamId) {
    requestedBytes.remove(streamId);
    failedStreams.remove(streamId);
    streamTimestamps.remove(streamId);
  }

  /**
   * Removes abandoned streams that have not been accessed within the maximum age threshold.
   * This prevents memory leaks from clients that start uploads but never complete or abort them.
   * Should be called periodically or at the start of new upload requests.
   */
  protected void cleanupAbandonedStreams() {
    long currentTime = System.currentTimeMillis();
    streamTimestamps.entrySet().removeIf(entry -> {
      long age = currentTime - entry.getValue();
      if (age > STREAM_MAX_AGE_MS) {
        String streamId = entry.getKey();
        log.info("Cleaning up abandoned stream {} (age: {} ms)", streamId, age);
        requestedBytes.remove(streamId);
        failedStreams.remove(streamId);
        return true;
      }
      return false;
    });
  }

  /** Functional interface for reading bytes from input stream */
  @FunctionalInterface
  protected interface StreamReader {
    byte[] readBytes(InputStream is) throws IOException;
  }

  /** Functional interface for creating file after stream is complete */
  @FunctionalInterface
  protected interface FileCreator {
    void createFile(
        String streamId,
        Map<String, String> okapiHeaders,
        Handler<AsyncResult<Response>> asyncResultHandler,
        Context vertxContext);
  }

  /** Interface for creating appropriate response types */
  protected interface StreamUploadResponses {
    Response streamAborted();

    Response fileSizeExceeded();

    default Response fileSizeExceeded(String message) {
      return fileSizeExceeded();
    }

    Response internalError();
  }
}
