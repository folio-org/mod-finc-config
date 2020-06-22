package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.folio.rest.jaxrs.model.Credential;

public class SelectEZBCredentialsDAOImpl implements SelectEZBCredentialsDAO {

  private final EZBCredentialsDAO ezbCredentialsDAO;

  public SelectEZBCredentialsDAOImpl() {
    super();
    ezbCredentialsDAO = new EZBCredentialsDAOImpl();
  }

  @Override
  public Future<Credential> getByIsil(String isil, Context ctx) {
    return ezbCredentialsDAO.getByIsil(isil, ctx);
  }

  @Override
  public Future<Credential> upsert(Credential entity, Context ctx) {
    Promise<Credential> result = Promise.promise();
    ezbCredentialsDAO.getByIsil(entity.getIsil(), ctx)
        .compose(credential -> {
          if (credential == null) {
            return ezbCredentialsDAO.insert(entity, ctx);
          } else {
            return ezbCredentialsDAO.deleteByIsil(entity.getIsil(), ctx)
                .compose(
                    integer -> ezbCredentialsDAO.insert(entity, ctx)
                );
          }
        }).onComplete(ar -> {
      if (ar.succeeded()) {
        result.complete(entity);
      } else {
        result.fail(ar.cause());
      }
    });
    return result.future();
  }
}
