package org.folio.finc.select;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.finc.select.verticles.UnselectMetadataSourceVerticle;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.utils.Constants;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class UnselectMetadataSourceVerticleIT extends MetadataSourceVerticleTestBase {

  private static final UnselectMetadataSourceVerticle cut =
      new UnselectMetadataSourceVerticle(vertx, vertx.getOrCreateContext());
  @Rule public Timeout timeout = Timeout.seconds(10);

  @Before
  public void before2(TestContext context) {
    JsonObject cfg2 = vertx.getOrCreateContext().config();
    cfg2.put("tenantId", TENANT_UBL);
    cfg2.put("metadataSourceId", metadataSource2.getId());
    cfg2.put("testing", true);

    vertx
        .deployVerticle(cut, new DeploymentOptions().setConfig(cfg2))
        .onComplete(context.asyncAssertSuccess());
  }

  @Test
  public void testSuccessfulUnSelect(TestContext context) {
    Async async = context.async();
    cut.selectAllCollections(metadataSource1.getId(), TENANT_UBL)
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
