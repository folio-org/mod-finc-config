package org.folio.rest.impl;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.rest.utils.Constants.QUARTZ_EZB_JOB_KEY;

import io.vertx.core.Promise;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.TestUtils;
import org.folio.finc.periodic.EZBHarvestJob;
import org.folio.finc.periodic.ezb.EZBServiceImpl;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.listeners.JobListenerSupport;
import org.quartz.listeners.SchedulerListenerSupport;

@RunWith(VertxUnitRunner.class)
public class ITPostDeployImpl {

  @AfterClass
  public static void afterClass() throws Exception {
    TestUtils.undeployRestVerticle();
    TestUtils.teardownPostgres();
  }

  @Test
  public void testThatPostDeployImplIsSchedulingJobs(TestContext context) throws Exception {
    Async async = context.async(1);
    Promise<String> urlResultPromise = Promise.promise();

    Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
    scheduler.getListenerManager().addSchedulerListener(new TestSchedulerListener(async));
    scheduler.getListenerManager().addJobListener(new TestJobListener(urlResultPromise));

    TestUtils.setupPostgres();
    TestUtils.deployRestVerticle(false);

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
    private final Promise<String> resultPromise;

    public TestJobListener(Promise<String> resultPromise) {
      this.resultPromise = resultPromise;
    }

    @Override
    public String getName() {
      return this.getClass().getName();
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
      String url = getUrl(context.getJobInstance());
      resultPromise.complete(url);
    }

    private static String getUrl(Job jobInstance) {
      EZBHarvestJob ezbHarvestJob = (EZBHarvestJob) jobInstance;
      EZBServiceImpl ezbService = (EZBServiceImpl) ezbHarvestJob.getEzbService();
      return ezbService.getUrl();
    }
  }
}
