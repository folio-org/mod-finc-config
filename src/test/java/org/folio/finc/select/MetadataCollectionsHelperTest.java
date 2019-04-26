package org.folio.finc.select;

import static org.junit.Assert.*;

import io.vertx.core.Vertx;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.folio.rest.jaxrs.model.FincSelectMetadataSourcesGetOrder;
import org.folio.rest.jaxrs.model.MetadataCollection;
import org.folio.rest.jaxrs.model.MetadataCollection.UsageRestricted;
import org.junit.Test;

public class MetadataCollectionsHelperTest {

  @Test
  public void transform() {
    MetadataCollection collection1 = new MetadataCollection();
    collection1.setId("uuid-1234");
    collection1.setLabel("Collection 1");
    collection1.setUsageRestricted(UsageRestricted.NO);
    collection1.setSolrMegaCollections(Arrays.asList());
    List<String> permittedFor = new ArrayList<>();
    permittedFor.add("DE-15");
    permittedFor.add("DE-14");
    collection1.setPermittedFor(permittedFor);
    collection1.setSelectedBy(permittedFor);

    MetadataCollection collection2 = new MetadataCollection();
    collection2.setId("uuid-6789");
    collection2.setLabel("Collection 2");
    collection2.setUsageRestricted(UsageRestricted.YES);
    collection2.setSolrMegaCollections(Arrays.asList());
    List<String> permittedFor2 = new ArrayList<>();
    permittedFor2.add("DE-14");
    collection2.setPermittedFor(permittedFor2);
    collection2.setSelectedBy(permittedFor2);


    List<MetadataCollection> collections = new ArrayList<>();
    collections.add(collection1);
    collections.add(collection2);

    MetadataCollectionsHelper cut = new MetadataCollectionsHelper(Vertx.vertx(), "diku");

    cut.transform(collections, "diku");

  }
}