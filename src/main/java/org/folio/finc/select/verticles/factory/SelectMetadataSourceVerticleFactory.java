package org.folio.finc.select.verticles.factory;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import org.folio.finc.select.verticles.AbstractSelectMetadataSourceVerticle;
import org.folio.finc.select.verticles.SelectMetadataSourceVerticle;
import org.folio.finc.select.verticles.UnselectMetadataSourceVerticle;
import org.folio.rest.jaxrs.model.Select;

public class SelectMetadataSourceVerticleFactory {

  private SelectMetadataSourceVerticleFactory() {}

  public static AbstractSelectMetadataSourceVerticle create(
      Vertx vertx, Context context, Select select) {
    boolean doSelect = select.getSelect();
    if (doSelect) {
      return new SelectMetadataSourceVerticle(vertx, context);
    } else {
      return new UnselectMetadataSourceVerticle(vertx, context);
    }
  }
}
