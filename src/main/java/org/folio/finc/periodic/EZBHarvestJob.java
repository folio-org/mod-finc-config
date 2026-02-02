package org.folio.finc.periodic;

import io.vertx.core.Context;
import io.vertx.core.Future;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.finc.dao.EZBCredentialsDAO;
import org.folio.finc.dao.EZBCredentialsDAOImpl;
import org.folio.finc.periodic.ezb.EZBService;
import org.folio.rest.jaxrs.model.Credential;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;

/** A {@link Job} to harvest EZB holding files automatically */
public class EZBHarvestJob implements Job {

  private static final Logger log = LogManager.getLogger(EZBHarvestJob.class);
  private final EZBCredentialsDAO ezbCredentialsDAO = new EZBCredentialsDAOImpl();
  private final EZBHarvestService ezbHarvestService;
  private final EZBService ezbService;

  public EZBHarvestJob(EZBService ezbService) {
    this.ezbService = ezbService;
    this.ezbHarvestService = new EZBHarvestService(ezbService);
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    final Context vertxContext;
    try {
      Object o = jobExecutionContext.getScheduler().getContext().get("vertxContext");
      vertxContext = o instanceof Context ctx ? ctx : null;
      if (vertxContext == null) {
        log.error("Cannot find vertxContext.");
        return;
      }
      run(vertxContext);
    } catch (SchedulerException e) {
      log.error("Error while executing job to update ezb file.", e);
    }
  }

  public Future<Void> run(Context vertxContext) {
    // For each tenant there is an ezb credential entry holding the credentials to fetch the
    // tenant's holding files.
    return ezbCredentialsDAO
        .getAll(null, 0, 1000, vertxContext)
        .compose(
            credentials -> {
              List<Credential> creds = credentials.getCredentials();
              if (creds.isEmpty()) {
                log.info("No ezb credentials in DB, thus will not start ezb harvester.");
                return Future.succeededFuture();
              }
              return executeHarvestForCredentials(creds, vertxContext);
            })
        .onFailure(err -> log.error("Error during ezb harvest", err));
  }

  private Future<Void> executeHarvestForCredentials(
      List<Credential> credentials, Context vertxContext) {
    List<Future<Void>> harvestFutures =
        credentials.stream()
            .map(
                cred ->
                    ezbHarvestService
                        .harvest(cred, vertxContext)
                        .onFailure(
                            err ->
                                log.error(
                                    "Failed to harvest ezb for isil {}: {}",
                                    cred.getIsil(),
                                    err.getMessage(),
                                    err)))
            .toList();

    return Future.all(harvestFutures).mapEmpty();
  }

  public EZBService getEzbService() {
    return ezbService;
  }
}
