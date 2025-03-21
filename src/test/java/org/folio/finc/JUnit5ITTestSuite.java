package org.folio.finc.select;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.folio.finc.ApiTestBase;
import org.folio.finc.ApiTestSuite;
import org.folio.finc.config.ConfigMetadataCollectionsWithFiltersIT;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
