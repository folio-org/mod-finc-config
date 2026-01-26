package org.folio.finc.select;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.finc.select.services.UnselectMetadataSourceService;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.utils.Constants;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class UnselectMetadataSourceServiceIT extends MetadataSourceServiceTestBase {

  @Rule public Timeout timeout = Timeout.seconds(10);

  @Test
  public void testSuccessfulUnSelect(TestContext context) {
    Async async = context.async();
    UnselectMetadataSourceService service =
        new UnselectMetadataSourceService(vertx.getOrCreateContext());
    service
        .selectAllCollections(metadataSource1.getId(), TENANT_UBL)
        .onComplete(
            aVoid -> {
              if (aVoid.succeeded()) {
                try {
                  Criteria labelCrit =
                      new Criteria()
                          .addField("'label'")
                          .setJSONB(true)
                          .setOperation("=")
                          .setVal(metadataCollection1.getLabel());
                  Criterion criterion = new Criterion(labelCrit);
                  PostgresClient.getInstance(vertx, Constants.MODULE_TENANT)
                      .get(
                          "metadata_collections",
                          FincConfigMetadataCollection.class,
                          criterion,
                          true,
                          true,
                          ar -> {
                            if (ar.succeeded()) {
                              if (ar.result() != null) {
                                FincConfigMetadataCollection collection =
                                    ar.result().getResults().get(0);
                                if (collection == null) {
                                  context.fail("No results found.");
                                } else {
                                  context.assertFalse(collection.getSelectedBy().contains("DE-15"));
                                }
                              } else {
                                context.fail("No results found.");
                              }
                              async.complete();
                            } else {
                              context.fail(ar.cause().toString());
                            }
                          });
                } catch (Exception e) {
                  context.fail(e);
                }
              } else {
                context.fail();
              }
            });
  }
}
