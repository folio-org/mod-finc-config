package org.folio.rest;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.jackson.DatabindCodec;
import org.folio.dbschema.ObjectMapperTool;

public class FincConfigLauncher extends RestLauncher {

  static final int MAX_STRING_LEN = 50 * 1024 * 1024; // 50MB

  public static void main(String[] args) {
    new FincConfigLauncher().dispatch(args);
  }

  @Override
  public void beforeStartingVertx(VertxOptions options) {
    super.beforeStartingVertx(options);

    ObjectMapper objectMapper = DatabindCodec.mapper();
    JsonFactory factory = objectMapper.getFactory();

    // Modify the max string length constraint for Vertx ObjectMapper
    factory.setStreamReadConstraints(
        StreamReadConstraints.builder().maxStringLength(MAX_STRING_LEN).build());

    // and for the ObjectMapper used by RMB
    ObjectMapperTool.getMapper()
        .getFactory()
        .setStreamReadConstraints(
            StreamReadConstraints.builder().maxStringLength(MAX_STRING_LEN).build());

    System.out.println(
        "Increased max string length to 50 MB for JSON parsing. (FincConfigLauncher)");
  }
}
