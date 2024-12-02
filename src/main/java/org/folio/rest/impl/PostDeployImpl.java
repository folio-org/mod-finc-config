package org.folio.rest.impl;

import static org.folio.rest.utils.Constants.ENV_EZB_DOWNLOAD_URL;
import static org.folio.rest.utils.Constants.QUARTZ_EZB_JOB_KEY;
import static org.folio.rest.utils.Constants.QUARTZ_EZB_TRIGGER_KEY;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import io.vertx.core.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.finc.periodic.EZBHarvestJob;
import org.folio.finc.periodic.ezb.EZBServiceImpl;
import org.folio.rest.resource.interfaces.PostDeployVerticle;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

public class PostDeployImpl implements PostDeployVerticle {

  private static final Logger log = LogManager.getLogger(PostDeployImpl.class);

  @Override
  public void init(Vertx vertx, Context context, Handler<AsyncResult<Boolean>> handler) {
    if (Boolean.TRUE.equals(context.config().getBoolean("testing"))) {
      log.info("Skipping PostDeployImpl: testing==true");
      handler.handle(Future.succeededFuture(true));
      return;
    }

    String ezbDownloadUrl = System.getenv(ENV_EZB_DOWNLOAD_URL);
    if (StringUtils.isEmpty(ezbDownloadUrl)) {
      log.warn(
          "Environment variable "
              + ENV_EZB_DOWNLOAD_URL
              + " is not set. EZB file harvesting is not setup.");
      return;
    }

    try {
      Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
      scheduler.getContext().put("vertxContext", context);
      scheduler.start();

      JobDetail job =
          newJob(EZBHarvestJob.class).withIdentity(new JobKey(QUARTZ_EZB_JOB_KEY)).build();

      Trigger trigger =
          newTrigger()
              .withIdentity(QUARTZ_EZB_TRIGGER_KEY)
              .withSchedule(cronSchedule("0 00 01 ? * *"))
              .build();

      scheduler.setJobFactory(
          (bundle, sched) -> {
            if (bundle.getJobDetail().getJobClass().equals(EZBHarvestJob.class)) {
              return new EZBHarvestJob(new EZBServiceImpl(ezbDownloadUrl));
            }
            try {
              return bundle.getJobDetail().getJobClass().getDeclaredConstructor().newInstance();
            } catch (Exception e) {
              throw new SchedulerException(e);
            }
          });

      scheduler.scheduleJob(job, trigger);
    } catch (SchedulerException e) {
      log.error(e);
    }
  }
}
