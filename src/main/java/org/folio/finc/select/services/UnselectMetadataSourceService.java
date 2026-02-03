package org.folio.finc.select.services;

import io.vertx.core.Context;
import java.util.List;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;

public class UnselectMetadataSourceService extends AbstractSelectMetadataSourceService {

  public UnselectMetadataSourceService(Context context) {
    super(context);
  }

  @Override
  List<FincConfigMetadataCollection> select(
      List<FincConfigMetadataCollection> metadataCollections, String isil) {
    return metadataCollections.stream()
        .filter(metadataCollection -> metadataCollection.getSelectedBy().remove(isil))
        .toList();
  }
}
