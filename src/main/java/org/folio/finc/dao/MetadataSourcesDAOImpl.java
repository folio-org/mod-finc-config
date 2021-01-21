package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.cql2pgjson.CQL2PgJSON;
import org.folio.cql2pgjson.exception.FieldException;
import org.folio.rest.impl.FincConfigMetadataSourcesAPI;
import org.folio.rest.jaxrs.model.Contact;
import org.folio.rest.jaxrs.model.Contacts;
import org.folio.rest.jaxrs.model.FincConfigMetadataSource;
import org.folio.rest.jaxrs.model.FincConfigMetadataSources;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.PgExceptionUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.utils.Constants;

import java.util.Collections;
import java.util.List;

public class MetadataSourcesDAOImpl implements MetadataSourcesDAO {

  private static final Logger logger = LogManager.getLogger(MetadataSourcesDAOImpl.class);

  private static final String TABLE_NAME = "metadata_sources";
  private static final String TABLE_NAME_CONTACTS = "metadata_sources_contacts";

  private CQLWrapper getCQL(String query, int limit, int offset) throws FieldException {
    CQL2PgJSON cql2PgJSON =
        new CQL2PgJSON(
            Collections.singletonList(FincConfigMetadataSourcesAPI.TABLE_NAME + ".jsonb"));
    return new CQLWrapper(cql2PgJSON, query)
        .setLimit(new Limit(limit))
        .setOffset(new Offset(offset));
  }

  @Override
  public Future<FincConfigMetadataSources> getAll(
      String query, int offset, int limit, Context vertxContext) {

    Promise<FincConfigMetadataSources> result = Promise.promise();

    String tenantId = Constants.MODULE_TENANT;
    String field = "*";
    String[] fieldList = {field};
    CQLWrapper cql = null;
    try {
      cql = getCQL(query, limit, offset);
    } catch (FieldException e) {
      logger.error("Error while processing CQL {}", PgExceptionUtil.getMessage(e));
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
                sourcesCollection.setTotalRecords(reply.result().getResultInfo().getTotalRecords());
                result.complete(sourcesCollection);
              } else {
                result.fail("Cannot get finc config metadata sources. " + reply.cause());
              }
            });
    return result.future();
  }

  @Override
  public Future<FincConfigMetadataSource> getById(String id, Context vertxContext) {
    Promise<FincConfigMetadataSource> result = Promise.promise();

    String tenantId = Constants.MODULE_TENANT;
    PostgresClient.getInstance(vertxContext.owner(), tenantId)
        .getById(
            TABLE_NAME,
            id,
            FincConfigMetadataSource.class,
            reply -> {
              if (reply.succeeded()) {
                result.complete(reply.result());
              } else {
                result.fail("Cannot get finc config metadatasource by id. " + reply.cause());
              }
            });

    return result.future();
  }

  @Override
  public Future<Contacts> getContacts(Context vertxContext) {
    Promise<Contacts> result = Promise.promise();

    PostgresClient.getInstance(vertxContext.owner(), Constants.MODULE_TENANT)
        .get(
            TABLE_NAME_CONTACTS,
            Contact.class,
            new Criterion(),
            true,
            reply -> {
              if (reply.succeeded()) {
                List<Contact> results = reply.result().getResults();
                Contacts contacts = new Contacts();
                contacts.setContacts(results);
                contacts.setTotalRecords(results.size());
                result.complete(contacts);
              } else {
                result.fail(reply.cause());
              }
            });
    return result.future();
  }
}
