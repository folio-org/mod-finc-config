package org.folio.finc.periodic;

import io.vertx.core.Context;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.finc.dao.EZBCredentialsDAO;
import org.folio.finc.dao.EZBCredentialsDAOImpl;
import org.folio.finc.periodic.ezb.EZBService;
import org.folio.okapi.common.GenericCompositeFuture;
import org.folio.rest.jaxrs.model.Credentials;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;

/** A {@Link Job} to harvest EZB holding files automatically */
public class EZBHarvestJob implements Job {

  private static final Logger log = LogManager.getLogger(EZBHarvestJob.class);
  private final EZBCredentialsDAO ezbCredentialsDAO = new EZBCredentialsDAOImpl();
  private EZBService ezbService;

  public EZBHarvestJob(EZBService ezbService) {
    this.ezbService = ezbService;
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    final Context vertxContext;
    try {
      Object o = jobExecutionContext.getScheduler().getContext().get("vertxContext");
      vertxContext = o instanceof Context ? (Context) o : null;
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
    Promise<Void> result = Promise.promise();
    List<Future> composedFutures = new ArrayList<>();
    //   For each tenant there is an ezb credential entry holding the credentials to fetch the
    // tenant's holding files.
    ezbCredentialsDAO
        .getAll(null, 0, 1000, vertxContext)
        .onComplete(
            ar -> {
              if (ar.succeeded()) {
                Credentials credentials = ar.result();
                List<JsonObject> configs = createConfigs(credentials, vertxContext.config());
                if (configs.isEmpty()) {
                  log.info("No ezb credentials in DB, thus will not start ezb harvester.");
                }
                configs.forEach(
                    c -> {
                      Promise<Void> singleResult = Promise.promise();
                      composedFutures.add(singleResult.future());
                      EZBHarvestVerticle verticle = new EZBHarvestVerticle(ezbService);
                      vertxContext
                          .owner()
                          .deployVerticle(
                              verticle,
                              new DeploymentOptions().setConfig(c),
                              stringAsyncResult -> {
                                if (stringAsyncResult.failed()) {
                                  log.error(
                                      String.format(
                                          "Failed to deploy ezb verticle: %s",
                                          stringAsyncResult.cause().getMessage()),
                                      stringAsyncResult.cause());
                                  singleResult.fail(stringAsyncResult.cause());
                                } else {
                                  singleResult.complete();
                                }
                              });
                    });
                GenericCompositeFuture.all(composedFutures)
                    .onComplete(
                        comFutAR -> {
                          if (comFutAR.succeeded()) {
                            result.complete();
                          } else {
                            result.fail(comFutAR.cause());
                          }
                        });
              } else {
                log.error("Error getting ezb credentials", ar.cause());
                result.fail(ar.cause());
              }
            });
    return result.future();
  }

  private List<JsonObject> createConfigs(Credentials creds, JsonObject vertxConfig) {
    return creds.getCredentials().stream()
        .map(
            c -> {
              JsonObject cfg = vertxConfig.copy();
              cfg.put("isil", c.getIsil());
              cfg.put("user", c.getUser());
              cfg.put("password", c.getPassword());
              cfg.put("libId", c.getLibId());
              return cfg;
            })
        .collect(Collectors.toList());
  }

  public EZBService getEzbService() {
    return ezbService;
  }
}
