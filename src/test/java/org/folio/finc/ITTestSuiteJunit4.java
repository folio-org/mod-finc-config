package org.folio.finc;

import org.folio.finc.config.*;
import org.folio.finc.select.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  ConfigContactsIT.class,
  ConfigEZBCredentialsIT.class,
  ConfigFilesIT.class,
  ConfigFiltersIT.class,
  ConfigMetadataCollectionsIT.class,
  ConfigMetadataSourcesIT.class,
  FilterHelperIT.class,
  FincSelectFilesIT.class,
  FincSelectFiltersIT.class,
  IsilsIT.class,
  SelectEZBCredentialsIT.class,
  SelectMetadataCollectionsIT.class,
  SelectMetadataSourcesIT.class,
  TinyMetadataSourcesIT.class,
})
public class ITTestSuiteJunit4 {

  @BeforeClass
  public static void before() throws Exception {
    TestUtils.setupTestSuite();
  }

  @AfterClass
  public static void after() throws Exception {
    TestUtils.teardownTestSuite();
  }
}
