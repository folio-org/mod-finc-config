package org.folio.rest.impl;

import static org.folio.rest.impl.Messages.MSG_INTERNAL_SERVER_ERROR;

import io.vertx.core.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.cql2pgjson.CQL2PgJSON;
import org.folio.cql2pgjson.exception.FieldException;
import org.folio.finc.dao.IsilDAO;
import org.folio.finc.dao.IsilDAOImpl;
import org.folio.rest.RestVerticle;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.Isil;
import org.folio.rest.jaxrs.resource.FincConfigIsils;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.utils.Constants;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Manages isil to tenant relations
 */
public class IsilsAPI implements FincConfigIsils {

  private static final String TABLE_NAME = "isils";
  private final Logger logger = LogManager.getLogger(IsilsAPI.class);
  private final IsilDAO isilDAO;

  public IsilsAPI(Vertx vertx, String tenantId) {
    PostgresClient.getInstance(vertx);
    isilDAO = new IsilDAOImpl();
  }

  private CQLWrapper getCQL(String query, int limit, int offset) throws FieldException {
    CQL2PgJSON cql2PgJSON = new CQL2PgJSON(Arrays.asList(TABLE_NAME + ".jsonb"));
    return new CQLWrapper(cql2PgJSON, query)
        .setLimit(new Limit(limit))
        .setOffset(new Offset(offset));
  }

  @Override
  @Validate
  public void getFincConfigIsils(
      String query,
      String totalRecords,
      int offset,
      int limit,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    logger.debug("Getting isisl");
    try {
      CQLWrapper cql = getCQL(query, limit, offset);
      vertxContext.runOnContext(
          v -> {
            String tenantId = Constants.MODULE_TENANT;
            String field = "*";
            String[] fieldList = {field};
            try {
              PostgresClient.getInstance(vertxContext.owner(), tenantId)
                  .get(
                      TABLE_NAME,
                      Isil.class,
                      fieldList,
                      cql,
                      true,
                      false,
                      reply -> {
                        try {
                          if (reply.succeeded()) {
                            org.folio.rest.jaxrs.model.Isils isilsCollection =
                                new org.folio.rest.jaxrs.model.Isils();
                            List<Isil> results = reply.result().getResults();
                            isilsCollection.setIsils(results);
                            isilsCollection.setTotalRecords(
                                reply.result().getResultInfo().getTotalRecords());
                            asyncResultHandler.handle(
                                Future.succeededFuture(
                                    GetFincConfigIsilsResponse.respond200WithApplicationJson(
                                        isilsCollection)));
                          } else {
                            asyncResultHandler.handle(
                                Future.succeededFuture(
                                    GetFincConfigIsilsResponse.respond500WithTextPlain(
                                        MSG_INTERNAL_SERVER_ERROR)));
                          }
                        } catch (Exception e) {
                          logger.debug(e.getLocalizedMessage());
                          asyncResultHandler.handle(
                              Future.succeededFuture(
                                  GetFincConfigIsilsResponse.respond500WithTextPlain(
                                      MSG_INTERNAL_SERVER_ERROR)));
                        }
                      });
            } catch (IllegalStateException e) {
              logger.debug("IllegalStateException: {}", e.getLocalizedMessage());
              asyncResultHandler.handle(
                  Future.succeededFuture(
                      GetFincConfigIsilsResponse.respond400WithTextPlain(
                          "CQL Illegal State Error for '" + "" + "': " + e.getLocalizedMessage())));
            } catch (Exception e) {
              Throwable cause = e;
              while (cause.getCause() != null) {
                cause = cause.getCause();
              }
              logger.debug(
                  "Got error {}: {}", cause.getClass().getSimpleName(), e.getLocalizedMessage());
              if (cause.getClass().getSimpleName().contains("CQLParseException")) {
                logger.debug("BAD CQL");
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        GetFincConfigIsilsResponse.respond400WithTextPlain(
                            "CQL Parsing Error for '" + "" + "': " + cause.getLocalizedMessage())));
              } else {
                asyncResultHandler.handle(
                    io.vertx.core.Future.succeededFuture(
                        GetFincConfigIsilsResponse.respond500WithTextPlain(
                            MSG_INTERNAL_SERVER_ERROR)));
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
                GetFincConfigIsilsResponse.respond400WithTextPlain(
                    "CQL Parsing Error for '" + "" + "': " + e.getLocalizedMessage())));
      } else {
        asyncResultHandler.handle(
            io.vertx.core.Future.succeededFuture(
                GetFincConfigIsilsResponse.respond500WithTextPlain(MSG_INTERNAL_SERVER_ERROR)));
      }
    }
  }

  @Override
  @Validate
  public void postFincConfigIsils(
      Isil entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    okapiHeaders.put(RestVerticle.OKAPI_HEADER_TENANT, Constants.MODULE_TENANT);

    String tenant = entity.getTenant();
    isilDAO
        .getIsilForTenant(tenant, vertxContext)
        .onSuccess(
            isil -> {
              if (isil == null) {
                PgUtil.post(
                    TABLE_NAME,
                    entity,
                    okapiHeaders,
                    vertxContext,
                    PostFincConfigIsilsResponse.class,
                    asyncResultHandler);
              } else {
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        PostFincConfigIsilsResponse.respond400WithTextPlain(
                            "Tenant already has an isil. Only one isil per tenant allowed.")));
              }
            })
        .onFailure(
            throwable ->
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        PostFincConfigIsilsResponse.respond500WithTextPlain(throwable))));
  }

  @Override
  @Validate
  public void getFincConfigIsilsById(
      String id,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    okapiHeaders.put(RestVerticle.OKAPI_HEADER_TENANT, Constants.MODULE_TENANT);
    PgUtil.getById(
        TABLE_NAME,
        Isil.class,
        id,
        okapiHeaders,
        vertxContext,
        GetFincConfigIsilsByIdResponse.class,
        asyncResultHandler);
  }

  @Override
  @Validate
  public void deleteFincConfigIsilsById(
      String id,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    okapiHeaders.put(RestVerticle.OKAPI_HEADER_TENANT, Constants.MODULE_TENANT);
    PgUtil.deleteById(
        TABLE_NAME,
        id,
        okapiHeaders,
        vertxContext,
        DeleteFincConfigIsilsByIdResponse.class,
        asyncResultHandler);
  }

  @Override
  @Validate
  public void putFincConfigIsilsById(
      String id,
      Isil entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    okapiHeaders.put(RestVerticle.OKAPI_HEADER_TENANT, Constants.MODULE_TENANT);

    String tenant = entity.getTenant();
    isilDAO
        .getIsilForTenant(tenant, vertxContext)
        .onSuccess(
            isil -> {
              if (isilIsValid(isil, entity.getIsil())) {
                PgUtil.put(
                    TABLE_NAME,
                    entity,
                    id,
                    okapiHeaders,
                    vertxContext,
                    PutFincConfigIsilsByIdResponse.class,
                    asyncResultHandler);
              } else {
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        PostFincConfigIsilsResponse.respond400WithTextPlain(
                            "Tenant already has an isil. Only one isil per tenant allowed.")));
              }
            })
        .onFailure(
            throwable ->
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        PostFincConfigIsilsResponse.respond500WithTextPlain(throwable))));
  }

  private boolean isilIsValid(String isilForTenant, String isilFromRequestes) {
    if (isilForTenant == null) {
      return true;
    }
    return isilForTenant.equals(isilFromRequestes);
  }
}
