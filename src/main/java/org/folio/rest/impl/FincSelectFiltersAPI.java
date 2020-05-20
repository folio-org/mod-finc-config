package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import org.folio.finc.dao.FilterToCollectionsDAO;
import org.folio.finc.dao.FilterToCollectionsDAOImpl;
import org.folio.finc.dao.IsilDAO;
import org.folio.finc.dao.IsilDAOImpl;
import org.folio.finc.dao.SelectFilterDAO;
import org.folio.finc.dao.SelectFilterDAOImpl;
import org.folio.finc.select.FilterHelper;
import org.folio.rest.RestVerticle;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.FincSelectFilter;
import org.folio.rest.jaxrs.model.FincSelectFilterToCollections;
import org.folio.rest.jaxrs.model.FincSelectFiltersGetOrder;
import org.folio.rest.jaxrs.resource.FincSelectFilters;
import org.folio.rest.tools.messages.MessageConsts;
import org.folio.rest.tools.messages.Messages;
import org.folio.rest.tools.utils.TenantTool;

public class FincSelectFiltersAPI implements FincSelectFilters {

  private final IsilDAO isilDAO;
  private final SelectFilterDAO selectFilterDAO;
  private final FilterToCollectionsDAO filterToCollectionsDAO;
  private final FilterHelper filterHelper;
  private final Messages messages = Messages.getInstance();
  private final Logger logger = LoggerFactory.getLogger(FincSelectFiltersAPI.class);

  public FincSelectFiltersAPI() {
    this.isilDAO = new IsilDAOImpl();
    this.selectFilterDAO = new SelectFilterDAOImpl();
    this.filterToCollectionsDAO = new FilterToCollectionsDAOImpl();
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

    isilDAO
        .getIsilForTenant(tenantId, vertxContext)
        .compose(isil -> selectFilterDAO.getAll(query, offset, limit, isil, vertxContext))
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

    isilDAO
        .getIsilForTenant(tenantId, vertxContext)
        .compose(isil -> selectFilterDAO.insert(entity.withIsil(isil), vertxContext))
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
    isilDAO
        .getIsilForTenant(tenantId, vertxContext)
        .compose(isil -> selectFilterDAO.getById(id, isil, vertxContext))
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
        isilDAO
            .getIsilForTenant(tenantId, vertxContext)
            .compose(
                isil ->
                    filterHelper
                        .deleteFilesOfFilter(id, isil, vertxContext)
                        .compose(integer -> selectFilterDAO.deleteById(id, isil, vertxContext)));

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

    isilDAO
        .getIsilForTenant(tenantId, vertxContext)
        .compose(
            isil ->
                filterHelper
                    .removeFilesToDelete(entity, isil, vertxContext)
                    .compose(
                        fincSelectFilter ->
                            selectFilterDAO
                                .update(fincSelectFilter.withIsil(isil), id, vertxContext)))
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

  @Override
  @Validate
  public void getFincSelectFiltersCollectionsById(
      String id,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    String tenantId =
        TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));

    isilDAO
        .getIsilForTenant(tenantId, vertxContext)
        .compose(isil -> filterToCollectionsDAO.getById(id, isil, vertxContext))
        .setHandler(
            reply -> {
              if (reply.succeeded()) {
                FincSelectFilterToCollections filterToCollections = reply.result();
                if (filterToCollections != null) {
                  filterToCollections.setIsil(null);
                  filterToCollections.setId(null);
                  asyncResultHandler.handle(
                      Future.succeededFuture(
                          GetFincSelectFiltersCollectionsByIdResponse.respond200WithApplicationJson(
                              filterToCollections)));
                } else {
                  asyncResultHandler.handle(
                      Future.succeededFuture(
                          GetFincSelectFiltersCollectionsByIdResponse.respond404WithTextPlain(
                              "Not found")));
                }
              } else {
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        GetFincSelectFiltersCollectionsByIdResponse.respond500WithTextPlain(
                            MessageConsts.InternalServerError)));
              }
            });
  }

  @Override
  @Validate
  public void putFincSelectFiltersCollectionsById(
      String id,
      FincSelectFilterToCollections entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    String tenantId =
        TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));

    isilDAO
        .getIsilForTenant(tenantId, vertxContext)
        .compose(isil -> selectFilterDAO.getById(id, isil, vertxContext))
        .setHandler(
            reply -> {
              if (reply.succeeded()) {
                if (reply.result() == null) {
                  asyncResultHandler.handle(
                      Future.succeededFuture(
                          PutFincSelectFiltersCollectionsByIdResponse.respond400WithTextPlain(
                              "Cannot find filter with id " + id)));
                } else {
                  saveFiltersToCollections(id, entity, tenantId, asyncResultHandler, vertxContext);
                }
              } else {
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        PutFincSelectFiltersCollectionsByIdResponse.respond500WithTextPlain(
                            reply.cause())));
              }
            });
  }

  private void saveFiltersToCollections(
      String id,
      FincSelectFilterToCollections entity,
      String tenantId,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    entity.setId(id);
    entity.setCollectionsCount(entity.getCollectionIds().size());
    isilDAO
        .getIsilForTenant(tenantId, vertxContext)
        .compose(
            isil -> {
              entity.setIsil(isil);
              return filterToCollectionsDAO.getById(id, isil, vertxContext);
            })
        .compose(
            fincSelectFilterCollections -> {
              if (fincSelectFilterCollections == null) {
                // do insert
                return filterToCollectionsDAO.insert(entity, vertxContext);
              } else {
                // do update -> delete and insert
                return filterToCollectionsDAO
                    .deleteById(entity.getId(), vertxContext)
                    .compose(integer -> filterToCollectionsDAO.insert(entity, vertxContext));
              }
            })
        .setHandler(
            reply -> {
              if (reply.succeeded()) {
                FincSelectFilterToCollections filterToCollections = reply.result();
                filterToCollections.setId(null);
                filterToCollections.setIsil(null);
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        PutFincSelectFiltersCollectionsByIdResponse.respond200WithApplicationJson(
                            filterToCollections)));
              } else {
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        PutFincSelectFiltersCollectionsByIdResponse.respond500WithTextPlain(
                            reply.cause())));
              }
            });
  }
}
