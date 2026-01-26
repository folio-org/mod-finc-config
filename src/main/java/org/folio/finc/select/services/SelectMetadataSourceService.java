package org.folio.finc.select.services;

import io.vertx.core.Context;
import java.util.List;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;

public class SelectMetadataSourceService extends AbstractSelectMetadataSourceService {

  public SelectMetadataSourceService(Context context) {
    super(context);
  }

  @Override
  List<FincConfigMetadataCollection> select(
      List<FincConfigMetadataCollection> metadataCollections, String isil) {
    return metadataCollections.stream()
        .filter(metadataCollection -> !metadataCollection.getSelectedBy().contains(isil))
        .map(
            metadataCollection -> {
              metadataCollection.getSelectedBy().add(isil);
              return metadataCollection;
            })
        .toList();
  }
}
