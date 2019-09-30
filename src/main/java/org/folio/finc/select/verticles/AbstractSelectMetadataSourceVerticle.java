package org.folio.finc.select.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import java.util.List;
import java.util.stream.Collectors;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.jaxrs.model.Isil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSelectMetadataSourceVerticle extends AbstractVerticle {

  private static final Logger logger =
      LoggerFactory.getLogger(AbstractSelectMetadataSourceVerticle.class);

  private static final String METADATA_COLLECTIONS_TABLE = "metadata_collections";
  private static final String ISILS_TABLE = "isils";

  public AbstractSelectMetadataSourceVerticle(Vertx vertx, Context ctx) {
    super();
    super.init(vertx, ctx);
  }

  @Override
  public void start() {
    String metadataSourceId = config().getString("metadataSourceId");
    String tenantId = config().getString("tenantId");
    logger.info("Deployed AbstractSelectMetadataSourceVerticle");
    if(config().getBoolean("testing", false)) {
      logger.info("TEST ENV");
    } else {
      selectAllCollections(metadataSourceId, tenantId);
    }
  }

  public Future<CompositeFuture> selectAllCollections(String mdSourceId, String tenantId) {

    return fetchIsil(tenantId)
        .compose(isil -> fetchPermittedCollections(mdSourceId, isil))
        .compose(metadataCollections -> doSelectAndSave(metadataCollections, tenantId));
  }

  private Future<List<FincConfigMetadataCollection>> fetchPermittedCollections(
      String mdSourceId, String isil) {

    Future<List<FincConfigMetadataCollection>> result = Future.future();
    String where =
        String.format(
            " WHERE (jsonb->>'usageRestricted'='no' OR (jsonb->>'permittedFor')::jsonb ? '%s') AND jsonb->'mdSource'->>'id'='%s'",
            isil, mdSourceId);

    PostgresClient.getInstance(context.owner(), Constants.MODULE_TENANT)
        .get(
            METADATA_COLLECTIONS_TABLE,
            FincConfigMetadataCollection.class,
            where,
            false,
            false,
            ar -> {
              if (ar.succeeded()) {
                result.complete(ar.result().getResults());
              } else {
                result.fail("Cannot fetch permitted collections: " + ar.cause());
              }
            });

    return result;
  }

  private Future<CompositeFuture> doSelectAndSave(
      List<FincConfigMetadataCollection> metadataCollections, String tenantId) {

    return fetchIsil(tenantId)
        .compose(
            isil -> {
              List<FincConfigMetadataCollection> selected = select(metadataCollections, isil);
              List<Future> futures = saveCollections(selected);
              return CompositeFuture.join(futures);
            });
  }

  abstract List<FincConfigMetadataCollection> select(
      List<FincConfigMetadataCollection> metadataCollections, String isil);

  private Future<String> fetchIsil(String tenantId) {
    Future<String> future = Future.future();
    String where = String.format(" WHERE (jsonb->>'tenant' = '%s')", tenantId);
    PostgresClient.getInstance(context.owner(), Constants.MODULE_TENANT)
        .get(
            ISILS_TABLE,
            Isil.class,
            where,
            false,
            false,
            ar -> {
              if (ar.succeeded()) {
                List<Isil> isils = ar.result().getResults();
                if (isils.size() != 1) {
                  future.fail("Number isils != 1");
                } else {
                  Isil isil = isils.get(0);
                  future.complete(isil.getIsil());
                }
              } else {
                future.fail("Cannot fetch isil: " + ar.cause());
              }
            });
    return future;
  }

  private List<Future> saveCollections(List<FincConfigMetadataCollection> selected) {
    return selected.stream().map(this::saveSingleCollection).collect(Collectors.toList());
  }

  private Future saveSingleCollection(FincConfigMetadataCollection metadataCollection) {
    Future future = Future.future();
    PostgresClient.getInstance(context.owner(), Constants.MODULE_TENANT)
        .update(
            METADATA_COLLECTIONS_TABLE,
            metadataCollection,
            metadataCollection.getId(),
            ar -> {
              if (ar.succeeded()) {
                future.complete();
              } else {
                future.fail("Cannot save md collection: " + ar.cause());
              }
            });
    return future;
  }
}
