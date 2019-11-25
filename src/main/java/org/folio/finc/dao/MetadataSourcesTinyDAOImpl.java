package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Promise;
import java.util.List;
import org.folio.rest.jaxrs.model.TinyMetadataSource;
import org.folio.rest.jaxrs.model.TinyMetadataSources;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.utils.Constants;

public class MetadataSourcesTinyDAOImpl implements MetadataSourcesTinyDAO {

  private static final String TABLE_NAME = "metadata_sources_tiny";

  @Override
  public Promise<TinyMetadataSources> getAll(Context vertxContext) {

    Promise<TinyMetadataSources> result = Promise.promise();

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
