package org.folio.finc.select;

import org.folio.finc.ApiTestSuite;
import org.folio.finc.config.ConfigMetadataCollectionsWithFiltersIT;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;

class JUnit5ITTestSuite {

  @BeforeAll
  static void beforeAll() throws Exception {
    ApiTestSuite.before();
  }

  @AfterAll
  static void afterAll() throws Exception {
    ApiTestSuite.after();
  }

  @Nested
  class ConfigMetadataCollectionsWithFiltersITNested
      extends ConfigMetadataCollectionsWithFiltersIT {}
}
