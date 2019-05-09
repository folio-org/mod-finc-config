package org.folio.finc.select;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.folio.rest.jaxrs.model.FincConfigMetadataSource;
import org.folio.rest.jaxrs.model.FincSelectMetadataSource;
import org.junit.Test;

public class MetadataSourcesHelperTest {

  private static final String SOURCE_1 = "Source 1";
  private static final String SOURCE_2 = "Source 2";
  private static final String DE_14 = "DE-14";
  private static final String DE_15 = "DE-15";

  @Test
  public void testFilterForIsil() {
    FincConfigMetadataSource source1 = new FincConfigMetadataSource();
    source1.setLabel(SOURCE_1);
    source1.setId("uuid-1234");
    List<String> selectedBy1 = new ArrayList<>();
    selectedBy1.add(DE_15);
    selectedBy1.add(DE_14);
    source1.setSelectedBy(selectedBy1);

    FincConfigMetadataSource source2 = new FincConfigMetadataSource();
    source2.setLabel(SOURCE_2);
    source2.setId("uuid-6789");
    List<String> selectedBy2 = new ArrayList<>();
    selectedBy2.add(DE_14);
    source2.setSelectedBy(selectedBy2);

    List<FincConfigMetadataSource> sources = new ArrayList<>();
    sources.add(source1);
    sources.add(source2);

    List<FincSelectMetadataSource> transformed =
        MetadataSourcesHelper.filterForIsil(sources, DE_15);
    transformed.stream()
        .forEach(
            mdSource -> {
              String label = mdSource.getLabel();
              switch (label) {
                case SOURCE_1:
                  assertTrue(mdSource.getSelected());
                  break;
                case SOURCE_2:
                  assertFalse(mdSource.getSelected());
                  break;
              }
            });
  }
}
