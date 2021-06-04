package org.folio.rest.impl;

import io.vertx.core.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.cql2pgjson.CQL2PgJSON;
import org.folio.cql2pgjson.exception.FieldException;
import org.folio.finc.dao.MetadataSourcesDAO;
import org.folio.finc.dao.MetadataSourcesDAOImpl;
import org.folio.rest.RestVerticle;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollectionsGetOrder;
import org.folio.rest.jaxrs.model.FincConfigMetadataSource;
import org.folio.rest.jaxrs.model.MdSource;
import org.folio.rest.jaxrs.resource.FincConfigMetadataCollections;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.tools.messages.MessageConsts;
import org.folio.rest.tools.messages.Messages;
import org.folio.rest.utils.Constants;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Manages metadata collections for ui-finc-config
 *
 * ATTENTION: API works tenant agnostic. Thus, don't use 'x-okapi-tenant' header, but {@value
 * Constants#MODULE_TENANT} as tenant.
 */
public class FincConfigMetadataCollectionsAPI implements FincConfigMetadataCollections {

  private static final String TABLE_NAME = "metadata_collections";
  private final Messages messages = Messages.getInstance();
  private final Logger logger = LogManager.getLogger(FincConfigMetadataCollectionsAPI.class);

  private MetadataSourcesDAO metadataSourcesDAO;

  public FincConfigMetadataCollectionsAPI(Vertx vertx, String tenantId) {
    PostgresClient.getInstance(vertx);
    metadataSourcesDAO = new MetadataSourcesDAOImpl();
  }

  private CQLWrapper getCQL(String query, int limit, int offset) throws FieldException {
    CQL2PgJSON cql2PgJSON = new CQL2PgJSON(Arrays.asList(TABLE_NAME + ".jsonb"));
    return new CQLWrapper(cql2PgJSON, query)
        .setLimit(new Limit(limit))
        .setOffset(new Offset(offset));
  }

