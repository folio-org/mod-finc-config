package org.folio.finc.select.isil.filter;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.folio.rest.jaxrs.model.FincConfigMetadataSource;
import org.folio.rest.jaxrs.model.FincSelectMetadataSource;
import org.folio.rest.jaxrs.model.FincSelectMetadataSource.Selected;
import org.folio.rest.jaxrs.model.SelectedBy;
import org.junit.Before;
import org.junit.Test;

public class MetadataSourcesIsilFilterTest {

  private static final String SOURCE_1 = "Source 1";
  private static final String SOURCE_2 = "Source 2";
  private static final String DE_14 = "DE-14";
  private static final String DE_15 = "DE-15";

  private IsilFilter<FincSelectMetadataSource, FincConfigMetadataSource> isilFilter;

  @Before
  public void setUp() {
    isilFilter = new MetadataSourcesIsilFilter();
  }

  @Test
  public void testFilterForIsil() {
    FincConfigMetadataSource source1 = new FincConfigMetadataSource();
    source1.setLabel(SOURCE_1);
    source1.setId("uuid-1234");

    List<SelectedBy> selectedBy1 = new ArrayList<>();
    selectedBy1.add(new SelectedBy().withIsil(DE_15).withSelected(SelectedBy.Selected.ALL));
    selectedBy1.add(new SelectedBy().withIsil(DE_14).withSelected(SelectedBy.Selected.ALL));

    source1.setSelectedBy(selectedBy1);

    FincConfigMetadataSource source2 = new FincConfigMetadataSource();
    source2.setLabel(SOURCE_2);
    source2.setId("uuid-6789");
    List<SelectedBy> selectedBy2 = new ArrayList<>();
    selectedBy1.add(new SelectedBy().withIsil(DE_15).withSelected(SelectedBy.Selected.NONE));
    selectedBy1.add(new SelectedBy().withIsil(DE_14).withSelected(SelectedBy.Selected.ALL));
    source2.setSelectedBy(selectedBy2);

    List<FincConfigMetadataSource> sources = new ArrayList<>();
    sources.add(source1);
    sources.add(source2);

    List<FincSelectMetadataSource> transformed = isilFilter.filterForIsil(sources, DE_15);
    transformed.stream()
        .forEach(
            mdSource -> {
              String label = mdSource.getLabel();
              switch (label) {
                case SOURCE_1:
                  assertEquals(Selected.ALL, mdSource.getSelected());
                  break;
                case SOURCE_2:
                  assertEquals(Selected.NONE, mdSource.getSelected());
                  break;
              }
            });
  }
}
