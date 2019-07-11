package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Future;
import java.util.List;
import org.folio.finc.select.QueryTranslator;
import org.folio.finc.select.isil.filter.IsilFilter;
import org.folio.finc.select.isil.filter.MetadataCollectionIsilFilter;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.jaxrs.model.FincSelectMetadataCollection;
import org.folio.rest.jaxrs.model.FincSelectMetadataCollections;

public class SelectMetadataCollectionsDAOImpl implements SelectMetadataCollectionsDAO {

  private final IsilFilter<FincSelectMetadataCollection, FincConfigMetadataCollection> isilFilter;
  private MetadataCollectionsDAO metadataCollectionsDAO;

  public SelectMetadataCollectionsDAOImpl() {
    super();
    this.isilFilter = new MetadataCollectionIsilFilter();
    this.metadataCollectionsDAO = new MetadataCollectionsDAOImpl();
  }

  @Override
  public Future<FincSelectMetadataCollections> getAll(String query, int offset, int limit,
    String isil, Context vertxContext) {

    Future<FincSelectMetadataCollections> result = Future.future();
    query = QueryTranslator.translate(query, isil);
    metadataCollectionsDAO.getAll(query, offset, limit, vertxContext)
      .setHandler(ar -> {
        if (ar.succeeded()) {

          FincSelectMetadataCollections collectionsCollection = new FincSelectMetadataCollections();

          List<FincConfigMetadataCollection> fincConfigMetadataCollections = ar.result().getFincConfigMetadataCollections();

          List<FincSelectMetadataCollection> transformedCollections =
            isilFilter.filterForIsil(fincConfigMetadataCollections, isil);

          collectionsCollection.setFincSelectMetadataCollections(
            transformedCollections);
          collectionsCollection.setTotalRecords(
            transformedCollections.size());
          result.complete(collectionsCollection);
        } else {
          result.fail(ar.cause());
        }
      });
    return result;
  }

  @Override
  public Future<FincSelectMetadataCollection> getById(String id, String isil,
    Context vertxContext) {
    Future<FincSelectMetadataCollection> result = Future.future();

    metadataCollectionsDAO.getById(id, vertxContext)
      .setHandler(ar -> {
        if (ar.succeeded()) {
          FincSelectMetadataCollection fincSelectMetadataCollection = isilFilter.filterForIsil(
            ar.result(), isil);
          result.complete(fincSelectMetadataCollection);
        } else {
          result.fail("Cannot get finc select metadata collection by id. " + ar.cause());
        }
      });
    return result;
  }

}
