package org.folio.finc.periodic;

import com.google.gson.Gson;
import io.vertx.core.Context;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.List;
import java.util.stream.Collectors;
import org.folio.finc.dao.EZBCredentialsDAO;
import org.folio.finc.dao.EZBCredentialsDAOImpl;
import org.folio.finc.periodic.ezb.EZBService;
import org.folio.finc.periodic.ezb.EZBServiceImpl;
import org.folio.rest.jaxrs.model.Credentials;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;

public class EZBHarvestJob implements Job {

  private static final Logger log = LoggerFactory.getLogger(EZBHarvestJob.class);
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
    ezbCredentialsDAO.getAll(null, 0, 1000, vertxContext)
        .onComplete(ar -> {
          if (ar.succeeded()) {
            Credentials credentials = ar.result();
            List<JsonObject> configs = createConfigs(credentials, vertxContext.config());
            configs.forEach(c -> {
              EZBHarvestVerticle verticle = new EZBHarvestVerticle(ezbService);
              vertxContext.owner().deployVerticle(
                  verticle,
                  new DeploymentOptions().setConfig(c).setWorker(true));
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
    return creds.getCredentials().stream().map(c -> {
      JsonObject cfg = gson
          .fromJson(gson.toJson(vertxConfig), JsonObject.class);
      cfg.put("isil", c.getIsil());
      cfg.put("user", c.getUser());
      cfg.put("password", c.getPassword());
      cfg.put("libId", c.getLibId());
      return cfg;
    }).collect(Collectors.toList());
  }
}
