package org.folio.rest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Tests for FincConfigLauncher */
class FincConfigLauncherTest {

  @Test
  void testLauncherCanBeInstantiated() {
    FincConfigLauncher launcher = new FincConfigLauncher();
    assertThat(launcher).isNotNull();
  }
}
