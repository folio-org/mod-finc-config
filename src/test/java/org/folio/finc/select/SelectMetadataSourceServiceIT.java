package org.folio.finc.select;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.finc.select.services.SelectMetadataSourceService;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.utils.Constants;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class SelectMetadataSourceServiceIT extends MetadataSourceServiceTestBase {

  @Rule public Timeout timeout = Timeout.seconds(10);

  @Test
  public void testSuccessfulSelect(TestContext context) {
    Async async = context.async();
    SelectMetadataSourceService service =
        new SelectMetadataSourceService(vertx.getOrCreateContext());
    service
        .selectAllCollections(metadataSource2.getId(), TENANT_UBL)
        .onComplete(
            aVoid -> {
              if (aVoid.succeeded()) {
                try {
                  Criteria labelCrit =
                      new Criteria()
                          .addField("'label'")
                          .setJSONB(true)
                          .setOperation("=")
                          .setVal(metadataCollection3.getLabel());
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
                                context.assertTrue(collection.getSelectedBy().contains("DE-15"));
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

  @Test
  public void testNoSelect(TestContext context) {
    Async async = context.async();
    SelectMetadataSourceService service =
        new SelectMetadataSourceService(vertx.getOrCreateContext());
    service
        .selectAllCollections(metadataSource2.getId(), TENANT_UBL)
        .onComplete(
            aVoid -> {
              if (aVoid.succeeded()) {
                try {
                  Criteria labelCrit =
                      new Criteria()
                          .addField("'label'")
                          .setJSONB(true)
                          .setOperation("=")
                          .setVal(metadataCollection2.getLabel());
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
                                context.assertFalse(collection.getSelectedBy().contains("DE-15"));
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
