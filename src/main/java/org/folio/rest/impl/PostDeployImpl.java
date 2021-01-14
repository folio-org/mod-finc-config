package org.folio.rest.impl;

import io.vertx.core.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.finc.periodic.EZBHarvestJob;
import org.folio.rest.resource.interfaces.PostDeployVerticle;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

public class PostDeployImpl implements PostDeployVerticle {

  private static final Logger log = LogManager.getLogger(PostDeployImpl.class);

  @Override
  public void init(Vertx vertx, Context context, Handler<AsyncResult<Boolean>> handler) {
    if (Boolean.TRUE.equals(context.config().getBoolean("testing"))) {
      log.info("Skipping PostDeployImpl: testing==true");
      handler.handle(Future.succeededFuture(true));
      return;
    }

    try {
      Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
      scheduler.getContext().put("vertxContext", context);
      scheduler.start();

      JobDetail job =
          newJob(EZBHarvestJob.class).withIdentity(new JobKey("harvest-ezb-files-job")).build();

      Trigger trigger =
          newTrigger()
              .withIdentity("harvest-ezb-files-trigger")
              .withSchedule(cronSchedule("0 00 01 ? * *"))
              .build();

      scheduler.scheduleJob(job, trigger);
    } catch (SchedulerException e) {
      log.error(e);
    }
  }
}
