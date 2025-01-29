package org.folio.finc;

import static io.vertx.core.Future.succeededFuture;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.rest.utils.Constants.QUARTZ_EZB_JOB_KEY;

import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.finc.periodic.EZBHarvestJob;
import org.folio.finc.periodic.ezb.EZBServiceImpl;
import org.folio.rest.RestVerticle;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.rest.tools.utils.VertxUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.listeners.JobListenerSupport;
import org.quartz.listeners.SchedulerListenerSupport;

@RunWith(VertxUnitRunner.class)
public class PostDeployImplITest {

  private static final Vertx vertx = VertxUtils.getVertxFromContextOrNew();

  @Test
  public void testThatPostDeployImplIsSchedulingJobs(TestContext context)
      throws SchedulerException {
    Async async = context.async(2);
    Promise<String> urlResultPromise = Promise.promise();

    Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
    scheduler.getListenerManager().addSchedulerListener(new TestSchedulerListener(async));
    scheduler.getListenerManager().addJobListener(new TestJobListener(urlResultPromise));

    int port = NetworkUtils.nextFreePort();
    DeploymentOptions options =
        new DeploymentOptions().setConfig(new JsonObject().put("http.port", port));
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

    // wait for the RestVerticle to be deployed and the job to be scheduled
    async.awaitSuccess(5000);
    JobKey jobKey = new JobKey(QUARTZ_EZB_JOB_KEY);
    assertThat(scheduler.checkExists(jobKey)).isTrue();

    // execute the job and check that the URL is set correctly
    scheduler.triggerJob(jobKey);
    urlResultPromise
        .future()
        .timeout(5000, MILLISECONDS)
        .onComplete(
            context.asyncAssertSuccess(
                url -> assertThat(url).isEqualTo("http://localhost?libId=%s")));
  }

  private static class TestSchedulerListener extends SchedulerListenerSupport {
    private final Async async;

    @Override
    public void jobAdded(JobDetail jobDetail) {
      async.countDown();
    }

    public TestSchedulerListener(Async async) {
      this.async = async;
    }
  }

  private static class TestJobListener extends JobListenerSupport {
    private final Handler<AsyncResult<String>> resultHandler;

    public TestJobListener(Handler<AsyncResult<String>> resultHandler) {
      this.resultHandler = resultHandler;
    }

    @Override
    public String getName() {
      return this.getClass().getName();
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
      String url = getUrl(context.getJobInstance());
      resultHandler.handle(succeededFuture(url));
    }

    private static String getUrl(Job jobInstance) {
      EZBHarvestJob ezbHarvestJob = (EZBHarvestJob) jobInstance;
      EZBServiceImpl ezbService = (EZBServiceImpl) ezbHarvestJob.getEzbService();
      return ezbService.getUrl();
    }
  }
}
