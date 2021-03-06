package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import java.util.List;
import java.util.stream.Collectors;
import org.folio.rest.jaxrs.model.TinyMetadataSource;
import org.folio.rest.jaxrs.model.TinyMetadataSources;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.utils.Constants;

public class MetadataSourcesTinyDAOImpl implements MetadataSourcesTinyDAO {

  private static final String TABLE_NAME = "metadata_sources_tiny";

  @Override
  public Future<TinyMetadataSources> getAll(Context vertxContext) {

    Promise<TinyMetadataSources> result = Promise.promise();
    String tenantId = Constants.MODULE_TENANT;

    PostgresClient.getInstance(vertxContext.owner(), tenantId)
        .get(
            TABLE_NAME,
            TinyMetadataSource.class,
            new Criterion(),
            false,
            reply -> {
              if (reply.succeeded()) {
                TinyMetadataSources tinySourcesCollection = new TinyMetadataSources();
                List<TinyMetadataSource> sources = reply.result().getResults();

                // as we cannot use CQL here (we are querying from a custom view), we need to sort by hand
                List<TinyMetadataSource> sourcesSorted = sources.stream()
                    .sorted((o1, o2) -> o1.getLabel().compareToIgnoreCase(o2.getLabel()))
                    .collect(Collectors.toList());
                tinySourcesCollection.setTinyMetadataSources(sourcesSorted);
                tinySourcesCollection.setTotalRecords(
                    reply.result().getResultInfo().getTotalRecords());
                result.complete(tinySourcesCollection);
              } else {
                result.fail(reply.cause());
              }
            });
    return result.future();
  }
}
