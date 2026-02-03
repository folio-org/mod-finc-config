package org.folio.finc.select.services;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Context;
import io.vertx.core.Future;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.jaxrs.model.Isil;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.interfaces.Results;
import org.folio.rest.utils.Constants;

/**
 * Service to select resp. unselect all metadata collections of a single metadata source. The actual
 * behavior (select/unselect) is implemented by overriding the {@link #select(List, String)} method.
 */
public abstract class AbstractSelectMetadataSourceService {

  private static final Logger logger =
      LogManager.getLogger(AbstractSelectMetadataSourceService.class);
  private static final String METADATA_COLLECTIONS_TABLE = "metadata_collections";
  private static final String ISILS_TABLE = "isils";

  protected final Context context;

  protected AbstractSelectMetadataSourceService(Context context) {
    this.context = context;
  }

  public Future<Void> selectAllCollections(String mdSourceId, String tenantId) {
    return fetchIsil(tenantId)
        .compose(isil -> fetchPermittedCollections(mdSourceId, isil))
        .compose(metadataCollections -> doSelectAndSave(metadataCollections, tenantId))
        .compose(compositeFuture -> updateSelectedBy(mdSourceId));
  }

  /**
   * Fetches {@link FincConfigMetadataCollection}s from the DB that are permitted. Permitted means
   * usageRestricted is set to no or the isil is listed in the permittedFor array.
   *
   * @param mdSourceId ID of metadata source
   * @param isil The current isil
   * @return Future with permitted collections
   */
  private Future<List<FincConfigMetadataCollection>> fetchPermittedCollections(
      String mdSourceId, String isil) {

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
    Criterion criterion =
        new Criterion()
            .addCriterion(usageRestrictedCrit, "OR", permittedForCrit)
            .addCriterion(mdSourceCrit);

    return PostgresClient.getInstance(context.owner(), Constants.MODULE_TENANT)
        .get(METADATA_COLLECTIONS_TABLE, FincConfigMetadataCollection.class, criterion, false)
        .map(Results::getResults)
        .onFailure(cause -> logger.error("Cannot fetch permitted collections", cause));
  }

  private Future<CompositeFuture> doSelectAndSave(
      List<FincConfigMetadataCollection> metadataCollections, String tenantId) {

    return fetchIsil(tenantId)
        .compose(
            isil -> {
              List<FincConfigMetadataCollection> selected = select(metadataCollections, isil);
              List<Future<Void>> futures = saveCollections(selected);
              return Future.join(futures);
            });
  }

  abstract List<FincConfigMetadataCollection> select(
      List<FincConfigMetadataCollection> metadataCollections, String isil);

  /**
   * Fetches the isil that is assigned to the tenant
   *
   * @param tenantId ID of tenant
   * @return Future with the isil string
   */
  private Future<String> fetchIsil(String tenantId) {
    Criteria tenantCrit =
        new Criteria().addField("'tenant'").setJSONB(true).setOperation("=").setVal(tenantId);
    Criterion criterion = new Criterion(tenantCrit);

    return PostgresClient.getInstance(context.owner(), Constants.MODULE_TENANT)
        .get(ISILS_TABLE, Isil.class, criterion, false)
        .onFailure(cause -> logger.error("Cannot fetch isil for tenant {}", tenantId, cause))
        .compose(
            results -> {
              List<Isil> isils = results.getResults();
              if (isils.isEmpty()) {
                return Future.failedFuture("Cannot find isil for tenant " + tenantId);
              } else if (isils.size() > 1) {
                return Future.failedFuture("Found multiple isils for tenant " + tenantId);
              } else {
                return Future.succeededFuture(isils.get(0).getIsil());
              }
            });
  }

  private List<Future<Void>> saveCollections(List<FincConfigMetadataCollection> selected) {
    return selected.stream().map(this::saveSingleCollection).toList();
  }

  private Future<Void> saveSingleCollection(FincConfigMetadataCollection metadataCollection) {
    return PostgresClient.getInstance(context.owner(), Constants.MODULE_TENANT)
        .update(METADATA_COLLECTIONS_TABLE, metadataCollection, metadataCollection.getId())
        .onFailure(cause -> logger.error("Cannot save md collection", cause))
        .mapEmpty();
  }

  public Future<Void> updateSelectedBy(String mdSourceId) {
    String query = String.format("SELECT * FROM update_selected_state('%s')", mdSourceId);
    return PostgresClient.getInstance(context.owner(), Constants.MODULE_TENANT)
        .select(query)
        .onFailure(
            cause -> logger.error("Cannot update selectedBy for source {}", mdSourceId, cause))
        .mapEmpty();
  }
}
