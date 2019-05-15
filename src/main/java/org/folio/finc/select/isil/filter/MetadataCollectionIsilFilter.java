package org.folio.finc.select.isil.filter;

import io.vertx.core.json.Json;
import java.util.List;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.jaxrs.model.FincSelectMetadataCollection;

public class MetadataCollectionIsilFilter
    extends IsilFilter<FincSelectMetadataCollection, FincConfigMetadataCollection> {

  @Override
  public FincSelectMetadataCollection filterForIsil(
      FincConfigMetadataCollection entry, String isil) {
    List<String> selectedBy = entry.getSelectedBy();
    boolean selected = selectedBy.contains(isil);
    entry.setSelectedBy(null);

    List<String> permittedFor = entry.getPermittedFor();
    boolean permitted = permittedFor.contains(isil);
    entry.setPermittedFor(null);

    FincSelectMetadataCollection result =
      Json.mapper.convertValue(entry, FincSelectMetadataCollection.class);
    result.setSelected(selected);
    result.setPermitted(permitted);
    return result;
  }
}
