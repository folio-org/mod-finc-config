package org.folio.finc.select.transform;

import java.util.List;
import java.util.stream.Collectors;

import io.vertx.core.json.jackson.DatabindCodec;
import org.folio.rest.jaxrs.model.FincConfigMetadataSource;
import org.folio.rest.jaxrs.model.FincSelectMetadataSource;
import org.folio.rest.jaxrs.model.FincSelectMetadataSource.Selected;
import org.folio.rest.jaxrs.model.SelectedBy;

public class MetadataSourcesTransformer
    implements Transformer<FincSelectMetadataSource, FincConfigMetadataSource> {

  @Override
  public FincSelectMetadataSource transformEntry(FincConfigMetadataSource entry, String isil) {
    List<SelectedBy> selectedBy = entry.getSelectedBy();
    List<SelectedBy> selectedByIsil =
        selectedBy.stream().filter(sb -> sb.getIsil().equals(isil)).collect(Collectors.toList());
    Selected selected;
    if (selectedByIsil.isEmpty()) {
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
        DatabindCodec.mapper().convertValue(entry, FincSelectMetadataSource.class);
    result.setSelected(selected);
    return result;
  }
}
