package org.folio.finc.periodic;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.finc.mocks.EZBServiceMock;
import org.folio.rest.jaxrs.model.Credential;
import org.folio.rest.persist.PostgresClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class EZBHarvestJobWithoutFilterITest extends AbstractEZBHarvestJobTest {

  @Before
  public void setUp(TestContext context) {
    vertx = Vertx.vertx();
    vertxContext = vertx.getOrCreateContext();
    Async async = context.async();
    try {
      PostgresClient.getInstance(vertx).startEmbeddedPostgres();

      createSchema(tenant)
          .compose(s -> insertIsil(tenant))
          .onComplete(
              ar -> {
                if (ar.succeeded()) {
                  async.complete();
                } else {
                  context.fail(ar.cause());
                }
              });
    } catch (Exception e) {
      context.fail(e);
    }
  }

  @After
  public void cleanUp(TestContext context) {
    PostgresClient.stopEmbeddedPostgres();
  }

  @Test
  public void checkThatFilterFileIsNotAdded(TestContext context) {
    // insert ezb credentials
    Async async = context.async(1);
    Credential credential = new Credential().withUser("user").withPassword("password")
        .withLibId("libId").withIsil(tenant);

    PostgresClient.getInstance(vertx, tenant)
        .save("ezb_credentials", credential, ar -> {
          if (ar.succeeded()) {
            EZBHarvestJob job = new EZBHarvestJob();
            job.setEZBService(new EZBServiceMock());
            job.run(vertxContext)
                .onComplete(ar2 -> {
                  if (ar2.succeeded()) {
                    getEZBFilter()
                        .onSuccess(fincSelectFilters -> {
                          context.assertEquals(0, fincSelectFilters.getTotalRecords());
                          async.countDown();
                        })
                        .onFailure(throwable -> {
                          context.fail(throwable);
                          async.countDown();
                        });
                  } else {
                    context.fail();
                    async.countDown();
                  }
                });
          } else {
            context.fail();
            async.countDown();
          }
        });
  }

}
