package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Future;
import java.util.List;
import org.folio.rest.jaxrs.model.TinyMetadataSource;
import org.folio.rest.jaxrs.model.TinyMetadataSources;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.utils.Constants;

public class MetadataSourcesTinyDAOImpl implements MetadataSourcesTinyDAO {

  @Override
  public Future<TinyMetadataSources> getAll(Context vertxContext) {

    Future<TinyMetadataSources> result = Future.future();

    String tenantId = Constants.MODULE_TENANT;
    PostgresClient.getInstance(vertxContext.owner(), tenantId)
        .get(
            TABLE_NAME,
            TinyMetadataSource.class,
            "",
            false,
            false,
            reply -> {
              if (reply.succeeded()) {
                TinyMetadataSources tinySourcesCollection = new TinyMetadataSources();
                List<TinyMetadataSource> sources = reply.result().getResults();
                tinySourcesCollection.setTinyMetadataSources(sources);
                tinySourcesCollection.setTotalRecords(
                    reply.result().getResultInfo().getTotalRecords());
                result.complete(tinySourcesCollection);
              } else {
                result.fail(reply.cause());
              }
            });
    return result;
  }
}
