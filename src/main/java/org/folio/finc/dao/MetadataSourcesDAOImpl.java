package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.Arrays;
import java.util.List;
import org.folio.cql2pgjson.CQL2PgJSON;
import org.folio.cql2pgjson.exception.FieldException;
import org.folio.rest.impl.FincConfigMetadataSourcesAPI;
import org.folio.rest.jaxrs.model.FincConfigMetadataSource;
import org.folio.rest.jaxrs.model.FincConfigMetadataSources;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.utils.Constants;

public class MetadataSourcesDAOImpl implements MetadataSourcesDAO {

  private static final Logger logger = LoggerFactory.getLogger(MetadataSourcesDAOImpl.class);

  private static final String TABLE_NAME = "metadata_sources";

  private CQLWrapper getCQL(String query, int limit, int offset) throws FieldException {
    CQL2PgJSON cql2PgJSON =
      new CQL2PgJSON(Arrays.asList(FincConfigMetadataSourcesAPI.TABLE_NAME + ".jsonb"));
    return new CQLWrapper(cql2PgJSON, query)
      .setLimit(new Limit(limit))
      .setOffset(new Offset(offset));
  }

  @Override
  public Future<FincConfigMetadataSources> getAll(String query, int offset, int limit,
    Context vertxContext) {

    Future<FincConfigMetadataSources> result = Future.future();

    String tenantId = Constants.MODULE_TENANT;
    String field = "*";
    String[] fieldList = {field};
    CQLWrapper cql = null;
    try {
      cql = getCQL(query, limit, offset);
    } catch (FieldException e) {
      logger.error("Error while processing CQL " + e.getMessage());
      result.fail(e);
    }

    PostgresClient.getInstance(vertxContext.owner(), tenantId)
      .get(
        TABLE_NAME,
        FincConfigMetadataSource.class,
        fieldList,
        cql,
        true,
        false,
        reply -> {
            if (reply.succeeded()) {
              org.folio.rest.jaxrs.model.FincConfigMetadataSources sourcesCollection =
                new org.folio.rest.jaxrs.model.FincConfigMetadataSources();
              List<FincConfigMetadataSource> sources = reply.result().getResults();
              sourcesCollection.setFincConfigMetadataSources(sources);
              sourcesCollection.setTotalRecords(
                reply.result().getResultInfo().getTotalRecords());
              result.complete(sourcesCollection);
            } else {
              result.fail("Cannot get finc config metadata sources. " + reply.cause());
            }
        });
    return result;
  }

  @Override
  public Future<FincConfigMetadataSource> getById(String id, Context vertxContext) {
    Future<FincConfigMetadataSource> result = Future.future();

    String tenantId = Constants.MODULE_TENANT;
    PostgresClient.getInstance(vertxContext.owner(), tenantId)
      .getById(
        TABLE_NAME,
        id,
        FincConfigMetadataSource.class,
        result.setHandler(ar -> {
          if (ar.succeeded()) {
            FincConfigMetadataSource result1 = ar.result();
            result.complete(result1);
          } else {
            result.fail("Cannot find metadata source: " + id);
          }
        })
      );
    return result;
  }
}
