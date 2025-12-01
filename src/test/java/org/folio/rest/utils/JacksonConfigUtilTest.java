package org.folio.rest.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.vertx.core.json.jackson.DatabindCodec;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.folio.dbschema.ObjectMapperTool;
import org.junit.jupiter.api.Test;

/** Tests for JacksonConfigUtil */
class JacksonConfigUtilTest {

  @Test
  void testConfigureJacksonConstraints() {
    JacksonConfigUtil.configureJacksonConstraints();

    // Verify DatabindCodec mapper
    int maxStringLength =
        DatabindCodec.mapper().getFactory().streamReadConstraints().getMaxStringLength();
    assertThat(maxStringLength).isEqualTo(70 * 1024 * 1024);

    // Verify ObjectMapperTool mapper
    int maxStringLengthTool =
        ObjectMapperTool.getMapper().getFactory().streamReadConstraints().getMaxStringLength();
    assertThat(maxStringLengthTool).isEqualTo(70 * 1024 * 1024);
  }

  @Test
  void testConfigureJacksonConstraintsCanBeCalledMultipleTimes() {
    // Should not throw exception when called multiple times
    JacksonConfigUtil.configureJacksonConstraints();
    JacksonConfigUtil.configureJacksonConstraints();

    int maxStringLength =
        DatabindCodec.mapper().getFactory().streamReadConstraints().getMaxStringLength();
    assertThat(maxStringLength).isEqualTo(70 * 1024 * 1024);
  }

  @Test
  void testUtilityClassCannotBeInstantiated() throws Exception {
    Constructor<JacksonConfigUtil> constructor = JacksonConfigUtil.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    assertThatThrownBy(constructor::newInstance)
        .isInstanceOf(InvocationTargetException.class)
        .hasCauseInstanceOf(IllegalStateException.class)
        .hasStackTraceContaining("Utility class");
  }
}
