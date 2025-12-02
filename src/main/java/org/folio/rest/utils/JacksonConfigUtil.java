package org.folio.rest.utils;

import com.fasterxml.jackson.core.StreamReadConstraints;
import io.vertx.core.json.jackson.DatabindCodec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dbschema.ObjectMapperTool;

/** Utility class for configuring Jackson ObjectMapper settings */
public final class JacksonConfigUtil {

  private static final Logger log = LogManager.getLogger(JacksonConfigUtil.class);

  private JacksonConfigUtil() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Configures Jackson with increased string length limit to support large file uploads (50MB after
   * Base64 encoding)
   */
  public static void configureJacksonConstraints() {
    // 70MB allows 50MB uploads after Base64 encoding (~33% overhead)
    int maxStringLength = 70 * 1024 * 1024;
    StreamReadConstraints constraints =
        StreamReadConstraints.builder().maxStringLength(maxStringLength).build();

    try {
      DatabindCodec.mapper().getFactory().setStreamReadConstraints(constraints);
      ObjectMapperTool.getMapper().getFactory().setStreamReadConstraints(constraints);
      log.info("Configured Jackson with maxStringLength: {} bytes", maxStringLength);
    } catch (Exception e) {
      log.error("Failed to configure Jackson constraints: {}", e.getMessage(), e);
    }
  }
}
