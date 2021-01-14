package org.folio.finc.periodic;

import com.google.gson.Gson;
import io.vertx.core.Context;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.finc.dao.EZBCredentialsDAO;
import org.folio.finc.dao.EZBCredentialsDAOImpl;
import org.folio.finc.periodic.ezb.EZBService;
import org.folio.finc.periodic.ezb.EZBServiceImpl;
import org.folio.okapi.common.GenericCompositeFuture;
import org.folio.rest.jaxrs.model.Credentials;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** A {@Link Job} to harvest EZB holding files automatically */
public class EZBHarvestJob implements Job {

  private static final Logger log = LogManager.getLogger(EZBHarvestJob.class);
  private final EZBCredentialsDAO ezbCredentialsDAO = new EZBCredentialsDAOImpl();
  private EZBService ezbService;

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
      ezbService = new EZBServiceImpl();
      run(vertxContext);
    } catch (SchedulerException e) {
      log.error("Error while executing job to update ezb file.", e);
    }
  }

  public void setEZBService(EZBService service) {
    this.ezbService = service;
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
                if (configs.size() == 0) {
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
    Gson gson = new Gson();
    return creds.getCredentials().stream()
        .map(
            c -> {
              JsonObject cfg = gson.fromJson(gson.toJson(vertxConfig), JsonObject.class);
              cfg.put("isil", c.getIsil());
              cfg.put("user", c.getUser());
              cfg.put("password", c.getPassword());
              cfg.put("libId", c.getLibId());
              return cfg;
            })
        .collect(Collectors.toList());
  }
}
