package org.folio.finc.config;

import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.TestUtils;
import org.folio.finc.FileUploadSizeLimitTestBase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class ConfigFilesUploadSizeLimitIT extends FileUploadSizeLimitTestBase {

  @Rule public Timeout timeout = Timeout.seconds(30);

  @BeforeClass
  public static void beforeClass() throws Exception {
    TestUtils.setupTenants();
  }

  @AfterClass
  public static void afterClass() throws Exception {
    TestUtils.teardownTenants();
  }

  @Override
  protected String getUploadEndpoint() {
    return FINC_CONFIG_FILES_ENDPOINT + "?isil=" + isilUbl.getIsil();
  }

  @Override
  protected String getDeleteEndpoint() {
    return FINC_CONFIG_FILES_ENDPOINT;
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
