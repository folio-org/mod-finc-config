package org.folio.finc.select.isil.filter;

import io.vertx.core.json.Json;
import java.util.List;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.jaxrs.model.FincSelectMetadataCollection;
import org.folio.rest.jaxrs.model.FincSelectMetadataCollection.Permitted;
import org.folio.rest.jaxrs.model.FincSelectMetadataCollection.Selected;

public class MetadataCollectionIsilFilter
    extends IsilFilter<FincSelectMetadataCollection, FincConfigMetadataCollection> {

  @Override
  public FincSelectMetadataCollection filterForIsil(
      FincConfigMetadataCollection entry, String isil) {
    List<String> selectedBy = entry.getSelectedBy();
    Selected selected = selectedBy.contains(isil) ? Selected.YES : Selected.NO;

    entry.setSelectedBy(null);

    List<String> permittedFor = entry.getPermittedFor();
    Permitted permitted = permittedFor.contains(isil) ? Permitted.YES : Permitted.NO;
    entry.setPermittedFor(null);

    FincSelectMetadataCollection result =
        Json.mapper.convertValue(entry, FincSelectMetadataCollection.class);
    result.setSelected(selected);
    result.setPermitted(permitted);
    return result;
  }
}
