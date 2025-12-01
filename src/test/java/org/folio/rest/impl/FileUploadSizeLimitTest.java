package org.folio.rest.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.rest.utils.Constants.MAX_UPLOAD_FILE_SIZE;

import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for file upload size limit validation */
class FileUploadSizeLimitTest {

  private TestFincFileHandler handler;

  @BeforeEach
  void setUp() {
    handler = new TestFincFileHandler();
  }

  @Test
  void testFileSizeWithinLimit() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] data = new byte[1024]; // 1 KB

    handler.validateAndWriteBytes(baos, data, "test-stream-1");

    assertThat(baos.size()).isEqualTo(1024);
  }

  @Test
  void testFileSizeExactlyAtLimit() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] data = new byte[(int) MAX_UPLOAD_FILE_SIZE];

    handler.validateAndWriteBytes(baos, data, "test-stream-2");

    assertThat(baos.size()).isEqualTo(MAX_UPLOAD_FILE_SIZE);
  }

  @Test
  void testFileSizeJustBelowLimit() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] data = new byte[(int) MAX_UPLOAD_FILE_SIZE - 1];

    handler.validateAndWriteBytes(baos, data, "test-stream-3");

    assertThat(baos.size()).isEqualTo(MAX_UPLOAD_FILE_SIZE - 1);
  }

  @Test
  void testFileSizeExceedsLimit() {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] data = new byte[(int) MAX_UPLOAD_FILE_SIZE + 1];

    assertThatThrownBy(() -> handler.validateAndWriteBytes(baos, data, "test-stream-4"))
        .isInstanceOf(FileSizeExceededException.class)
        .hasMessageContaining("File size exceeds maximum allowed size of 50 MB");
  }

  @Test
  void testFileSizeExceedsLimitWithMultipleChunks() {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] chunk1 = new byte[30 * 1024 * 1024]; // 30 MB
    byte[] chunk2 = new byte[21 * 1024 * 1024]; // 21 MB, total = 51 MB

    // First chunk should succeed
    assertThat(baos.size()).isZero();
    try {
      handler.validateAndWriteBytes(baos, chunk1, "test-stream-5");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    assertThat(baos.size()).isEqualTo(30 * 1024 * 1024);

    // Second chunk should fail
    assertThatThrownBy(() -> handler.validateAndWriteBytes(baos, chunk2, "test-stream-5"))
        .isInstanceOf(FileSizeExceededException.class)
        .hasMessageContaining("File size exceeds maximum allowed size of 50 MB");
  }

  @Test
  void testFileSizeLargeMultipleChunks() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] chunk = new byte[10 * 1024 * 1024]; // 10 MB chunks

    // Add 5 chunks of 10 MB each = 50 MB total (should succeed)
    for (int i = 0; i < 5; i++) {
      handler.validateAndWriteBytes(baos, chunk, "test-stream-6");
    }

    assertThat(baos.size()).isEqualTo(50 * 1024 * 1024);

    // Adding one more byte should fail
    byte[] oneByte = new byte[1];
    assertThatThrownBy(() -> handler.validateAndWriteBytes(baos, oneByte, "test-stream-6"))
        .isInstanceOf(FileSizeExceededException.class);
  }

  @Test
  void testGetAndRemoveStream() {
    String streamId = "test-stream";
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    handler.requestedBytes.put(streamId, baos);

    ByteArrayOutputStream result = handler.getAndRemoveStream(streamId);

    assertThat(result).isSameAs(baos);
    assertThat(handler.requestedBytes).doesNotContainKey(streamId);
  }

  @Test
  void testGetAndRemoveStreamNotFound() {
    ByteArrayOutputStream result = handler.getAndRemoveStream("non-existent");
    assertThat(result).isNull();
  }

  @Test
  void testGetAndRemoveStreamCleansUpAllMaps() {
    String streamId = "test-stream";
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    handler.requestedBytes.put(streamId, baos);
    handler.failedStreams.put(streamId, true);
    handler.streamTimestamps.put(streamId, System.currentTimeMillis());

    assertThat(handler.requestedBytes).containsKey(streamId);
    assertThat(handler.failedStreams).containsKey(streamId);
    assertThat(handler.streamTimestamps).containsKey(streamId);

    // getAndRemoveStream should clean up all three maps to prevent memory leaks
    handler.getAndRemoveStream(streamId);
    assertThat(handler.requestedBytes).doesNotContainKey(streamId);
    assertThat(handler.failedStreams).doesNotContainKey(streamId);
    assertThat(handler.streamTimestamps).doesNotContainKey(streamId);
  }

  @Test
  void testEmptyStreamProcessing() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] emptyData = new byte[0];

    handler.validateAndWriteBytes(baos, emptyData, "empty-stream");

    assertThat(baos.size()).isZero();
  }

  @Test
  void testFileSizeExceededExceptionWithMessage() {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] data = new byte[(int) MAX_UPLOAD_FILE_SIZE + 1000];

    FileSizeExceededException exception =
        org.junit.jupiter.api.Assertions.assertThrows(
            FileSizeExceededException.class,
            () -> handler.validateAndWriteBytes(baos, data, "test-stream-large"));

    assertThat(exception.getMessage()).contains("50 MB");
    assertThat(exception).isInstanceOf(java.io.IOException.class);
  }

  /** Test implementation of FincFileHandler to access protected method */
  private static class TestFincFileHandler extends FincFileHandler {
    // Exposes protected members for testing
  }
}
