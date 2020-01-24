package org.folio.finc.select.isil.filter;

import io.vertx.core.json.Json;
import java.util.List;
import java.util.stream.Collectors;
import org.folio.rest.jaxrs.model.FincConfigMetadataSource;
import org.folio.rest.jaxrs.model.FincSelectMetadataSource;
import org.folio.rest.jaxrs.model.FincSelectMetadataSource.Selected;
import org.folio.rest.jaxrs.model.SelectedBy;

public class MetadataSourcesIsilFilter
    extends IsilFilter<FincSelectMetadataSource, FincConfigMetadataSource> {

  @Override
  public FincSelectMetadataSource filterForIsil(FincConfigMetadataSource entry, String isil) {
    List<SelectedBy> selectedBy = entry.getSelectedBy();
    List<SelectedBy> selectedByIsil =
        selectedBy.stream().filter(sb -> sb.getIsil().equals(isil)).collect(Collectors.toList());
    Selected selected;
    if (selectedByIsil.size() == 0) {
      selected = Selected.NONE;
    } else {
      SelectedBy.Selected currentSelected = selectedByIsil.get(0).getSelected();

      if (Selected.ALL.value().equals(currentSelected.value())) {
        selected = Selected.ALL;
      } else if (Selected.SOME.value().equals(currentSelected.value())) {
        selected = Selected.SOME;
      } else {
        selected = Selected.NONE;
      }
    }

    entry.setSelectedBy(null);
    FincSelectMetadataSource result =
        Json.mapper.convertValue(entry, FincSelectMetadataSource.class);
    result.setSelected(selected);
    return result;
  }
}
