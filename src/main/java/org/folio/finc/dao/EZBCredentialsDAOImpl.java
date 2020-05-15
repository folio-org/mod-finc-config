package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import java.util.Collections;
import java.util.List;
import org.folio.cql2pgjson.CQL2PgJSON;
import org.folio.cql2pgjson.exception.FieldException;
import org.folio.rest.jaxrs.model.Credential;
import org.folio.rest.jaxrs.model.Credentials;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.utils.Constants;

public class EZBCredentialsDAOImpl implements EZBCredentialsDAO {

  private static final String TABLE_NAME = "ezb_credentials";

  @Override
  public Future<Credentials> getAll(String query, int offset, int limit, Context vertxContext) {
    Promise<Credentials> result = Promise.promise();
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
            Credential.class,
            fieldList,
            cql,
            true,
            false,
            reply -> {
              if (reply.succeeded()) {
                List<Credential> results = reply.result().getResults();
                Credentials credentials = new Credentials();
                credentials.setCredentials(results);
                credentials.setTotalRecords(reply.result().getResultInfo().getTotalRecords());
                result.complete(credentials);
              } else {
                result.fail("Cannot get credentials. " + reply.cause());
              }
            });

    return result.future();
  }

  @Override
  public Future<Credential> getByIsil(String isil, Context vertxContext) {
    Promise<Credential> result = Promise.promise();
    String query = String.format("isil=%s", isil);
    getAll(query, 0, 1, vertxContext)
        .setHandler(ar -> {
          if (ar.succeeded()) {
            if (ar.result().getTotalRecords() == 0) {
              result.complete();
              return;
            }
            Credential cred = ar.result().getCredentials().get(0);
            result.complete(cred);
          } else {
            result.fail(ar.cause());
          }
        });
    return result.future();
  }

  @Override
  public Future<Credential> insert(Credential entity, Context vertxContext) {
    Promise<Credential> result = Promise.promise();
    getByIsil(entity.getIsil(), vertxContext)
        .setHandler(ar -> {
          if (ar.succeeded()) {
            Credential credential = ar.result();
            if (credential != null) {
              result.fail(new EZBCredentialsException("Isil must be unique."));
            } else {
              PostgresClient.getInstance(vertxContext.owner(), Constants.MODULE_TENANT)
                  .save(TABLE_NAME, entity, reply -> {
                    if (reply.succeeded()) {
                      result.complete(entity);
                    } else {
                      result.fail(reply.cause());
                    }
                  });
            }
          }
        });

    return result.future();
  }

  @Override
  public Future<Integer> deleteByIsil(String isil, Context vertxContext) {
    Promise<Integer> result = Promise.promise();
    getByIsil(isil, vertxContext)
        .setHandler(ar -> {
          if (ar.succeeded()) {
            Credential credential = ar.result();
            if (credential == null) {
              result.fail("Cannot delete credential. Isil not found.");
            } else {
              PostgresClient.getInstance(vertxContext.owner(), Constants.MODULE_TENANT)
                  .delete(TABLE_NAME, credential.getId(), reply -> {
                    if (reply.succeeded()) {
                      result.complete(1);
                    } else {
                      result.fail(reply.cause());
                    }
                  });
            }
          }
        });
    return result.future();
  }

  private CQLWrapper getCQL(String query, int limit, int offset) throws FieldException {
    CQL2PgJSON cql2PgJSON = new CQL2PgJSON(Collections.singletonList(TABLE_NAME + ".jsonb"));
    return new CQLWrapper(cql2PgJSON, query)
        .setLimit(new Limit(limit))
        .setOffset(new Offset(offset));
  }

  public static class EZBCredentialsException extends RuntimeException {

    public EZBCredentialsException(String message) {
      super(message);
    }
  }
}
