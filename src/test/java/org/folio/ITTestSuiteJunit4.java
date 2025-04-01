package org.folio;

import org.folio.finc.config.ConfigContactsIT;
import org.folio.finc.config.ConfigEZBCredentialsIT;
import org.folio.finc.config.ConfigFilesIT;
import org.folio.finc.config.ConfigFiltersIT;
import org.folio.finc.config.ConfigMetadataCollectionsIT;
import org.folio.finc.config.ConfigMetadataSourcesIT;
import org.folio.finc.config.TinyMetadataSourcesIT;
import org.folio.finc.periodic.EZBHarvestJobIT;
import org.folio.finc.select.FilterHelperIT;
import org.folio.finc.select.FincSelectFilesIT;
import org.folio.finc.select.FincSelectFiltersIT;
import org.folio.finc.select.SelectEZBCredentialsIT;
import org.folio.finc.select.SelectMetadataCollectionsIT;
import org.folio.finc.select.SelectMetadataSourceVerticleIT;
import org.folio.finc.select.SelectMetadataSourcesHelperIT;
import org.folio.finc.select.SelectMetadataSourcesIT;
import org.folio.finc.select.UnselectMetadataSourceVerticleIT;
import org.folio.rest.impl.TenantIT;
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
  EZBHarvestJobIT.class,
  FilterHelperIT.class,
  FincSelectFilesIT.class,
  FincSelectFiltersIT.class,
  SelectEZBCredentialsIT.class,
  SelectMetadataCollectionsIT.class,
  SelectMetadataSourceVerticleIT.class,
  SelectMetadataSourcesHelperIT.class,
  SelectMetadataSourcesIT.class,
  TenantIT.class,
  TinyMetadataSourcesIT.class,
  UnselectMetadataSourceVerticleIT.class
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
