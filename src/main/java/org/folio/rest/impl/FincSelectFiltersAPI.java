package org.folio.rest.impl;

import static org.folio.rest.impl.Messages.MSG_INTERNAL_SERVER_ERROR;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.folio.rest.tools.utils.TenantTool;

/** Manages filters for ui-finc-select, hence depends on isil/tenant. */
public class FincSelectFiltersAPI implements FincSelectFilters {

  private final IsilDAO isilDAO;
  private final SelectFilterDAO selectFilterDAO;
  private final FilterToCollectionsDAO filterToCollectionsDAO;
  private final FilterHelper filterHelper;
  private final Logger logger = LogManager.getLogger(FincSelectFiltersAPI.class);

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
      String totalRecords,
      int offset,
      int limit,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {

    String tenantId =
        TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));

    isilDAO
        .withIsilForTenant(tenantId, vertxContext)
        .compose(isil -> selectFilterDAO.getAll(query, offset, limit, isil, vertxContext))
        .onComplete(
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
                            MSG_INTERNAL_SERVER_ERROR)));
              }
            });
  }

  @Override
  @Validate
  public void postFincSelectFilters(
      FincSelectFilter entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {

    logger.debug("Posting finc select filter");

    String tenantId =
        TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));

    isilDAO
        .withIsilForTenant(tenantId, vertxContext)
        .compose(isil -> selectFilterDAO.insert(entity.withIsil(isil), vertxContext))
        .onComplete(
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
                            MSG_INTERNAL_SERVER_ERROR)));
              }
            });
  }

  @Override
  @Validate
  public void getFincSelectFiltersById(
      String id,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {

    logger.debug("Get single select filter by id");

    String tenantId =
        TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
    isilDAO
        .withIsilForTenant(tenantId, vertxContext)
        .compose(isil -> selectFilterDAO.getById(id, isil, vertxContext))
        .onComplete(
            ar -> {
              if (ar.succeeded()) {
                FincSelectFilter result = ar.result();
                if (result == null) {
                  asyncResultHandler.handle(
                      Future.succeededFuture(
                          GetFincSelectFiltersByIdResponse.respond404WithTextPlain(
                              MSG_INTERNAL_SERVER_ERROR)));
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
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {

    logger.debug("Delete single select filter by id");

    String tenantId =
        TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));

    Future<Integer> compose =
        isilDAO
            .withIsilForTenant(tenantId, vertxContext)
            .compose(
                isil ->
                    filterHelper
                        .deleteFilesOfFilter(id, isil, vertxContext)
                        .compose(integer -> selectFilterDAO.deleteById(id, isil, vertxContext)));

    compose.onComplete(
        ar -> {
          if (ar.succeeded()) {
            asyncResultHandler.handle(
                Future.succeededFuture(DeleteFincSelectFiltersByIdResponse.respond204()));
          } else {
            asyncResultHandler.handle(
                Future.succeededFuture(
                    DeleteFincSelectFiltersByIdResponse.respond500WithTextPlain(
                        MSG_INTERNAL_SERVER_ERROR)));
          }
        });
  }

  @Override
  @Validate
  public void putFincSelectFiltersById(
      String id,
      FincSelectFilter entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {

    logger.debug("Putting finc select filter");

    String tenantId =
        TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));

    isilDAO
        .withIsilForTenant(tenantId, vertxContext)
        .compose(
            isil ->
                filterHelper
                    .removeFilesToDelete(entity, isil, vertxContext)
                    .compose(
                        fincSelectFilter ->
                            selectFilterDAO.update(
                                fincSelectFilter.withIsil(isil), id, vertxContext)))
        .onComplete(
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
                            MSG_INTERNAL_SERVER_ERROR)));
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
      .withIsilForTenant(tenantId, vertxContext)
      .compose(isil ->
        // First check if filter exists
        selectFilterDAO.getById(id, isil, vertxContext)
          .compose(filter -> {
            if (filter == null) {
              return Future.failedFuture("Filter not found");
            }
            // If filter exists, get its collections
            return filterToCollectionsDAO.getByIdAndIsil(id, isil, vertxContext)
              .map(collections -> collections != null ? collections :
                new FincSelectFilterToCollections()
                  .withCollectionIds(List.of())
                  .withCollectionsCount(0));
          }))
      .onComplete(
        reply -> {
          if (reply.succeeded()) {
            FincSelectFilterToCollections filterToCollections = reply.result();
            filterToCollections.setIsil(null);
            filterToCollections.setId(null);
            asyncResultHandler.handle(
              Future.succeededFuture(
                GetFincSelectFiltersCollectionsByIdResponse
                  .respond200WithApplicationJson(filterToCollections)));
          } else {
            if ("Filter not found".equals(reply.cause().getMessage())) {
              asyncResultHandler.handle(
                Future.succeededFuture(
                  GetFincSelectFiltersCollectionsByIdResponse
                    .respond404WithTextPlain("Not found")));
            } else {
              asyncResultHandler.handle(
                Future.succeededFuture(
                  GetFincSelectFiltersCollectionsByIdResponse
                    .respond500WithTextPlain(MSG_INTERNAL_SERVER_ERROR)));
            }
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
        .withIsilForTenant(tenantId, vertxContext)
        .compose(isil -> selectFilterDAO.getById(id, isil, vertxContext))
        .onComplete(
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
        .withIsilForTenant(tenantId, vertxContext)
        .compose(
            isil -> {
              entity.setIsil(isil);
              return filterToCollectionsDAO.getByIdAndIsil(id, isil, vertxContext);
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
        .onComplete(
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
