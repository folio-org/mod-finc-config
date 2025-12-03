package org.folio.finc.select;

import org.folio.TestUtils;
import org.folio.finc.FileUploadSizeLimitTestBase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class FincSelectFilesUploadSizeLimitIT extends FileUploadSizeLimitTestBase {

  @BeforeAll
  static void beforeAll() throws Exception {
    TestUtils.setupTenants();
  }

  @AfterAll
  static void afterAll() throws Exception {
    TestUtils.teardownTenants();
  }

  @Override
  protected String getUploadEndpoint() {
    return FINC_SELECT_FILES_ENDPOINT;
  }

  @Override
  protected String getDeleteEndpoint() {
    return FINC_SELECT_FILES_ENDPOINT;
  }

  @Test
  public void testUploadFileBelowSizeLimit() {
    super.testUploadFileBelowSizeLimit();
  }

  @Test
  public void testUploadFileExceedsSizeLimit() {
    super.testUploadFileExceedsSizeLimit();
  }

  @Test
  public void testUploadLargeFileNearLimit() {
    super.testUploadLargeFileNearLimit();
  }

  @Test
  public void testUploadFileJustOverLimit() {
    super.testUploadFileJustOverLimit();
  }
}
