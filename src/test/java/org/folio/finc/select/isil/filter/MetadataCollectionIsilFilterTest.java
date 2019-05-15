package org.folio.finc.select.isil.filter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.folio.finc.select.isil.filter.IsilFilter;
import org.folio.finc.select.isil.filter.MetadataCollectionIsilFilter;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection.UsageRestricted;
import org.folio.rest.jaxrs.model.FincSelectMetadataCollection;
import org.junit.Before;
import org.junit.Test;

public class MetadataCollectionIsilFilterTest {

  private static final String COLLECTION_1 = "Collection 1";
  private static final String COLLECTION_2 = "Collection 2";
  private static final String DE_14 = "DE-14";
  private static final String DE_15 = "DE-15";

  private IsilFilter<FincSelectMetadataCollection, FincConfigMetadataCollection> isilFilter;

  @Before
  public void setUp() {
    this.isilFilter = new MetadataCollectionIsilFilter();
  }

  @Test
  public void testTransform() {
    FincConfigMetadataCollection collection1 = new FincConfigMetadataCollection();
    collection1.setId("uuid-1234");
    collection1.setLabel(COLLECTION_1);
    collection1.setUsageRestricted(UsageRestricted.NO);
    collection1.setSolrMegaCollections(Arrays.asList());
    List<String> permittedFor = new ArrayList<>();
    permittedFor.add(DE_15);
    permittedFor.add(DE_14);
    collection1.setPermittedFor(permittedFor);
    collection1.setSelectedBy(permittedFor);

    FincConfigMetadataCollection collection2 = new FincConfigMetadataCollection();
    collection2.setId("uuid-6789");
    collection2.setLabel(COLLECTION_2);
    collection2.setUsageRestricted(UsageRestricted.YES);
    collection2.setSolrMegaCollections(Arrays.asList());
    List<String> permittedFor2 = new ArrayList<>();
    permittedFor2.add(DE_14);
    collection2.setPermittedFor(permittedFor2);
    collection2.setSelectedBy(permittedFor2);

    List<FincConfigMetadataCollection> collections = new ArrayList<>();
    collections.add(collection1);
    collections.add(collection2);

    List<FincSelectMetadataCollection> transformed = isilFilter.filterForIsil(collections, DE_15);
    transformed.stream()
        .forEach(
            mdCollection -> {
              String label = mdCollection.getLabel();
              switch (label) {
                case COLLECTION_1:
                  assertTrue(mdCollection.getSelected());
                  break;
                case COLLECTION_2:
                  assertFalse(mdCollection.getSelected());
                  break;
              }
            });
  }
}
