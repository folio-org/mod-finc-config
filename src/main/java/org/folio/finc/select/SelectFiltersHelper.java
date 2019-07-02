package org.folio.finc.select;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.FincSelectFilter;
import org.folio.rest.jaxrs.model.FincSelectFiltersGetOrder;
import org.folio.rest.jaxrs.resource.FincSelectFilters.DeleteFincSelectFiltersByIdResponse;
import org.folio.rest.jaxrs.resource.FincSelectFilters.GetFincSelectFiltersByIdResponse;
import org.folio.rest.jaxrs.resource.FincSelectFilters.GetFincSelectFiltersResponse;
import org.folio.rest.jaxrs.resource.FincSelectFilters.PostFincSelectFiltersResponse;
import org.folio.rest.jaxrs.resource.FincSelectFilters.PutFincSelectFiltersByIdResponse;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.tools.messages.MessageConsts;
import org.folio.rest.tools.messages.Messages;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.rest.utils.Constants;
import org.z3950.zing.cql.cql2pgjson.CQL2PgJSON;
import org.z3950.zing.cql.cql2pgjson.FieldException;

public class SelectFiltersHelper {

  private static final String ID_FIELD = "_id";
  private static final String TABLE_NAME = "filters";
  private final Logger logger = LoggerFactory.getLogger(SelectFiltersHelper.class);
  private final Messages messages = Messages.getInstance();
  private final IsilHelper isilHelper;

  public SelectFiltersHelper(Vertx vertx, String tenantId) {
    PostgresClient.getInstance(vertx).setIdField(ID_FIELD);
    this.isilHelper = new IsilHelper(vertx, tenantId);
  }

  private CQLWrapper getCQL(String query, int limit, int offset, String isil)
      throws FieldException {
    CQL2PgJSON cql2PgJSON = new CQL2PgJSON(Arrays.asList(TABLE_NAME + ".jsonb"));

    query = addIsilTo(query, isil);

    return new CQLWrapper(cql2PgJSON, query)
        .setLimit(new Limit(limit))
        .setOffset(new Offset(offset));
  }

