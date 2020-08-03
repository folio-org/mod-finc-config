package org.folio.finc.select.transform;

import io.vertx.core.json.jackson.DatabindCodec;
import java.util.List;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection.UsageRestricted;
import org.folio.rest.jaxrs.model.FincSelectMetadataCollection;
import org.folio.rest.jaxrs.model.FincSelectMetadataCollection.Permitted;
import org.folio.rest.jaxrs.model.FincSelectMetadataCollection.Selected;

public class MetadataCollectionTransformer
    implements Transformer<FincSelectMetadataCollection, FincConfigMetadataCollection> {

  @Override
  public FincSelectMetadataCollection transformEntry(
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

    FincSelectMetadataCollection result =
        DatabindCodec.mapper().convertValue(entry, FincSelectMetadataCollection.class);

    result.setSelected(selected);
    result.setPermitted(permitted);
    return result;
  }
}