  @Override
  @Validate
  public void getFincConfigMetadataCollections(
      String query,
      String orderBy,
      FincConfigMetadataCollectionsGetOrder order,
      int offset,
      int limit,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    logger.debug("Getting metadata collections");
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
                      FincConfigMetadataCollection.class,
                      fieldList,
                      cql,
                      true,
                      false,
                      reply -> {
                        try {
                          if (reply.succeeded()) {
                            org.folio.rest.jaxrs.model.FincConfigMetadataCollections
                                collectionsCollection =
                                    new org.folio.rest.jaxrs.model.FincConfigMetadataCollections();
                            List<FincConfigMetadataCollection> results =
                                reply.result().getResults();
                            collectionsCollection.setFincConfigMetadataCollections(results);
                            collectionsCollection.setTotalRecords(
                                reply.result().getResultInfo().getTotalRecords());
                            asyncResultHandler.handle(
                                Future.succeededFuture(
                                    GetFincConfigMetadataCollectionsResponse
                                        .respond200WithApplicationJson(collectionsCollection)));
                          } else {
                            asyncResultHandler.handle(
                                Future.succeededFuture(
                                    GetFincConfigMetadataCollectionsResponse
                                        .respond500WithTextPlain(
                                            messages.getMessage(
                                                lang, MessageConsts.InternalServerError))));
                          }
                        } catch (Exception e) {
                          logger.debug(e.getLocalizedMessage());
                          asyncResultHandler.handle(
                              Future.succeededFuture(
                                  GetFincConfigMetadataCollectionsResponse.respond500WithTextPlain(
                                      messages.getMessage(
                                          lang, MessageConsts.InternalServerError))));
                        }
                      });
            } catch (IllegalStateException e) {
              logger.debug("IllegalStateException: {}", e.getLocalizedMessage());
              asyncResultHandler.handle(
                  Future.succeededFuture(
                      GetFincConfigMetadataCollectionsResponse.respond400WithTextPlain(
                          "CQL Illegal State Error for '" + "" + "': " + e.getLocalizedMessage())));
            } catch (Exception e) {
              Throwable cause = e;
              while (cause.getCause() != null) {
                cause = cause.getCause();
              }
              logger.debug(
                  String.format(
                      "Got error %s: %s",
                      cause.getClass().getSimpleName(), e.getLocalizedMessage()));
              if (cause.getClass().getSimpleName().contains("CQLParseException")) {
                logger.debug("BAD CQL");
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        GetFincConfigMetadataCollectionsResponse.respond400WithTextPlain(
                            "CQL Parsing Error for '" + "" + "': " + cause.getLocalizedMessage())));
              } else {
                asyncResultHandler.handle(
                    io.vertx.core.Future.succeededFuture(
                        GetFincConfigMetadataCollectionsResponse.respond500WithTextPlain(
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
                GetFincConfigMetadataCollectionsResponse.respond400WithTextPlain(
                    "CQL Parsing Error for '" + "" + "': " + e.getLocalizedMessage())));
      } else {
        asyncResultHandler.handle(
            io.vertx.core.Future.succeededFuture(
                GetFincConfigMetadataCollectionsResponse.respond500WithTextPlain(
                    messages.getMessage(lang, MessageConsts.InternalServerError))));
      }
    }
  }

  @Override
  @Validate
  public void postFincConfigMetadataCollections(
      String lang,
      FincConfigMetadataCollection entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    logger.debug("Posting metadata collection");
    okapiHeaders.put(RestVerticle.OKAPI_HEADER_TENANT, Constants.MODULE_TENANT);

    this.addMdSourceNameTo(entity, vertxContext)
        .onComplete(
            ar -> {
              if (ar.succeeded()) {
                FincConfigMetadataCollection newEntity = ar.result();
                PgUtil.post(
                    TABLE_NAME,
                    newEntity,
                    okapiHeaders,
                    vertxContext,
                    PostFincConfigMetadataCollectionsResponse.class,
                    asyncResultHandler);
              } else {
                asyncResultHandler.handle(
                    io.vertx.core.Future.succeededFuture(
                        PostFincConfigMetadataCollectionsResponse.respond500WithTextPlain(
                            ar.cause())));
              }
            });
  }

  @Override
  @Validate
  public void getFincConfigMetadataCollectionsById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    logger.debug("Getting single metadata collection by id: {}", id);
    okapiHeaders.put(RestVerticle.OKAPI_HEADER_TENANT, Constants.MODULE_TENANT);
    PgUtil.getById(
        TABLE_NAME,
        FincConfigMetadataCollection.class,
        id,
        okapiHeaders,
        vertxContext,
        GetFincConfigMetadataCollectionsByIdResponse.class,
        asyncResultHandler);
  }

  @Override
  @Validate
  public void deleteFincConfigMetadataCollectionsById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    logger.debug("Delete metadata collection: {}", id);
    okapiHeaders.put(RestVerticle.OKAPI_HEADER_TENANT, Constants.MODULE_TENANT);
    PgUtil.deleteById(
        TABLE_NAME,
        id,
        okapiHeaders,
        vertxContext,
        DeleteFincConfigMetadataCollectionsByIdResponse.class,
        asyncResultHandler);
  }

  @Override
  @Validate
  public void putFincConfigMetadataCollectionsById(
      String id,
      String lang,
      FincConfigMetadataCollection entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    logger.debug("Update metadata collection: {}", id);
    okapiHeaders.put(RestVerticle.OKAPI_HEADER_TENANT, Constants.MODULE_TENANT);

    this.addMdSourceNameTo(entity, vertxContext)
        .onComplete(
            ar -> {
              if (ar.succeeded()) {
                FincConfigMetadataCollection newEntity = ar.result();
                PgUtil.put(
                    TABLE_NAME,
                    newEntity,
                    id,
                    okapiHeaders,
                    vertxContext,
                    PutFincConfigMetadataCollectionsByIdResponse.class,
                    asyncResultHandler);
              } else {
                asyncResultHandler.handle(
                    io.vertx.core.Future.succeededFuture(
                        PutFincConfigMetadataCollectionsByIdResponse.respond500WithTextPlain(
                            ar.cause())));
              }
            });
  }

  private Future<FincConfigMetadataCollection> addMdSourceNameTo(
      FincConfigMetadataCollection entity, Context context) {
    Promise<FincConfigMetadataCollection> result = Promise.promise();
    MdSource entitiesMDSource = entity.getMdSource();
    if (entitiesMDSource == null) {
      result.complete(entity);
    } else {
      metadataSourcesDAO
          .getById(entitiesMDSource.getId(), context)
          .onComplete(
              ar -> {
                if (ar.succeeded()) {
                  FincConfigMetadataSource mdSource = ar.result();
                  if (mdSource != null) {
                    entitiesMDSource.setName(mdSource.getLabel());
                    result.complete(entity.withMdSource(entitiesMDSource));
                  } else {
                    logger.info("No metadata source found for id {}", entitiesMDSource.getId());
                    result.complete(entity);
                  }
                } else {
                  result.fail("Cannot resolve name of linked metadata source");
                }
              });
    }
    return result.future();
  }
}
