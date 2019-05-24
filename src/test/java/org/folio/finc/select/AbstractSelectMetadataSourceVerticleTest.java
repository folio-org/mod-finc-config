package org.folio.finc.select;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.jaxrs.model.FincConfigMetadataSource;
import org.folio.rest.jaxrs.model.Isil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.utils.Constants;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public abstract class AbstractSelectMetadataSourceVerticleTest {

  private static final Logger logger =
      LoggerFactory.getLogger(SelectMetadataSourceVerticleTest.class);

  static FincConfigMetadataSource metadataSource1;
  static FincConfigMetadataSource metadataSource2;
  static FincConfigMetadataCollection metadataCollection1;
  static FincConfigMetadataCollection metadataCollection2;
  static FincConfigMetadataCollection metadataCollection3;
  static Isil isil1;
  static Isil isil2;

  private static Vertx vertx = Vertx.vertx();

  static void readData(TestContext context) {
    try {
      String metadataSourceStr1 =
          new String(
              Files.readAllBytes(Paths.get("ramls/examples/fincConfigMetadataSource.sample")));
      metadataSource1 = Json.decodeValue(metadataSourceStr1, FincConfigMetadataSource.class);
      String metadataSourceStr2 =
          new String(
              Files.readAllBytes(Paths.get("ramls/examples/fincConfigMetadataSource2.sample")));
      metadataSource2 = Json.decodeValue(metadataSourceStr2, FincConfigMetadataSource.class);

      String metadataCollectionStr1 =
          new String(
              Files.readAllBytes(Paths.get("ramls/examples/fincConfigMetadataCollection.sample")));
      metadataCollection1 =
          Json.decodeValue(metadataCollectionStr1, FincConfigMetadataCollection.class);
      String metadataCollectionStr2 =
          new String(
              Files.readAllBytes(Paths.get("ramls/examples/fincConfigMetadataCollection2.sample")));
      metadataCollection2 =
          Json.decodeValue(metadataCollectionStr2, FincConfigMetadataCollection.class);
      String metadataCollectionStr3 =
          new String(
              Files.readAllBytes(Paths.get("ramls/examples/fincConfigMetadataCollection3.sample")));
      metadataCollection3 =
          Json.decodeValue(metadataCollectionStr3, FincConfigMetadataCollection.class);

      String isilStr1 = new String(Files.readAllBytes(Paths.get("ramls/examples/isil1.sample")));
      isil1 = Json.decodeValue(isilStr1, Isil.class);

      String isilStr2 = new String(Files.readAllBytes(Paths.get("ramls/examples/isil2.sample")));
      isil2 = Json.decodeValue(isilStr2, Isil.class);
    } catch (Exception e) {
      context.fail(e);
    }
  }

  static Future<Void> writeDataToDB(TestContext context) {
    Async async = context.async(7);
    PostgresClient.getInstance(vertx, Constants.MODULE_TENANT)
        .save(
            "isils",
            isil1.getId(),
            isil1,
            asyncResult -> {
              if (asyncResult.succeeded()) {
                logger.info("Loaded isil");
                async.countDown();
              } else {
                context.fail("Could not load isil");
              }
            });

    PostgresClient.getInstance(vertx, Constants.MODULE_TENANT)
        .save(
            "isils",
            isil2.getId(),
            isil2,
            asyncResult -> {
              if (asyncResult.succeeded()) {
                logger.info("Loaded isil");
                async.countDown();
              } else {
                context.fail("Could not load isil");
              }
            });

    PostgresClient.getInstance(vertx, Constants.MODULE_TENANT)
        .save(
            "metadata_sources",
            metadataSource1.getId(),
            metadataSource1,
            asyncResult -> {
              if (asyncResult.succeeded()) {
                logger.info("Loaded metadata source 1");
                async.countDown();
              } else {
                context.fail("Could not load metadata source 1");
              }
            });
    PostgresClient.getInstance(vertx, Constants.MODULE_TENANT)
        .save(
            "metadata_sources",
            metadataSource2.getId(),
            metadataSource2,
            asyncResult -> {
              if (asyncResult.succeeded()) {
                logger.info("Loaded metadata source 2");
                async.countDown();
              } else {
                context.fail("Could not load metadata source 2");
              }
            });
    PostgresClient.getInstance(vertx, Constants.MODULE_TENANT)
        .save(
            "metadata_collections",
            metadataCollection1.getId(),
            metadataCollection1,
            asyncResult -> {
              if (asyncResult.succeeded()) {
                logger.info("Loaded metadata collection 1");
                async.countDown();
              } else {
                context.fail("Could not load metadata collection 1");
              }
            });
    PostgresClient.getInstance(vertx, Constants.MODULE_TENANT)
        .save(
            "metadata_collections",
            metadataCollection2.getId(),
            metadataCollection2,
            asyncResult -> {
              if (asyncResult.succeeded()) {
                logger.info("Loaded metadata collection 2");
                async.countDown();
              } else {
                context.fail("Could not load metadata collection 2");
              }
            });
    PostgresClient.getInstance(vertx, Constants.MODULE_TENANT)
        .save(
            "metadata_collections",
            metadataCollection3.getId(),
            metadataCollection3,
            asyncResult -> {
              if (asyncResult.succeeded()) {
                logger.info("Loaded metadata collection 3");
                async.countDown();
              } else {
                context.fail("Could not load metadata collection 3");
              }
            });
    async.await();
    return Future.succeededFuture();
  }
}
