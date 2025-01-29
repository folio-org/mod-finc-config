package org.folio.rest;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.jackson.DatabindCodec;

public class FincConfigLauncher extends RestLauncher {

  public static void main(String[] args) {
    new FincConfigLauncher().dispatch(args);
  }

  @Override
  public void beforeStartingVertx(VertxOptions options) {
    super.beforeStartingVertx(options);

    ObjectMapper objectMapper = DatabindCodec.mapper();
    JsonFactory factory = objectMapper.getFactory();

    // Modify only the max string length constraint
    factory.setStreamReadConstraints(
        StreamReadConstraints.builder()
            .maxStringLength(50 * 1024 * 1024) // Increase limit to 50 MB
            .build());
    System.out.println(
        "Increased max string length to 50 MB for JSON parsing. (FincConfigLauncher)");
  }
}
