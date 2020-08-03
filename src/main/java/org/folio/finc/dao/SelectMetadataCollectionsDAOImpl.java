package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import java.util.List;
import org.folio.finc.select.transform.Transformer;
import org.folio.finc.select.transform.MetadataCollectionTransformer;
import org.folio.finc.select.query.MetadataCollectionsQueryTranslator;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.jaxrs.model.FincSelectMetadataCollection;
import org.folio.rest.jaxrs.model.FincSelectMetadataCollections;

public class SelectMetadataCollectionsDAOImpl implements SelectMetadataCollectionsDAO {

  private final Transformer<FincSelectMetadataCollection, FincConfigMetadataCollection> transformer;
  private final MetadataCollectionsDAO metadataCollectionsDAO;
  private final MetadataCollectionsQueryTranslator queryTranslator;

  public SelectMetadataCollectionsDAOImpl() {
    super();
    this.transformer = new MetadataCollectionTransformer();
    this.metadataCollectionsDAO = new MetadataCollectionsDAOImpl();
    this.queryTranslator = new MetadataCollectionsQueryTranslator();
  }

  @Override
  public Future<FincSelectMetadataCollections> getAll(
      String query, int offset, int limit, String isil, Context vertxContext) {

    Promise<FincSelectMetadataCollections> result = Promise.promise();
    query = queryTranslator.translateQuery(query, isil);
    metadataCollectionsDAO
        .getAll(query, offset, limit, vertxContext)
        .onComplete(
            ar -> {
              if (ar.succeeded()) {

                FincSelectMetadataCollections collectionsCollection =
                    new FincSelectMetadataCollections();

                List<FincConfigMetadataCollection> fincConfigMetadataCollections =
                    ar.result().getFincConfigMetadataCollections();

                List<FincSelectMetadataCollection> transformedCollections =
                    transformer.transformCollection(fincConfigMetadataCollections, isil);

                collectionsCollection.setFincSelectMetadataCollections(transformedCollections);
                collectionsCollection.setTotalRecords(ar.result().getTotalRecords());
                result.complete(collectionsCollection);
              } else {
                result.fail(ar.cause());
                return;
              }
            });
    return result.future();
  }

  @Override
  public Future<FincSelectMetadataCollection> getById(
      String id, String isil, Context vertxContext) {
    Promise<FincSelectMetadataCollection> result = Promise.promise();

    metadataCollectionsDAO
        .getById(id, vertxContext)
        .onComplete(
            ar -> {
              if (ar.succeeded()) {
                FincSelectMetadataCollection fincSelectMetadataCollection =
                    transformer.transformEntry(ar.result(), isil);
                result.complete(fincSelectMetadataCollection);
              } else {
                result.fail("Cannot get finc select metadata collection by id. " + ar.cause());
                return;
              }
            });
    return result.future();
  }
}
