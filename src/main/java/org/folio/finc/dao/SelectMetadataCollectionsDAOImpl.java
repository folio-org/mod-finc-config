package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Promise;
import java.util.List;
import org.folio.finc.select.query.MetadataCollectionsQueryTranslator;
import org.folio.finc.select.isil.filter.IsilFilter;
import org.folio.finc.select.isil.filter.MetadataCollectionIsilFilter;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.jaxrs.model.FincSelectMetadataCollection;
import org.folio.rest.jaxrs.model.FincSelectMetadataCollections;

public class SelectMetadataCollectionsDAOImpl implements SelectMetadataCollectionsDAO {

  private final IsilFilter<FincSelectMetadataCollection, FincConfigMetadataCollection> isilFilter;
  private MetadataCollectionsDAO metadataCollectionsDAO;
  private MetadataCollectionsQueryTranslator queryTranslator;

  public SelectMetadataCollectionsDAOImpl() {
    super();
    this.isilFilter = new MetadataCollectionIsilFilter();
    this.metadataCollectionsDAO = new MetadataCollectionsDAOImpl();
    this.queryTranslator = new MetadataCollectionsQueryTranslator();
  }

  @Override
  public Promise<FincSelectMetadataCollections> getAll(
      String query, int offset, int limit, String isil, Context vertxContext) {

    Promise<FincSelectMetadataCollections> result = Promise.promise();
    query = queryTranslator.translateQuery(query, isil);
    metadataCollectionsDAO
        .getAll(query, offset, limit, vertxContext)
        .future()
        .setHandler(
            ar -> {
              if (ar.succeeded()) {

                FincSelectMetadataCollections collectionsCollection =
                    new FincSelectMetadataCollections();

                List<FincConfigMetadataCollection> fincConfigMetadataCollections =
                    ar.result().getFincConfigMetadataCollections();

                List<FincSelectMetadataCollection> transformedCollections =
                    isilFilter.filterForIsil(fincConfigMetadataCollections, isil);

                collectionsCollection.setFincSelectMetadataCollections(transformedCollections);
                collectionsCollection.setTotalRecords(transformedCollections.size());
                result.complete(collectionsCollection);
              } else {
                result.fail(ar.cause());
                return;
              }
            });
    return result;
  }

  @Override
  public Promise<FincSelectMetadataCollection> getById(
      String id, String isil, Context vertxContext) {
    Promise<FincSelectMetadataCollection> result = Promise.promise();

    metadataCollectionsDAO
        .getById(id, vertxContext)
        .future()
        .setHandler(
            ar -> {
              if (ar.succeeded()) {
                FincSelectMetadataCollection fincSelectMetadataCollection =
                    isilFilter.filterForIsil(ar.result(), isil);
                result.complete(fincSelectMetadataCollection);
              } else {
                result.fail("Cannot get finc select metadata collection by id. " + ar.cause());
                return;
              }
            });
    return result;
  }
}
