package org.folio.finc;

import static org.junit.Assert.assertTrue;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.rest.RestVerticle;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.rest.tools.utils.VertxUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.listeners.SchedulerListenerSupport;

@RunWith(VertxUnitRunner.class)
public class PostDeployImplITest {

  private static final Vertx vertx = VertxUtils.getVertxFromContextOrNew();

  @Test
  public void testThatPostDeployImplIsSchedulingJobs(TestContext context)
      throws SchedulerException {
    Async async = context.async(2);

    Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
    scheduler.clear();
    scheduler.getListenerManager().addSchedulerListener(new SchedulerListenerAsyncCountdown(async));

    int port = NetworkUtils.nextFreePort();
    DeploymentOptions options =
        new DeploymentOptions(new JsonObject().put("http.port", port).put("testing", false));
    vertx.deployVerticle(
        RestVerticle.class.getName(),
        options,
        ar -> {
          if (ar.succeeded()) {
            async.countDown();
          } else {
            context.fail(ar.cause());
          }
        });

    async.awaitSuccess(5000);
    assertTrue(scheduler.checkExists(new JobKey("harvest-ezb-files-job")));
  }

  private static class SchedulerListenerAsyncCountdown extends SchedulerListenerSupport {

    private final Async async;

    @Override
    public void jobAdded(JobDetail jobDetail) {
      async.countDown();
    }

    public SchedulerListenerAsyncCountdown(Async async) {
      this.async = async;
    }
  }
}
