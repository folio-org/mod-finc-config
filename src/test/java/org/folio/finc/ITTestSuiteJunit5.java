package org.folio.finc;

import org.folio.finc.config.ConfigMetadataCollectionsWithFiltersIT;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;

class ITTestSuiteJunit5 {

  @BeforeAll
  static void beforeAll() throws Exception {
    ITTestSuiteJunit4.before();
  }

  @AfterAll
  static void afterAll() throws Exception {
    ITTestSuiteJunit4.after();
  }

  @Nested
  class ConfigMetadataCollectionsWithFiltersITNested
      extends ConfigMetadataCollectionsWithFiltersIT {}
}
