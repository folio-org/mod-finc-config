package org.folio;

import org.folio.finc.config.ConfigFilesUploadSizeLimitIT;
import org.folio.finc.config.ConfigMetadataCollectionsWithFiltersIT;
import org.folio.finc.select.FincSelectFilesUploadSizeLimitIT;
import org.folio.rest.impl.FincConfigIsilsIT;
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

  @Nested
  class FincConfigIsilsITNested extends FincConfigIsilsIT {}

  @Nested
  class ConfigFilesUploadSizeLimitITNested extends ConfigFilesUploadSizeLimitIT {}

  @Nested
  class FincSelectFilesUploadSizeLimitITNested extends FincSelectFilesUploadSizeLimitIT {}
}
