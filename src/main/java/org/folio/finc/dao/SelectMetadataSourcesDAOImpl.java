package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Promise;
import java.util.List;
import org.folio.finc.select.isil.filter.IsilFilter;
import org.folio.finc.select.isil.filter.MetadataSourcesIsilFilter;
import org.folio.rest.jaxrs.model.FincConfigMetadataSource;
import org.folio.rest.jaxrs.model.FincConfigMetadataSources;
import org.folio.rest.jaxrs.model.FincSelectMetadataSource;
import org.folio.rest.jaxrs.model.FincSelectMetadataSources;

public class SelectMetadataSourcesDAOImpl implements SelectMetadataSourcesDAO {

  private final MetadataSourcesDAO metadataSourcesDAO;
  private final IsilFilter<FincSelectMetadataSource, FincConfigMetadataSource> isilFilter;

  public SelectMetadataSourcesDAOImpl() {
    super();
    this.metadataSourcesDAO = new MetadataSourcesDAOImpl();
    this.isilFilter = new MetadataSourcesIsilFilter();
  }

  @Override
  public Promise<FincSelectMetadataSources> getAll(
      String query, int offset, int limit, String isil, Context vertxContext) {
    Promise<FincSelectMetadataSources> result = Promise.promise();

    metadataSourcesDAO
        .getAll(query, offset, limit, vertxContext)
        .future()
        .setHandler(
            ar -> {
              if (ar.succeeded()) {
                FincConfigMetadataSources fincConfigMetadataSources = ar.result();
                FincSelectMetadataSources sourcesCollection = new FincSelectMetadataSources();
                List<FincSelectMetadataSource> transformedSources =
                    isilFilter.filterForIsil(
                        fincConfigMetadataSources.getFincConfigMetadataSources(), isil);
                sourcesCollection.setFincSelectMetadataSources(transformedSources);
                sourcesCollection.setTotalRecords(transformedSources.size());
                result.complete(sourcesCollection);

              } else {
                result.fail(ar.cause());
              }
            });
    return result;
  }

  @Override
  public Promise<FincSelectMetadataSource> getById(String id, String isil, Context vertxContext) {
    Promise<FincSelectMetadataSource> result = Promise.promise();

    metadataSourcesDAO
        .getById(id, vertxContext)
        .future()
        .setHandler(
            ar -> {
              if (ar.succeeded()) {
                FincConfigMetadataSource fincConfigMetadataSource = ar.result();
                if (fincConfigMetadataSource == null) {
                  result.complete(null);
                } else {
                  FincSelectMetadataSource fincSelectMetadataSource =
                      isilFilter.filterForIsil(fincConfigMetadataSource, isil);
                  result.complete(fincSelectMetadataSource);
                }
              } else {
                result.fail("Cannot get finc select metadata source by id. " + ar.cause());
              }
            });

    return result;
  }
}
