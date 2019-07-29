package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Future;
import java.util.Arrays;
import java.util.List;
import org.folio.cql2pgjson.CQL2PgJSON;
import org.folio.cql2pgjson.exception.FieldException;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollections;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.utils.Constants;

public class MetadataCollectionsDAOImpl implements MetadataCollectionsDAO {

  private static final String TABLE_NAME = "metadata_collections";

  @Override
  public Future<FincConfigMetadataCollections> getAll(String query, int offset, int limit,
    Context vertxContext) {
    Future<FincConfigMetadataCollections> result = Future.future();
    String tenantId = Constants.MODULE_TENANT;
    String field = "*";
    String[] fieldList = {field};

    CQLWrapper cql = null;
    try {
      cql = getCQL(query, limit, offset);
    } catch (FieldException e) {
      result.fail(e);
    }

    PostgresClient.getInstance(vertxContext.owner(), tenantId)
      .get(
        TABLE_NAME,
        FincConfigMetadataCollection.class,
        fieldList,
        cql,
        true,
        false,
        reply -> {
          if (reply.succeeded()) {
            FincConfigMetadataCollections
              collectionsCollection =
              new FincConfigMetadataCollections();

            List<FincConfigMetadataCollection> results =
              reply.result().getResults();
            collectionsCollection.setFincConfigMetadataCollections(results);
            collectionsCollection.setTotalRecords(
              reply.result().getResultInfo().getTotalRecords());
            result.complete(collectionsCollection);
          } else {
            result.fail("Cannot get metadata collections. " + reply.cause());
          }
        });

    return result;
  }

  @Override
  public Future<FincConfigMetadataCollection> getById(String id, Context vertxContext) {
    Future<FincConfigMetadataCollection> result = Future.future();

    String tenantId = Constants.MODULE_TENANT;
    PostgresClient.getInstance(vertxContext.owner(), tenantId)
      .getById(
        TABLE_NAME,
        id,
        FincConfigMetadataCollection.class,
        reply -> {
          if (reply.succeeded()) {
            FincConfigMetadataCollection metadataCollection = reply.result();
            result.complete(metadataCollection);
          } else {
            result.fail("Cannot get metadata collection by id. " + reply.cause());
          }
        }
      );
    return result;
  }

  @Override
  public Future<FincConfigMetadataCollection> update(FincConfigMetadataCollection entity, String id,
    Context vertxContext) {
    Future<FincConfigMetadataCollection> result = Future.future();
    String tenantId = Constants.MODULE_TENANT;
    PostgresClient.getInstance(vertxContext.owner(), tenantId)
      .update(
        TABLE_NAME,
        entity,
        id,
        ar -> {
          if (ar.succeeded()) {
            result.complete(entity);
          } else {
            result.fail("Cannot update metadata collection: " + ar.cause());
          }
        }
      );
    return result;
  }

  private CQLWrapper getCQL(String query, int limit, int offset) throws FieldException {
    CQL2PgJSON cql2PgJSON = new CQL2PgJSON(Arrays.asList(TABLE_NAME + ".jsonb"));
    return new CQLWrapper(cql2PgJSON, query)
      .setLimit(new Limit(limit))
      .setOffset(new Offset(offset));
  }
}
