package org.folio.rest.impl;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.finc.periodic.EZBHarvestJob;
import org.folio.rest.resource.interfaces.PostDeployVerticle;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

public class PostDeployImpl implements PostDeployVerticle {

  private static final Logger log = LoggerFactory.getLogger(PostDeployImpl.class);

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

      JobDetail job = newJob(EZBHarvestJob.class)
          .withIdentity(new JobKey("harvest-ezb-files-job"))
          .build();

      Trigger trigger = newTrigger()
          .withIdentity("harvest-ezb-files-trigger")
          .withSchedule(cronSchedule("0 00 01 ? * *"))
          .build();

      scheduler.scheduleJob(job, trigger);
    } catch (SchedulerException e) {
      log.error(e);
    }

  }
}
