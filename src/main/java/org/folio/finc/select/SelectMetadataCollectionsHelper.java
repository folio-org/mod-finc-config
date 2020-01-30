package org.folio.finc.select;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import java.util.List;
import java.util.Map;
import org.folio.finc.dao.IsilDAO;
import org.folio.finc.dao.IsilDAOImpl;
import org.folio.finc.dao.MetadataCollectionsDAO;
import org.folio.finc.dao.MetadataCollectionsDAOImpl;
import org.folio.finc.select.exception.FincSelectNotPermittedException;
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection.UsageRestricted;
import org.folio.rest.jaxrs.model.Select;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.TenantTool;

/**
 * Helper class to select metadata collections for finc-select. Filters out information if an item
 * is selected by resp. permitted for another organization than the requesting one.
 */
public class SelectMetadataCollectionsHelper {
  private final IsilDAO isilDAO;
  private final MetadataCollectionsDAO metadataCollectionsDAO;

  public SelectMetadataCollectionsHelper(Vertx vertx, String tenantId) {
    PostgresClient.getInstance(vertx);
    this.isilDAO = new IsilDAOImpl();
    this.metadataCollectionsDAO = new MetadataCollectionsDAOImpl();
  }

  private static FincConfigMetadataCollection setSelectStatus(
      FincConfigMetadataCollection metadataCollection, Select select, String isil) {
    List<String> permittedFor = metadataCollection.getPermittedFor();
    UsageRestricted usageRestricted = metadataCollection.getUsageRestricted();
    boolean isPermitted = usageRestricted.equals(UsageRestricted.NO) || permittedFor.contains(isil);

    if (!isPermitted) {
      throw new FincSelectNotPermittedException(
          "Selecting this metadata collection is not permitted");
    }

    List<String> selectedBy = metadataCollection.getSelectedBy();
    Boolean doSelect = select.getSelect();
    if (doSelect && !selectedBy.contains(isil)) {
      selectedBy.add(isil);
    } else if (!doSelect) {
      selectedBy.remove(isil);
    }
    return metadataCollection.withSelectedBy(selectedBy);
  }

  public Future<Boolean> selectMetadataCollection(
      String mdCollectionId,
      Select selectEntity,
      Map<String, String> okapiHeaders,
      Context vertxContext) {

    String tenantId =
        TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));

    Promise<Boolean> result = Promise.promise();
    doSelect(selectEntity, mdCollectionId, tenantId, vertxContext)
        .compose(
            metadataCollection ->
                this.metadataCollectionsDAO.update(
                    metadataCollection, mdCollectionId, vertxContext))
        .setHandler(
            ar -> {
              if (ar.succeeded()) {
                result.complete(true);
              } else {
                result.fail(ar.cause());
              }
            });
    return result.future();
  }

  private Future<FincConfigMetadataCollection> doSelect(
      Select selectEntity, String id, String tenantId, Context vertxContext) {
    Future<String> isilFuture = isilDAO.getIsilForTenant(tenantId, vertxContext);
    Future<FincConfigMetadataCollection> metadataCollectionFuture =
        metadataCollectionsDAO.getById(id, vertxContext);

    Promise<FincConfigMetadataCollection> result = Promise.promise();
    CompositeFuture.all(isilFuture, metadataCollectionFuture)
        .setHandler(
            ar -> {
              if (ar.succeeded()) {
                String isil = isilFuture.result();
                FincConfigMetadataCollection fincConfigMetadataCollection =
                    metadataCollectionFuture.result();
                FincConfigMetadataCollection updatedMDCollection;
                try {
                  updatedMDCollection =
                      setSelectStatus(fincConfigMetadataCollection, selectEntity, isil);
                } catch (FincSelectNotPermittedException e) {
                  result.fail(e);
                  return;
                }
                result.complete(updatedMDCollection);

              } else {
                result.fail("Cannot (un)select metadata collection. " + ar.cause());
              }
            });
    return result.future();
  }
}
