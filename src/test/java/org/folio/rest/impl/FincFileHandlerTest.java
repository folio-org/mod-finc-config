package org.folio.rest.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.rest.utils.Constants.*;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.ws.rs.core.Response;
import org.folio.finc.model.File;
import org.folio.rest.jaxrs.resource.support.ResponseDelegate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FincFileHandlerTest {

  private Context vertxContext;
  private TestFincFileHandler handler;

  @BeforeEach
  void setUp() {
    Vertx vertx = Vertx.vertx();
    vertxContext = vertx.getOrCreateContext();
    handler = new TestFincFileHandler();
  }

  @Test
  void testHandleAsyncFileResponseWithFailure() {
    AsyncResult<File> failedResult = Future.failedFuture(new RuntimeException("Database error"));

    AtomicReference<Response> capturedResponse = new AtomicReference<>();
    Handler<AsyncResult<Response>> asyncResultHandler =
        ar -> capturedResponse.set(ar.result());

    handler.handleAsyncFileResponse(
        failedResult,
        bos -> new TestResponseDelegate(Response.ok().build()),
        msg -> new TestResponseDelegate(Response.status(404).entity(msg).build()),
        throwable -> new TestResponseDelegate(Response.status(500).entity("Internal error").build()),
        asyncResultHandler);

    assertThat(capturedResponse.get()).isNotNull();
    assertThat(capturedResponse.get().getStatus()).isEqualTo(500);
  }

  @Test
  void testHandleAsyncFileResponseWithSuccess() {
    File file = new File();
    file.setData(java.util.Base64.getEncoder().encodeToString("test data".getBytes()));

    AsyncResult<File> successResult = Future.succeededFuture(file);

    AtomicReference<Response> capturedResponse = new AtomicReference<>();
    Handler<AsyncResult<Response>> asyncResultHandler =
        ar -> capturedResponse.set(ar.result());

    handler.handleAsyncFileResponse(
        successResult,
        bos -> new TestResponseDelegate(Response.ok().entity(bos).build()),
        msg -> new TestResponseDelegate(Response.status(404).entity(msg).build()),
        throwable -> new TestResponseDelegate(Response.status(500).entity("Internal error").build()),
        asyncResultHandler);

    assertThat(capturedResponse.get()).isNotNull();
    assertThat(capturedResponse.get().getStatus()).isEqualTo(200);
  }

  @Test
  void testHandleStreamUploadWithAbort() {
    InputStream inputStream = new ByteArrayInputStream(new byte[0]);
    Map<String, String> headers = new HashMap<>();
    headers.put(STREAM_ID, "test-stream");
    headers.put(STREAM_ABORT, "true");

    AtomicReference<Response> capturedResponse = new AtomicReference<>();
    Handler<AsyncResult<Response>> asyncResultHandler =
        ar -> capturedResponse.set(ar.result());

    TestStreamUploadResponses responses = new TestStreamUploadResponses();

    handler.handleStreamUpload(
        inputStream,
        headers,
        asyncResultHandler,
        vertxContext,
        responses,
        is -> new byte[0],
        (streamId, okapiHeaders, handlerParam, ctx) -> {});

    assertThat(capturedResponse.get()).isNotNull();
    assertThat(capturedResponse.get().getStatus()).isEqualTo(204);
    assertThat(capturedResponse.get().getEntity()).isEqualTo("Stream aborted");
  }

  @Test
  void testHandleStreamUploadWithFileSizeExceededDuringStreaming() {
    FincFileHandler.StreamReader streamReader =
        is -> {
          throw new FileSizeExceededException("File too large");
        };

    InputStream inputStream = new ByteArrayInputStream(new byte[100]);
    Map<String, String> headers = new HashMap<>();
    headers.put(STREAM_ID, "test-stream");

    AtomicReference<Response> capturedResponse = new AtomicReference<>();
    Handler<AsyncResult<Response>> asyncResultHandler =
        ar -> capturedResponse.set(ar.result());

    TestStreamUploadResponses responses = new TestStreamUploadResponses();

    handler.handleStreamUpload(
        inputStream,
        headers,
        asyncResultHandler,
        vertxContext,
        responses,
        streamReader,
        (streamId, okapiHeaders, handlerParam, ctx) -> {});

    assertThat(capturedResponse.get()).isNotNull();
    assertThat(capturedResponse.get().getStatus()).isEqualTo(413);
  }

  @Test
  void testHandleStreamUploadWithIOException() {
    FincFileHandler.StreamReader streamReader =
        is -> {
          throw new IOException("Stream read error");
        };

    InputStream inputStream = new ByteArrayInputStream(new byte[100]);
    Map<String, String> headers = new HashMap<>();
    headers.put(STREAM_ID, "test-stream");

    AtomicReference<Response> capturedResponse = new AtomicReference<>();
    Handler<AsyncResult<Response>> asyncResultHandler =
        ar -> capturedResponse.set(ar.result());

    TestStreamUploadResponses responses = new TestStreamUploadResponses();

    handler.handleStreamUpload(
        inputStream,
        headers,
        asyncResultHandler,
        vertxContext,
        responses,
        streamReader,
        (streamId, okapiHeaders, handlerParam, ctx) -> {});

    assertThat(capturedResponse.get()).isNotNull();
    assertThat(capturedResponse.get().getStatus()).isEqualTo(500);
    assertThat(capturedResponse.get().getEntity()).isEqualTo("Internal server error");
  }

  @Test
  void testHandleStreamUploadWithGenericException() {
    FincFileHandler.StreamReader streamReader =
        is -> {
          throw new RuntimeException("Unexpected error");
        };

    InputStream inputStream = new ByteArrayInputStream(new byte[100]);
    Map<String, String> headers = new HashMap<>();
    headers.put(STREAM_ID, "test-stream");

    AtomicReference<Response> capturedResponse = new AtomicReference<>();
    Handler<AsyncResult<Response>> asyncResultHandler =
        ar -> capturedResponse.set(ar.result());

    TestStreamUploadResponses responses = new TestStreamUploadResponses();

    handler.handleStreamUpload(
        inputStream,
        headers,
        asyncResultHandler,
        vertxContext,
        responses,
        streamReader,
        (streamId, okapiHeaders, handlerParam, ctx) -> {});

    assertThat(capturedResponse.get()).isNotNull();
    assertThat(capturedResponse.get().getStatus()).isEqualTo(500);
  }

  @Test
  void testHandleStreamUploadWithFailedStreamCompletion() {
    String streamId = "failed-stream";

    InputStream inputStream1 = new ByteArrayInputStream(new byte[100]);
    Map<String, String> headers1 = new HashMap<>();
    headers1.put(STREAM_ID, streamId);

    FincFileHandler.StreamReader failingReader =
        is -> {
          throw new FileSizeExceededException("File too large");
        };

    AtomicReference<Response> capturedResponse1 = new AtomicReference<>();
    Handler<AsyncResult<Response>> asyncResultHandler1 =
        ar -> capturedResponse1.set(ar.result());

    TestStreamUploadResponses responses = new TestStreamUploadResponses();

    // First call fails
    handler.handleStreamUpload(
        inputStream1,
        headers1,
        asyncResultHandler1,
        vertxContext,
        responses,
        failingReader,
        (sid, okapiHeaders, handlerParam, ctx) -> {});

    assertThat(capturedResponse1.get()).isNotNull();
    assertThat(capturedResponse1.get().getStatus()).isEqualTo(413);

    // Now send completion for the failed stream
    InputStream inputStream2 = new ByteArrayInputStream(new byte[0]);
    Map<String, String> headers2 = new HashMap<>();
    headers2.put(STREAM_ID, streamId);
    headers2.put(STREAM_COMPLETE, "true");

    AtomicReference<Response> capturedResponse2 = new AtomicReference<>();
    Handler<AsyncResult<Response>> asyncResultHandler2 =
        ar -> capturedResponse2.set(ar.result());

    handler.handleStreamUpload(
        inputStream2,
        headers2,
        asyncResultHandler2,
        vertxContext,
        responses,
        is -> new byte[0],
        (sid, okapiHeaders, handlerParam, ctx) -> {});

    assertThat(capturedResponse2.get()).isNotNull();
    assertThat(capturedResponse2.get().getStatus()).isEqualTo(413);
  }

  @Test
  void testFileSizeExceededWithCustomMessage() {
    TestStreamUploadResponses responses = new TestStreamUploadResponses();
    Response response = responses.fileSizeExceeded("Custom error message");
    assertThat(response.getStatus()).isEqualTo(413);
    assertThat(response.getEntity()).isEqualTo("Custom error message");
  }

  @Test
  void testGetAndRemoveStream() {
    String streamId = "test-stream";
    handler.requestedBytes.put(streamId, new java.io.ByteArrayOutputStream());

    assertThat(handler.requestedBytes).containsKey(streamId);

    java.io.ByteArrayOutputStream result = handler.getAndRemoveStream(streamId);

    assertThat(result).isNotNull();
    assertThat(handler.requestedBytes).doesNotContainKey(streamId);
  }

  @Test
  void testCleanupAbandonedStreams() throws InterruptedException {
    // Add some streams with different ages
    String oldStream = "old-stream";
    String recentStream = "recent-stream";

    // Add old stream and set its timestamp to 6 minutes ago (older than 5 min threshold)
    handler.requestedBytes.put(oldStream, new java.io.ByteArrayOutputStream());
    handler.streamTimestamps.put(oldStream, System.currentTimeMillis() - (6 * 60 * 1000));

    // Add recent stream with current timestamp
    handler.requestedBytes.put(recentStream, new java.io.ByteArrayOutputStream());
    handler.streamTimestamps.put(recentStream, System.currentTimeMillis());

    // Verify both streams exist
    assertThat(handler.requestedBytes).containsKey(oldStream);
    assertThat(handler.requestedBytes).containsKey(recentStream);

    // Run cleanup
    handler.cleanupAbandonedStreams();

    // Old stream should be removed
    assertThat(handler.requestedBytes).doesNotContainKey(oldStream);
    assertThat(handler.streamTimestamps).doesNotContainKey(oldStream);

    // Recent stream should still exist
    assertThat(handler.requestedBytes).containsKey(recentStream);
    assertThat(handler.streamTimestamps).containsKey(recentStream);
  }

  @Test
  void testCleanupAbandonedStreamsWithFailedStreams() {
    // Add an abandoned stream that also has a failed status
    String abandonedStream = "abandoned-failed-stream";

    handler.requestedBytes.put(abandonedStream, new java.io.ByteArrayOutputStream());
    handler.failedStreams.put(abandonedStream, true);
    handler.streamTimestamps.put(abandonedStream, System.currentTimeMillis() - (6 * 60 * 1000));

    // Verify stream exists in all maps
    assertThat(handler.requestedBytes).containsKey(abandonedStream);
    assertThat(handler.failedStreams).containsKey(abandonedStream);
    assertThat(handler.streamTimestamps).containsKey(abandonedStream);

    // Run cleanup
    handler.cleanupAbandonedStreams();

    // All traces should be removed
    assertThat(handler.requestedBytes).doesNotContainKey(abandonedStream);
    assertThat(handler.failedStreams).doesNotContainKey(abandonedStream);
    assertThat(handler.streamTimestamps).doesNotContainKey(abandonedStream);
  }

  @Test
  void testCleanupAbandonedStreamsWithEmptyMaps() {
    // Should not throw exception with empty maps
    handler.cleanupAbandonedStreams();

    assertThat(handler.requestedBytes).isEmpty();
    assertThat(handler.failedStreams).isEmpty();
    assertThat(handler.streamTimestamps).isEmpty();
  }

  // Test implementation of FincFileHandler
  static class TestFincFileHandler extends FincFileHandler {}

  // Test wrapper for ResponseDelegate with public constructor
  static class TestResponseDelegate extends ResponseDelegate {
    TestResponseDelegate(Response response) {
      super(response);
    }
  }

  // Test implementation of StreamUploadResponses
  static class TestStreamUploadResponses implements FincFileHandler.StreamUploadResponses {
    @Override
    public Response streamAborted() {
      return Response.status(204).entity("Stream aborted").build();
    }

    @Override
    public Response fileSizeExceeded() {
      return Response.status(413).entity("File size exceeded").build();
    }

    @Override
    public Response fileSizeExceeded(String message) {
      return Response.status(413).entity(message).build();
    }

    @Override
    public Response internalError() {
      return Response.status(500).entity("Internal server error").build();
    }
  }
}
