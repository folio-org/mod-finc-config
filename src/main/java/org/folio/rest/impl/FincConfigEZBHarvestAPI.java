package org.folio.rest.impl;

import static org.folio.rest.utils.Constants.ENV_EZB_DOWNLOAD_URL;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.finc.periodic.EZBHarvestService;
import org.folio.finc.periodic.ezb.EZBService;
import org.folio.finc.periodic.ezb.EZBServiceImpl;
import org.folio.rest.jaxrs.resource.FincConfigEzbHarvest;
import org.folio.rest.tools.utils.TenantTool;

/** Harvests EZB holding files. Invoked periodically via the {@code _timer} interface. */
public class FincConfigEZBHarvestAPI implements FincConfigEzbHarvest {

  private static final Logger log = LogManager.getLogger(FincConfigEZBHarvestAPI.class);

  private final Optional<EZBHarvestService> ezbHarvestService;

  public FincConfigEZBHarvestAPI() {
    this.ezbHarvestService = ezbDownloadUrl().map(EZBServiceImpl::new).map(EZBHarvestService::new);
  }

  FincConfigEZBHarvestAPI(EZBService ezbService) {
    this.ezbHarvestService = Optional.of(new EZBHarvestService(ezbService));
  }

  @Override
  public void postFincConfigEzbHarvest(
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    String tenantId = TenantTool.tenantId(okapiHeaders);
    harvest(tenantId, vertxContext)
        .onSuccess(
            v ->
                asyncResultHandler.handle(
                    Future.succeededFuture(PostFincConfigEzbHarvestResponse.respond204())))
        .onFailure(
            err -> {
              log.error("Error while harvesting ezb file for tenant {}", tenantId, err);
              asyncResultHandler.handle(
                  Future.succeededFuture(
                      PostFincConfigEzbHarvestResponse.respond500WithTextPlain(err.getMessage())));
            });
  }

  private Future<Void> harvest(String tenantId, Context vertxContext) {
    return ezbHarvestService
        .map(service -> service.harvest(tenantId, vertxContext))
        .orElseGet(
            () -> {
              log.warn(
                  "Environment variable {} is not set. Will not harvest ezb file.",
                  ENV_EZB_DOWNLOAD_URL);
              return Future.succeededFuture();
            });
  }

  private static Optional<String> ezbDownloadUrl() {
    return Optional.ofNullable(System.getenv(ENV_EZB_DOWNLOAD_URL)).filter(StringUtils::isNotEmpty);
  }
}
