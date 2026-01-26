package org.folio.finc.select.services.factory;

import io.vertx.core.Context;
import org.folio.finc.select.services.AbstractSelectMetadataSourceService;
import org.folio.finc.select.services.SelectMetadataSourceService;
import org.folio.finc.select.services.UnselectMetadataSourceService;
import org.folio.rest.jaxrs.model.Select;

public class SelectMetadataSourceServiceFactory {

  private SelectMetadataSourceServiceFactory() {}

  public static AbstractSelectMetadataSourceService create(Context context, Select select) {
    boolean doSelect = select.getSelect();
    if (doSelect) {
      return new SelectMetadataSourceService(context);
    } else {
      return new UnselectMetadataSourceService(context);
    }
  }
}
