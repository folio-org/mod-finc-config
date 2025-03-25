package org.folio;

import org.folio.finc.config.ConfigMetadataCollectionsWithFiltersIT;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;

class ITTestSuiteJunit5 {

  @BeforeAll
  static void beforeAll() throws Exception {
    TestUtils.setupTestSuite();
  }

  @AfterAll
  static void afterAll() throws Exception {
    TestUtils.teardownTestSuite();
  }

  @Nested
  class ConfigMetadataCollectionsWithFiltersITNested
      extends ConfigMetadataCollectionsWithFiltersIT {}
}
