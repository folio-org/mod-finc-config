package org.folio.rest.impl;

import static org.folio.rest.utils.Constants.MODULE_TENANT;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.RestVerticle;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.Isil;
import org.folio.rest.jaxrs.model.Isils;
import org.folio.rest.jaxrs.resource.FincConfigIsils;
import org.folio.rest.persist.PgUtil;

/** Manages isil to tenant relations */
public class FincConfigIsilsAPI implements FincConfigIsils {

  private static final String TABLE_NAME = "isils";
  private static final Logger logger = LogManager.getLogger(FincConfigIsilsAPI.class);

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
    okapiHeaders.put(RestVerticle.OKAPI_HEADER_TENANT, MODULE_TENANT);
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
    okapiHeaders.put(RestVerticle.OKAPI_HEADER_TENANT, MODULE_TENANT);
    PgUtil.post(
        TABLE_NAME,
        entity,
        okapiHeaders,
        vertxContext,
        PostFincConfigIsilsResponse.class,
        asyncResultHandler);
  }

  @Override
  @Validate
  public void getFincConfigIsilsById(
      String id,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    okapiHeaders.put(RestVerticle.OKAPI_HEADER_TENANT, MODULE_TENANT);
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
    okapiHeaders.put(RestVerticle.OKAPI_HEADER_TENANT, MODULE_TENANT);
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
    okapiHeaders.put(RestVerticle.OKAPI_HEADER_TENANT, MODULE_TENANT);
    PgUtil.put(
        TABLE_NAME,
        entity,
        id,
        okapiHeaders,
        vertxContext,
        PutFincConfigIsilsByIdResponse.class,
        asyncResultHandler);
  }
}
