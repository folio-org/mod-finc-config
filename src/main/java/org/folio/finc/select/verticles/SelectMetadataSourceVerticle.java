package org.folio.finc.select.verticles;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import java.util.List;
import java.util.stream.Collectors;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;

public class SelectMetadataSourceVerticle extends AbstractSelectMetadataSourceVerticle {

  public SelectMetadataSourceVerticle(Vertx vertx, Context ctx) {
    super(vertx, ctx);
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
        .collect(Collectors.toList());
  }
}
