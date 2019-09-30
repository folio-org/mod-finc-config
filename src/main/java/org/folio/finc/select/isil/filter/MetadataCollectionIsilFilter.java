package org.folio.finc.select.isil.filter;

import io.vertx.core.json.Json;
import java.util.List;
import java.util.stream.Collectors;
import org.folio.rest.jaxrs.model.Filter;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection.UsageRestricted;
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

    Permitted permitted;
    if (entry.getUsageRestricted().equals(UsageRestricted.NO)) {
      permitted = Permitted.YES;
    } else {
      List<String> permittedFor = entry.getPermittedFor();
      permitted = permittedFor.contains(isil) ? Permitted.YES : Permitted.NO;
    }
    entry.setPermittedFor(null);

    List<Filter> filters = entry.getFilters();
    List<String> filterIds =
        filters.stream()
            .filter(f -> f.getIsil().equals(isil))
            .map(Filter::getFilters)
            .flatMap(List::stream)
            .collect(Collectors.toList());
    entry.setFilters(null);

    FincSelectMetadataCollection result =
        Json.mapper.convertValue(entry, FincSelectMetadataCollection.class);
    result.setSelected(selected);
    result.setPermitted(permitted);
    result.setFilters(filterIds);
    return result;
  }
}