  private String addIsilTo(String query, String isil) {
    if (query == null || "".equals(query)) {
      return "isil=\"" + isil + "\"";
    } else {
      return query + "AND isil=\"" + isil + "\"";
    }
  }

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
    logger.debug("Getting filters for select");
    try {
      vertxContext.runOnContext(
          v -> {
            String field = "*";
            String[] fieldList = {field};
            try {
              String fincId = Constants.MODULE_TENANT;
              String tenantId =
                  TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));

              isilHelper
                  .fetchIsil(tenantId, vertxContext)
                  .setHandler(
                      ar -> {
                        if (ar.succeeded()) {
                          String isil = ar.result();
                          CQLWrapper cql = null;
                          try {
                            cql = getCQL(query, limit, offset, isil);
                          } catch (FieldException e) {
                            logger.error("Error while processing CQL " + e.getMessage());
                          }
                          getFiltersAndWriteResult(
                              lang, asyncResultHandler, vertxContext, fieldList, fincId, cql);
                        } else {
                          asyncResultHandler.handle(
                              Future.succeededFuture(
                                  GetFincSelectFiltersResponse.respond500WithTextPlain(
                                      ar.cause().getMessage())));
                        }
                      });
            } catch (IllegalStateException e) {
              logger.debug("IllegalStateException: " + e.getLocalizedMessage());
              asyncResultHandler.handle(
                  Future.succeededFuture(
                      GetFincSelectFiltersResponse.respond400WithTextPlain(
                          "CQL Illegal State Error for '" + "" + "': " + e.getLocalizedMessage())));
            } catch (Exception e) {
              Throwable cause = e;
              while (cause.getCause() != null) {
                cause = cause.getCause();
              }
              logger.debug(
                  "Got error " + cause.getClass().getSimpleName() + ": " + e.getLocalizedMessage());
              if (cause.getClass().getSimpleName().contains("CQLParseException")) {
                logger.debug("BAD CQL");
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        GetFincSelectFiltersResponse.respond400WithTextPlain(
                            "CQL Parsing Error for '" + "" + "': " + cause.getLocalizedMessage())));
              } else {
                asyncResultHandler.handle(
                    io.vertx.core.Future.succeededFuture(
                        GetFincSelectFiltersResponse.respond500WithTextPlain(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
              }
            }
          });
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage(), e);
      if (e.getCause() != null
          && e.getCause().getClass().getSimpleName().contains("CQLParseException")) {
        logger.debug("BAD CQL");
        asyncResultHandler.handle(
            Future.succeededFuture(
                GetFincSelectFiltersResponse.respond400WithTextPlain(
                    "CQL Parsing Error for '" + "" + "': " + e.getLocalizedMessage())));
      } else {
        asyncResultHandler.handle(
            io.vertx.core.Future.succeededFuture(
                GetFincSelectFiltersResponse.respond500WithTextPlain(
                    messages.getMessage(lang, MessageConsts.InternalServerError))));
      }
    }
  }

  private void getFiltersAndWriteResult(
      String lang,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext,
      String[] fieldList,
      String fincId,
      CQLWrapper cql) {
    PostgresClient.getInstance(vertxContext.owner(), fincId)
        .get(
            TABLE_NAME,
            FincSelectFilter.class,
            fieldList,
            cql,
            true,
            false,
            reply -> {
              try {
                if (reply.succeeded()) {

                  org.folio.rest.jaxrs.model.FincSelectFilters fincSelectFilters =
                      new org.folio.rest.jaxrs.model.FincSelectFilters();
                  List<FincSelectFilter> filterList = reply.result().getResults();
                  List<FincSelectFilter> results =
                      filterList.stream()
                          .map(
                              filter -> {
                                filter.setIsil(null);
                                return filter;
                              })
                          .collect(Collectors.toList());
                  fincSelectFilters.setFincSelectFilters(results);
                  fincSelectFilters.setTotalRecords(results.size());
                  asyncResultHandler.handle(
                      Future.succeededFuture(
                          GetFincSelectFiltersResponse.respond200WithApplicationJson(
                              fincSelectFilters)));
                } else {
                  asyncResultHandler.handle(
                      Future.succeededFuture(
                          GetFincSelectFiltersResponse.respond500WithTextPlain(
                              messages.getMessage(lang, MessageConsts.InternalServerError))));
                }
              } catch (Exception e) {
                logger.debug(e.getLocalizedMessage());
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        GetFincSelectFiltersResponse.respond500WithTextPlain(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
              }
            });
  }

  public void postFincSelectFilters(
      String lang,
      FincSelectFilter entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    logger.debug("Posting finc select filter");
    try {
      vertxContext.runOnContext(
          v -> {
            try {
              String tenantId =
                  TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
              isilHelper
                  .fetchIsil(tenantId, vertxContext)
                  .setHandler(
                      ar -> {
                        if (ar.succeeded()) {
                          String isil = ar.result();
                          entity.setIsil(isil);
                          okapiHeaders.put(
                              RestVerticle.OKAPI_HEADER_TENANT, Constants.MODULE_TENANT);
                          PgUtil.post(
                              TABLE_NAME,
                              entity,
                              okapiHeaders,
                              vertxContext,
                              PostFincSelectFiltersResponse.class,
                              asyncResultHandler);
                        } else {
                          asyncResultHandler.handle(
                              Future.succeededFuture(
                                  PostFincSelectFiltersResponse.respond500WithTextPlain(
                                      ar.cause().getMessage())));
                        }
                      });
            } catch (Exception e) {
              logger.debug(e.getLocalizedMessage());
              asyncResultHandler.handle(
                  Future.succeededFuture(
                      PostFincSelectFiltersResponse.respond500WithTextPlain(
                          messages.getMessage(lang, MessageConsts.InternalServerError))));
            }
          });
    } catch (Exception e) {
      logger.debug(e.getLocalizedMessage());
      asyncResultHandler.handle(
          Future.succeededFuture(
              PostFincSelectFiltersResponse.respond500WithTextPlain(
                  messages.getMessage(lang, MessageConsts.InternalServerError))));
    }
  }

  public void getFincSelectFiltersById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {

    logger.debug("Getting filter for select by id");
    try {
      vertxContext.runOnContext(
          v -> {
            String field = "*";
            String[] fieldList = {field};
            try {
              String fincId = Constants.MODULE_TENANT;
              String tenantId =
                  TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));

              isilHelper
                  .fetchIsil(tenantId, vertxContext)
                  .setHandler(
                      ar -> {
                        if (ar.succeeded()) {
                          String isil = ar.result();
                          CQLWrapper cql = null;
                          String query = "(id==" + id + ")";
                          try {
                            cql = getCQL(query, 1, 0, isil);
                          } catch (FieldException e) {
                            logger.error("Error while processing CQL " + e.getMessage());
                          }
                          getFilterByIdAndWriteResult(
                              lang, asyncResultHandler, vertxContext, fieldList, fincId, cql);
                        } else {
                          asyncResultHandler.handle(
                              Future.succeededFuture(
                                  GetFincSelectFiltersByIdResponse.respond500WithTextPlain(
                                      ar.cause().getMessage())));
                        }
                      });
            } catch (IllegalStateException e) {
              logger.debug("IllegalStateException: " + e.getLocalizedMessage());
              asyncResultHandler.handle(
                  Future.succeededFuture(
                      GetFincSelectFiltersByIdResponse.respond500WithTextPlain(
                          "CQL Illegal State Error for '" + "" + "': " + e.getLocalizedMessage())));
            } catch (Exception e) {
              Throwable cause = e;
              while (cause.getCause() != null) {
                cause = cause.getCause();
              }
              logger.debug(
                  "Got error " + cause.getClass().getSimpleName() + ": " + e.getLocalizedMessage());
              if (cause.getClass().getSimpleName().contains("CQLParseException")) {
                logger.debug("BAD CQL");
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        GetFincSelectFiltersByIdResponse.respond500WithTextPlain(
                            "CQL Parsing Error for '" + "" + "': " + cause.getLocalizedMessage())));
              } else {
                asyncResultHandler.handle(
                    io.vertx.core.Future.succeededFuture(
                        GetFincSelectFiltersByIdResponse.respond500WithTextPlain(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
              }
            }
          });
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage(), e);
      if (e.getCause() != null
          && e.getCause().getClass().getSimpleName().contains("CQLParseException")) {
        logger.debug("BAD CQL");
        asyncResultHandler.handle(
            Future.succeededFuture(
                GetFincSelectFiltersByIdResponse.respond500WithTextPlain(
                    "CQL Parsing Error for '" + "" + "': " + e.getLocalizedMessage())));
      } else {
        asyncResultHandler.handle(
            io.vertx.core.Future.succeededFuture(
                GetFincSelectFiltersByIdResponse.respond500WithTextPlain(
                    messages.getMessage(lang, MessageConsts.InternalServerError))));
      }
    }
  }

  private void getFilterByIdAndWriteResult(
      String lang,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext,
      String[] fieldList,
      String fincId,
      CQLWrapper cql) {
    PostgresClient.getInstance(vertxContext.owner(), fincId)
        .get(
            TABLE_NAME,
            FincSelectFilter.class,
            fieldList,
            cql,
            true,
            false,
            reply -> {
              try {
                if (reply.succeeded()) {
                  new org.folio.rest.jaxrs.model.FincSelectFilters();
                  List<FincSelectFilter> filterList = reply.result().getResults();
                  FincSelectFilter result = filterList.get(0);
                  result.setIsil(null);
                  asyncResultHandler.handle(
                      Future.succeededFuture(
                          GetFincSelectFiltersByIdResponse.respond200WithApplicationJson(result)));
                } else {
                  asyncResultHandler.handle(
                      Future.succeededFuture(
                          GetFincSelectFiltersByIdResponse.respond500WithTextPlain(
                              messages.getMessage(lang, MessageConsts.InternalServerError))));
                }
              } catch (Exception e) {
                logger.debug(e.getLocalizedMessage());
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        GetFincSelectFiltersByIdResponse.respond500WithTextPlain(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
              }
            });
  }

  public void deleteFincSelectFiltersById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    okapiHeaders.put(RestVerticle.OKAPI_HEADER_TENANT, Constants.MODULE_TENANT);
    PgUtil.deleteById(
        TABLE_NAME,
        id,
        okapiHeaders,
        vertxContext,
        DeleteFincSelectFiltersByIdResponse.class,
        asyncResultHandler);
  }

  public void putFincSelectFiltersById(
      String id,
      String lang,
      FincSelectFilter entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    try {
      vertxContext.runOnContext(
          v -> {
            try {
              String tenantId =
                  TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
              isilHelper
                  .fetchIsil(tenantId, vertxContext)
                  .setHandler(
                      ar -> {
                        if (ar.succeeded()) {
                          String isil = ar.result();
                          entity.setIsil(isil);
                          okapiHeaders.put(
                              RestVerticle.OKAPI_HEADER_TENANT, Constants.MODULE_TENANT);
                          PgUtil.put(
                              TABLE_NAME,
                              entity,
                              id,
                              okapiHeaders,
                              vertxContext,
                              PutFincSelectFiltersByIdResponse.class,
                              asyncResultHandler);
                        } else {
                          asyncResultHandler.handle(
                              Future.succeededFuture(
                                  PutFincSelectFiltersByIdResponse.respond500WithTextPlain(
                                      ar.cause().getMessage())));
                        }
                      });
            } catch (Exception e) {
              logger.debug(e.getLocalizedMessage());
              asyncResultHandler.handle(
                  Future.succeededFuture(
                      PutFincSelectFiltersByIdResponse.respond500WithTextPlain(
                          messages.getMessage(lang, MessageConsts.InternalServerError))));
            }
          });
    } catch (Exception e) {
      logger.debug(e.getLocalizedMessage());
      asyncResultHandler.handle(
          Future.succeededFuture(
              PutFincSelectFiltersByIdResponse.respond500WithTextPlain(
                  messages.getMessage(lang, MessageConsts.InternalServerError))));
    }
  }
}
