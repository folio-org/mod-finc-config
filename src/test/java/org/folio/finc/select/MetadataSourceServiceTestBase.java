package org.folio.finc.select;

import static org.folio.rest.utils.Constants.MODULE_TENANT;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.folio.ApiTestBase;
import org.folio.TestUtils;
import org.folio.rest.jaxrs.model.*;
import org.folio.rest.persist.PostgresClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class MetadataSourceServiceTestBase extends ApiTestBase {
  static final String TABLE_ISILS = "isils";
  static final String TABLE_MD_SOURCES = "metadata_sources";
  static final String TABLE_MD_COLLECTIONS = "metadata_collections";
  protected static FincConfigMetadataSource metadataSource1 =
      getSampleEntity("fincConfigMetadataSource.sample", FincConfigMetadataSource.class);
  protected static FincConfigMetadataSource metadataSource2 =
      getSampleEntity("fincConfigMetadataSource2.sample", FincConfigMetadataSource.class);
  protected static FincConfigMetadataCollection metadataCollection1 =
      getSampleEntity("fincConfigMetadataCollection.sample", FincConfigMetadataCollection.class);
  protected static FincConfigMetadataCollection metadataCollection2 =
      getSampleEntity("fincConfigMetadataCollection2.sample", FincConfigMetadataCollection.class);
  protected static FincConfigMetadataCollection metadataCollection3 =
      getSampleEntity("fincConfigMetadataCollection3.sample", FincConfigMetadataCollection.class);
  protected static Isil isil1 = getSampleEntity("isil1.sample", Isil.class);
  protected static Isil isil2 = getSampleEntity("isil2.sample", Isil.class);

  private static final String EXAMPLES_PATH = "ramls/examples/";

  @BeforeClass
  public static void beforeClass() throws Exception {
    TestUtils.setupTenants();
    loadTestSamples().toCompletionStage().toCompletableFuture().get();
  }

  @AfterClass
  public static void afterClass() throws Exception {
    TestUtils.teardownTenants();
  }

  private static <T> T getSampleEntity(String fileName, Class<T> clazz) {
    try {
      String content = Files.readString(Path.of(EXAMPLES_PATH + fileName));
      return Json.decodeValue(content, clazz);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  static CompositeFuture loadTestSamples() {
    PostgresClient pgClient = PostgresClient.getInstance(vertx, MODULE_TENANT);
    return Future.all(
        pgClient.saveBatch(TABLE_ISILS, List.of(isil1, isil2)),
        pgClient.saveBatch(TABLE_MD_SOURCES, List.of(metadataSource1, metadataSource2)),
        pgClient.saveBatch(
            TABLE_MD_COLLECTIONS,
            List.of(metadataCollection1, metadataCollection2, metadataCollection3)));
  }
}
