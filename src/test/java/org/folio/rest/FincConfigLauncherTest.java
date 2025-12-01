package org.folio.rest;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.json.jackson.DatabindCodec;
import org.folio.dbschema.ObjectMapperTool;
import org.junit.jupiter.api.Test;

/** Tests for FincConfigLauncher */
class FincConfigLauncherTest {

  @Test
  void testJacksonConstraintsConfiguredOnClassLoad() {
    // The static initializer should have configured Jackson with 70MB limit
    int maxStringLength =
        DatabindCodec.mapper().getFactory().streamReadConstraints().getMaxStringLength();

    assertThat(maxStringLength).isEqualTo(70 * 1024 * 1024);

    int maxStringLengthTool =
        ObjectMapperTool.getMapper().getFactory().streamReadConstraints().getMaxStringLength();
    assertThat(maxStringLengthTool).isEqualTo(70 * 1024 * 1024);
  }

  @Test
  void testLauncherCanBeInstantiated() {
    FincConfigLauncher launcher = new FincConfigLauncher();
    assertThat(launcher).isNotNull();
  }
}
