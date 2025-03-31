package org.folio.rest.impl;

import static org.folio.rest.impl.Messages.MSG_INTERNAL_SERVER_ERROR;

import io.vertx.core.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.cql2pgjson.CQL2PgJSON;
import org.folio.cql2pgjson.exception.FieldException;
import org.folio.finc.dao.IsilDAO;
import org.folio.finc.dao.IsilDAOImpl;
import org.folio.rest.RestVerticle;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.Isil;
import org.folio.rest.jaxrs.model.Isils;
import org.folio.rest.jaxrs.resource.FincConfigIsils;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.utils.Constants;

/** Manages isil to tenant relations */
public class IsilsAPI implements FincConfigIsils {

  private static final String TABLE_NAME = "isils";
  private static final IsilDAO isilDAO = new IsilDAOImpl();
  private static final Logger logger = LogManager.getLogger(IsilsAPI.class);

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
    logger.debug("Getting isils");
    okapiHeaders.put(RestVerticle.OKAPI_HEADER_TENANT, Constants.MODULE_TENANT);
    PgUtil.get(
        TABLE_NAME,
        Isil.class,
        Isils.class,
        query,
        offset,
        limit,
        okapiHeaders,
        vertxContext,
        GetFincConfigIsilsResponse.class,
        asyncResultHandler);
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
            isilOptional -> {
              if (isilOptional.isEmpty()) {
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
            isilOptional -> {
              if (isilOptional.isEmpty()) {
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
}
