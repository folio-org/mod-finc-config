package org.folio.finc.select.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import java.util.List;
import java.util.stream.Collectors;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.jaxrs.model.Isil;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
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
    if (Boolean.TRUE.equals(config().getBoolean("testing", false))) {
      logger.info("TEST ENV");
    } else {
      selectAllCollections(metadataSourceId, tenantId);
    }
  }

  public Future<Void> selectAllCollections(String mdSourceId, String tenantId) {

    return fetchIsil(tenantId)
        .compose(isil -> fetchPermittedCollections(mdSourceId, isil))
        .compose(metadataCollections -> doSelectAndSave(metadataCollections, tenantId))
        .compose(compositeFuture -> updateSelectedBy(mdSourceId));
  }

  private Future<List<FincConfigMetadataCollection>> fetchPermittedCollections(
      String mdSourceId, String isil) {

    Promise<List<FincConfigMetadataCollection>> result = Promise.promise();
    Criteria usageRestrictedCrit =
        new Criteria().addField("'usageRestricted'").setJSONB(true).setOperation("=").setVal("no");
    Criteria permittedForCrit =
        new Criteria()
            .addField("(jsonb->>'permittedFor')::jsonb")
            .setJSONB(false)
            .setOperation("?")
            .setVal(isil);
    Criteria mdSourceCrit =
        new Criteria()
            .addField("jsonb->'mdSource'->>'id'")
            .setJSONB(false)
            .setOperation("=")
            .setVal(mdSourceId);
    Criterion c =
        new Criterion()
            .addCriterion(usageRestrictedCrit, "OR", permittedForCrit)
            .addCriterion(mdSourceCrit);

    PostgresClient.getInstance(context.owner(), Constants.MODULE_TENANT)
        .get(
            METADATA_COLLECTIONS_TABLE,
            FincConfigMetadataCollection.class,
            c,
            false,
            false,
            ar -> {
              if (ar.succeeded()) {
                result.complete(ar.result().getResults());
              } else {
                result.fail("Cannot fetch permitted collections: " + ar.cause());
              }
            });

    return result.future();
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
    Promise<String> result = Promise.promise();
    Criteria tenantCrit =
        new Criteria().addField("'tenant'").setJSONB(true).setOperation("=").setVal(tenantId);
    Criterion criterion = new Criterion(tenantCrit);
    PostgresClient.getInstance(context.owner(), Constants.MODULE_TENANT)
        .get(
            ISILS_TABLE,
            Isil.class,
            criterion,
            false,
            false,
            ar -> {
              if (ar.succeeded()) {
                List<Isil> isils = ar.result().getResults();
                if (isils.isEmpty()) {
                  result.fail("Cannot find isil for tenant " + tenantId);
                } else if (isils.size() > 1) {
                  result.fail("Found multiple isils for tenant " + tenantId);
                } else {
                  Isil isil = isils.get(0);
                  result.complete(isil.getIsil());
                }
              } else {
                result.fail("Cannot fetch isil: " + ar.cause());
              }
            });
    return result.future();
  }

  private List<Future> saveCollections(List<FincConfigMetadataCollection> selected) {
    return selected.stream().map(this::saveSingleCollection).collect(Collectors.toList());
  }

  private Future<Void> saveSingleCollection(FincConfigMetadataCollection metadataCollection) {
    Promise<Void> result = Promise.promise();
    PostgresClient.getInstance(context.owner(), Constants.MODULE_TENANT)
        .update(
            METADATA_COLLECTIONS_TABLE,
            metadataCollection,
            metadataCollection.getId(),
            ar -> {
              if (ar.succeeded()) {
                result.complete();
              } else {
                result.fail("Cannot save md collection: " + ar.cause());
              }
            });
    return result.future();
  }

  public Future<Void> updateSelectedBy(String mdSourceId) {
    Promise<Void> result = Promise.promise();
    String query = String.format("SELECT * FROM update_selected_state('%s')", mdSourceId);
    PostgresClient.getInstance(context.owner(), Constants.MODULE_TENANT)
        .select(
            query,
            ar -> {
              if (ar.succeeded()) {
                result.complete();
              } else {
                result.fail("Cannot update selectedBy. " + ar.cause());
              }
            });
    return result.future();
  }
}
