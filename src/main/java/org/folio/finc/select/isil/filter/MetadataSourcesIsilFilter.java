package org.folio.finc.select.isil.filter;

import io.vertx.core.json.Json;
import java.util.List;
import org.folio.rest.jaxrs.model.FincConfigMetadataSource;
import org.folio.rest.jaxrs.model.FincSelectMetadataSource;
import org.folio.rest.jaxrs.model.FincSelectMetadataSource.Selected;

public class MetadataSourcesIsilFilter
    extends IsilFilter<FincSelectMetadataSource, FincConfigMetadataSource> {

  @Override
  public FincSelectMetadataSource filterForIsil(FincConfigMetadataSource entry, String isil) {
    List<String> selectedBy = entry.getSelectedBy();
    Selected selected = selectedBy.contains(isil) ? Selected.YES : Selected.NO;
    entry.setSelectedBy(null);

    FincSelectMetadataSource result =
        Json.mapper.convertValue(entry, FincSelectMetadataSource.class);
    result.setSelected(selected);
    return result;
  }
}
