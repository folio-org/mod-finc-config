package org.folio.rest.utils;

import com.fasterxml.jackson.core.StreamReadConstraints;
import io.vertx.core.json.jackson.DatabindCodec;
import org.folio.dbschema.ObjectMapperTool;

/** Utility class for configuring Jackson ObjectMapper settings */
public final class JacksonConfigUtil {

  private JacksonConfigUtil() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Configures Jackson with increased string length limit to support large file uploads (50MB after
   * Base64 encoding)
   */
  public static void configureJacksonConstraints() {
    // 70MB allows 50MB uploads after Base64 encoding (~33% overhead)
    StreamReadConstraints constraints =
        StreamReadConstraints.builder().maxStringLength(70 * 1024 * 1024).build();

    try {
      DatabindCodec.mapper().getFactory().setStreamReadConstraints(constraints);
      ObjectMapperTool.getMapper().getFactory().setStreamReadConstraints(constraints);
    } catch (Exception e) {
      // Log to stderr since loggers may not be initialized yet
      System.err.println("WARNING: Failed to configure Jackson constraints: " + e.getMessage());
    }
  }
}
