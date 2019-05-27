package org.folio.finc.select;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollections;
import org.folio.rest.jaxrs.model.FincConfigMetadataSource;
import org.folio.rest.jaxrs.model.FincConfigMetadataSources;
import org.folio.rest.jaxrs.model.Isil;
import org.folio.rest.jaxrs.model.Isils;
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

  static Future<Void> writeDataToDB(TestContext context, Vertx vertx) {
    Async async = context.async(3);
    Future<Void> result = Future.future();

    vertx.executeBlocking(
        future -> {
          List<Isil> isilList = Arrays.asList(isil1, isil2);
          Isils isils = new Isils().withIsils(isilList);
          JsonArray i =
              isils.getIsils().stream()
                  .map(Json::encode)
                  .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
          PostgresClient.getInstance(vertx, Constants.MODULE_TENANT)
              .saveBatch(
                  "isils",
                  i,
                  asyncResult -> {
                    if (asyncResult.succeeded()) {
                      logger.info("Loaded isils");
                      async.countDown();
                    } else {
                      context.fail("Could not load isils");
                    }
                  });

          List<FincConfigMetadataSource> sourcesList =
              Arrays.asList(metadataSource1, metadataSource2);
          FincConfigMetadataSources fincConfigMetadataSources =
              new FincConfigMetadataSources().withFincConfigMetadataSources(sourcesList);
          JsonArray sources =
              fincConfigMetadataSources.getFincConfigMetadataSources().stream()
                  .map(Json::encode)
                  .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
          PostgresClient.getInstance(vertx, Constants.MODULE_TENANT)
              .saveBatch(
                  "metadata_sources",
                  sources,
                  asyncResult -> {
                    if (asyncResult.succeeded()) {
                      logger.info("Loaded metadata sources");
                      async.countDown();
                    } else {
                      context.fail("Could not load metadata sources");
                    }
                  });
          List<FincConfigMetadataCollection> collectionsList =
              Arrays.asList(metadataCollection1, metadataCollection2, metadataCollection3);
          FincConfigMetadataCollections fincConfigMetadataCollections =
              new FincConfigMetadataCollections()
                  .withFincConfigMetadataCollections(collectionsList);
          JsonArray collections =
              fincConfigMetadataCollections.getFincConfigMetadataCollections().stream()
                  .map(Json::encode)
                  .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
          PostgresClient.getInstance(vertx, Constants.MODULE_TENANT)
              .saveBatch(
                  "metadata_collections",
                  collections,
                  asyncResult -> {
                    if (asyncResult.succeeded()) {
                      logger.info("Loaded metadata collections");
                      async.countDown();
                    } else {
                      context.fail("Could not load metadata collections");
                    }
                  });
          async.await();
          result.complete();
        },
        asyncResult -> {
          if (asyncResult.succeeded()) {
            result.complete();
          } else {
            result.fail("Cannot load testdata");
          }
        });
    return result;
  }
}
