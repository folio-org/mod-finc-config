package org.folio;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.impl.TenantReferenceApi;
import org.folio.rest.jaxrs.model.*;
import org.folio.rest.persist.PgExceptionUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.ModuleName;
import org.folio.rest.utils.Constants;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TenantUtil {

  private static final Logger logger = LogManager.getLogger(TenantUtil.class);
  private static final String TENANT_UBL = "ubl";
  private static final String TENANT_DIKU = "diku";

  static FincConfigMetadataSource metadataSource1;
  static FincConfigMetadataSource metadataSource2;
  static FincConfigMetadataCollection metadataCollection1;
  static FincConfigMetadataCollection metadataCollection2;
  static FincConfigMetadataCollection metadataCollection3;
  static Isil isil1;
  static Isil isil2;

  static {
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
      logger.error(e.getMessage());
    }
  }

  public static FincConfigMetadataSource getMetadataSource1() {
    return metadataSource1;
  }

  public static FincConfigMetadataSource getMetadataSource2() {
    return metadataSource2;
  }

  public static FincConfigMetadataCollection getMetadataCollection1() {
    return metadataCollection1;
  }

  public static FincConfigMetadataCollection getMetadataCollection2() {
    return metadataCollection2;
  }

  public static FincConfigMetadataCollection getMetadataCollection3() {
    return metadataCollection3;
  }

  public Future<Void> writeDataToDB(TestContext context, Vertx vertx) {
    Async async = context.async(3);
    Promise<Void> result = Promise.promise();

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
                      context.fail(
                          "Could not load isils. "
                              + PgExceptionUtil.getMessage(asyncResult.cause()));
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
    return result.future();
  }

  private Future<Void> postTenant(String tenant, int port, Vertx vertx) {
    Promise<Void> result = Promise.promise();
    try {

      Map<String, String> headers = new HashMap<>();
      headers.put(XOkapiHeaders.TENANT, tenant);
      headers.put(XOkapiHeaders.URL, "http://localhost:" + port);
      new TenantReferenceApi()
          .postTenant(
              new TenantAttributes().withModuleTo(ModuleName.getModuleVersion()),
              headers,
              res -> {
                if (res.result().getStatus() == 201) {
                  result.complete();
                } else {
                  result.fail(
                      String.format(
                          "Tenantloading returned %s %s",
                          res.result().getStatus(),
                          res.result().getStatusInfo().getReasonPhrase()));
                }
              },
              vertx.getOrCreateContext());

    } catch (Exception e) {
      result.fail(e);
    }
    return result.future();
  }

  public Future<Void> postDikuTenant(int port, Vertx vertx) {
    return postTenant(TENANT_DIKU, port, vertx);
  }

  public Future<Void> postUBLTenant(int port, Vertx vertx) {
    return postTenant(TENANT_UBL, port, vertx);
  }

  public Future<Void> postFincTenant(int port, Vertx vertx, TestContext context) {
    Promise<Void> promise = Promise.promise();
    try {
      Map<String, String> headers = new HashMap<>();
      headers.put(XOkapiHeaders.TENANT, Constants.MODULE_TENANT);
      headers.put(XOkapiHeaders.URL, "http://localhost:" + port);
      new TenantReferenceApi()
          .postTenant(
              new TenantAttributes().withModuleTo(ModuleName.getModuleVersion()),
              headers,
              res -> {
                if (res.result().getStatus() == 201) {
                  Future<Void> future = writeDataToDB(context, vertx);
                  future.onSuccess(promise::complete);
                  future.onFailure(promise::fail);
                } else {
                  promise.fail(
                      String.format(
                          "Tenantloading returned %s %s",
                          res.result().getStatus(),
                          res.result().getStatusInfo().getReasonPhrase()));
                }
              },
              vertx.getOrCreateContext());
    } catch (Exception e) {
      promise.fail(e);
    }
    return promise.future();
  }
}
