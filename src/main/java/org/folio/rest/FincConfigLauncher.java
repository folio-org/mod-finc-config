package org.folio.rest;

import com.fasterxml.jackson.core.StreamReadConstraints;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.jackson.DatabindCodec;
import org.folio.dbschema.ObjectMapperTool;

public class FincConfigLauncher extends RestLauncher {

  static final int MAX_STRING_LEN = 70 * 1024 * 1024; // 70MB (allows 50MB uploads after Base64 encoding)

  static {
    // Configure Jackson with increased string length limit to support 50MB file uploads
    StreamReadConstraints constraints =
        StreamReadConstraints.builder().maxStringLength(MAX_STRING_LEN).build();

    try {
      DatabindCodec.mapper().getFactory().setStreamReadConstraints(constraints);
      ObjectMapperTool.getMapper().getFactory().setStreamReadConstraints(constraints);
    } catch (Exception e) {
      // Log to stderr since loggers may not be initialized yet
      System.err.println("WARNING: Failed to configure Jackson constraints: " + e.getMessage());
    }
  }

  public static void main(String[] args) {
    new FincConfigLauncher().dispatch(args);
  }

  @Override
  public void beforeStartingVertx(VertxOptions options) {
    super.beforeStartingVertx(options);
  }
}
