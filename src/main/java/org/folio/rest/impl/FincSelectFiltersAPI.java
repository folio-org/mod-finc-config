package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import org.folio.finc.dao.FilterDAO;
import org.folio.finc.dao.FilterDAOImpl;
import org.folio.finc.select.FilterHelper;
import org.folio.finc.select.IsilHelper;
import org.folio.rest.RestVerticle;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.FincSelectFilter;
import org.folio.rest.jaxrs.model.FincSelectFiltersGetOrder;
import org.folio.rest.jaxrs.resource.FincSelectFilters;
import org.folio.rest.tools.messages.MessageConsts;
import org.folio.rest.tools.messages.Messages;
import org.folio.rest.tools.utils.TenantTool;

public class FincSelectFiltersAPI implements FincSelectFilters {

  private final IsilHelper isilHelper;
  private final FilterDAO filterDAO;
  private final FilterHelper filterHelper;
  private final Messages messages = Messages.getInstance();
  private final Logger logger = LoggerFactory.getLogger(FincSelectFiltersAPI.class);

  public FincSelectFiltersAPI(Vertx vertx, String tenantId) {
    this.isilHelper = new IsilHelper(vertx, tenantId);
    this.filterDAO = new FilterDAOImpl();
    this.filterHelper = new FilterHelper();
  }

  @Override
  @Validate
  public void getFincSelectFilters(
      String query,
      String orderBy,
      FincSelectFiltersGetOrder order,
      int offset,
      int limit,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {

    String tenantId =
        TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));

    isilHelper
        .fetchIsil(tenantId, vertxContext)
        .compose(isil -> filterDAO.getAll(query, offset, limit, isil, vertxContext))
        .setHandler(
            ar -> {
              if (ar.succeeded()) {
                org.folio.rest.jaxrs.model.FincSelectFilters filters = ar.result();
                List<FincSelectFilter> withoutIsils =
                    filters.getFincSelectFilters().stream()
                        .map(
                            filter -> {
                              filter.setIsil(null);
                              return filter;
                            })
                        .collect(Collectors.toList());
                filters.setFincSelectFilters(withoutIsils);
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        GetFincSelectFiltersResponse.respond200WithApplicationJson(filters)));
              } else {
                asyncResultHandler.handle(
                    io.vertx.core.Future.succeededFuture(
                        GetFincSelectFiltersResponse.respond500WithTextPlain(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
              }
            });
  }

  @Override
  @Validate
  public void postFincSelectFilters(
      String lang,
      FincSelectFilter entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {

    logger.debug("Posting finc select filter");

    String tenantId =
        TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));

    isilHelper
        .fetchIsil(tenantId, vertxContext)
        .compose(isil -> filterDAO.insert(entity.withIsil(isil), vertxContext))
        .setHandler(
            ar -> {
              if (ar.succeeded()) {
                FincSelectFilter fincSelectFilter = ar.result();
                fincSelectFilter.setIsil(null);
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        PostFincSelectFiltersResponse.respond201WithApplicationJson(
                            entity, PostFincSelectFiltersResponse.headersFor201())));
              } else {
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        PostFincSelectFiltersResponse.respond500WithTextPlain(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
              }
            });
  }

  @Override
  @Validate
  public void getFincSelectFiltersById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {

    logger.debug("Get single select filter by id");

    String tenantId =
        TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));

    isilHelper
        .fetchIsil(tenantId, vertxContext)
        .compose(isil -> filterDAO.getById(id, isil, vertxContext))
        .setHandler(
            ar -> {
              if (ar.succeeded()) {
                FincSelectFilter result = ar.result();
                if (result == null) {
                  asyncResultHandler.handle(
                      Future.succeededFuture(
                          GetFincSelectFiltersByIdResponse.respond404WithTextPlain(
                              messages.getMessage(lang, MessageConsts.ObjectDoesNotExist))));
                } else {
                  result.setIsil(null);
                  asyncResultHandler.handle(
                      Future.succeededFuture(
                          GetFincSelectFiltersByIdResponse.respond200WithApplicationJson(result)));
                }
              } else {
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        GetFincSelectFiltersByIdResponse.respond500WithTextPlain(ar.cause())));
              }
            });
  }

  @Override
  @Validate
  public void deleteFincSelectFiltersById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {

    logger.debug("Delete single select filter by id");

    String tenantId =
        TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));

    Future<Integer> compose =
        isilHelper
            .fetchIsil(tenantId, vertxContext)
            .compose(
                isil ->
                    filterHelper
                        .deleteFilesOfFilter(id, isil, vertxContext)
                        .compose(integer -> filterDAO.deleteById(id, isil, vertxContext)));

    compose.setHandler(
        ar -> {
          if (ar.succeeded()) {
            asyncResultHandler.handle(
                Future.succeededFuture(DeleteFincSelectFiltersByIdResponse.respond204()));
          } else {
            asyncResultHandler.handle(
                Future.succeededFuture(
                    DeleteFincSelectFiltersByIdResponse.respond500WithTextPlain(
                        messages.getMessage(lang, MessageConsts.InternalServerError))));
          }
        });
  }

  @Override
  @Validate
  public void putFincSelectFiltersById(
      String id,
      String lang,
      FincSelectFilter entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {

    logger.debug("Putting finc select filter");

    String tenantId =
        TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));

    isilHelper
        .fetchIsil(tenantId, vertxContext)
        .compose(
            isil ->
                filterHelper
                    .removeFilesToDelete(entity, isil, vertxContext)
                    .compose(
                        fincSelectFilter ->
                            filterDAO.update(fincSelectFilter.withIsil(isil), id, vertxContext)))
        .setHandler(
            ar -> {
              if (ar.succeeded()) {
                FincSelectFilter fincSelectFilter = ar.result();
                fincSelectFilter.setIsil(null);
                asyncResultHandler.handle(
                    Future.succeededFuture(PutFincSelectFiltersByIdResponse.respond204()));
              } else {
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        PutFincSelectFiltersByIdResponse.respond500WithTextPlain(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
              }
            });
  }
}
