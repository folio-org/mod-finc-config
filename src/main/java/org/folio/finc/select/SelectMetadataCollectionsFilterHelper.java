package org.folio.finc.select;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.folio.finc.dao.IsilDAO;
import org.folio.finc.dao.IsilDAOImpl;
import org.folio.finc.dao.MetadataCollectionsDAO;
import org.folio.finc.dao.MetadataCollectionsDAOImpl;
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.Filter;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.jaxrs.model.FincSelectFiltersOfCollection;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.TenantTool;

public class SelectMetadataCollectionsFilterHelper {

  private final IsilDAO isilDAO;
  private final MetadataCollectionsDAO metadataCollectionsDAO;

  public SelectMetadataCollectionsFilterHelper(Vertx vertx) {
    PostgresClient.getInstance(vertx);
    this.isilDAO = new IsilDAOImpl();
    this.metadataCollectionsDAO = new MetadataCollectionsDAOImpl();
  }

  public Future<Boolean> addFiltersToCollectionAndSave(
      String mdCollectionId,
      FincSelectFiltersOfCollection filters,
      Map<String, String> okapiHeaders,
      Context vertxContext) {
    String tenantId =
        TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));

    Promise<Boolean> result = Promise.promise();
    updateFiltersOfCollection(filters, mdCollectionId, tenantId, vertxContext)
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

  private Future<FincConfigMetadataCollection> updateFiltersOfCollection(
      FincSelectFiltersOfCollection filters,
      String mdCollectionId,
      String tenantId,
      Context vertxContext) {
    Future<String> isilFuture = isilDAO.getIsilForTenant(tenantId, vertxContext);
    Future<FincConfigMetadataCollection> metadataCollectionFuture =
        metadataCollectionsDAO.getById(mdCollectionId, vertxContext);

    Promise<FincConfigMetadataCollection> result = Promise.promise();
    CompositeFuture.all(isilFuture, metadataCollectionFuture)
        .setHandler(
            ar -> {
              if (ar.succeeded()) {
                String isil = isilFuture.result();
                FincConfigMetadataCollection fincConfigMetadataCollection =
                    metadataCollectionFuture.result();
                FincConfigMetadataCollection fincConfigMetadataCollectionUpdated =
                    this.replaceFilterOfTenant(fincConfigMetadataCollection, filters, isil);
                result.complete(fincConfigMetadataCollectionUpdated);
              } else {
                result.fail("Cannot update filters of metadata collection. " + ar.cause());
              }
            });
    return result.future();
  }

  private FincConfigMetadataCollection replaceFilterOfTenant(
      FincConfigMetadataCollection fincConfigMetadataCollection,
      FincSelectFiltersOfCollection filters,
      String isil) {
    List<Filter> filtersOfCollection = fincConfigMetadataCollection.getFilters();

    Filter newFilter = new Filter().withIsil(isil).withFilters(filters.getFilters());
    List<Filter> filtersOfIsil =
        filtersOfCollection.stream()
            .filter(it -> it.getIsil().equals(isil))
            .collect(Collectors.toList());
    if (!filtersOfIsil.isEmpty()) {
      filtersOfIsil.stream().forEach(filtersOfCollection::remove);
    }
    filtersOfCollection.add(newFilter);
    fincConfigMetadataCollection.setFilters(filtersOfCollection);
    return fincConfigMetadataCollection;
  }
}
