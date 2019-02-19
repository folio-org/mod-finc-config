package org.folio.rest.impl;

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
import java.util.UUID;
import javax.ws.rs.core.Response;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.MetadataCollection;
import org.folio.rest.jaxrs.model.MetadataCollectionsGetOrder;
import org.folio.rest.jaxrs.resource.MetadataCollections;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.tools.messages.MessageConsts;
import org.folio.rest.tools.messages.Messages;
import org.folio.rest.tools.utils.ValidationHelper;
import org.folio.rest.utils.Constants;
import org.z3950.zing.cql.cql2pgjson.CQL2PgJSON;
import org.z3950.zing.cql.cql2pgjson.FieldException;

/**
 * ATTENTION: API works tenant agnostic. Thus, don't use 'x-okapi-tenant' header, but {@value
 * Constants#MODULE_TENANT} as tenant.
 */
public class MetadataCollectionsAPI implements MetadataCollections {

  private static final String ID_FIELD = "_id";
  private static final String TABLE_NAME = "metadata_collections";
  private final Messages messages = Messages.getInstance();
  private final Logger logger = LoggerFactory.getLogger(MetadataCollectionsAPI.class);

  public MetadataCollectionsAPI(Vertx vertx, String tenantId) {
    PostgresClient.getInstance(vertx).setIdField(ID_FIELD);
  }

  private CQLWrapper getCQL(String query, int limit, int offset) throws FieldException {
    CQL2PgJSON cql2PgJSON = new CQL2PgJSON(Arrays.asList(TABLE_NAME + ".jsonb"));
    return new CQLWrapper(cql2PgJSON, query)
        .setLimit(new Limit(limit))
        .setOffset(new Offset(offset));
  }

  @Override
  @Validate
  public void getMetadataCollections(
      String query,
      String orderBy,
      MetadataCollectionsGetOrder order,
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
                      MetadataCollection.class,
                      fieldList,
                      cql,
                      true,
                      false,
                      reply -> {
                        try {
                          if (reply.succeeded()) {
                            org.folio.rest.jaxrs.model.MetadataCollections collectionsCollection =
                                new org.folio.rest.jaxrs.model.MetadataCollections();
                            List<MetadataCollection> results = reply.result().getResults();
                            collectionsCollection.setMetadataCollections(results);
                            collectionsCollection.setTotalRecords(
                                reply.result().getResultInfo().getTotalRecords());
                            asyncResultHandler.handle(
                                Future.succeededFuture(
                                    GetMetadataCollectionsResponse.respond200WithApplicationJson(
                                        collectionsCollection)));
                          } else {
                            asyncResultHandler.handle(
                                Future.succeededFuture(
                                    GetMetadataCollectionsResponse.respond500WithTextPlain(
                                        messages.getMessage(
                                            lang, MessageConsts.InternalServerError))));
                          }
                        } catch (Exception e) {
                          logger.debug(e.getLocalizedMessage());
                          asyncResultHandler.handle(
                              Future.succeededFuture(
                                  GetMetadataCollectionsResponse.respond500WithTextPlain(
                                      messages.getMessage(
                                          lang, MessageConsts.InternalServerError))));
                        }
                      });
            } catch (IllegalStateException e) {
              logger.debug("IllegalStateException: " + e.getLocalizedMessage());
              asyncResultHandler.handle(
                  Future.succeededFuture(
                      GetMetadataCollectionsResponse.respond400WithTextPlain(
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
                        GetMetadataCollectionsResponse.respond400WithTextPlain(
                            "CQL Parsing Error for '" + "" + "': " + cause.getLocalizedMessage())));
              } else {
                asyncResultHandler.handle(
                    io.vertx.core.Future.succeededFuture(
                        GetMetadataCollectionsResponse.respond500WithTextPlain(
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
                GetMetadataCollectionsResponse.respond400WithTextPlain(
                    "CQL Parsing Error for '" + "" + "': " + e.getLocalizedMessage())));
      } else {
        asyncResultHandler.handle(
            io.vertx.core.Future.succeededFuture(
                GetMetadataCollectionsResponse.respond500WithTextPlain(
                    messages.getMessage(lang, MessageConsts.InternalServerError))));
      }
    }
  }

  @Override
  @Validate
  public void postMetadataCollections(
      String lang,
      MetadataCollection entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    logger.debug("Posting metadata collection");
    try {
      vertxContext.runOnContext(
          v -> {
            String tenantId = Constants.MODULE_TENANT;
            try {
              String id = entity.getId();
              if (id == null) {
                id = UUID.randomUUID().toString();
                entity.setId(id);
              }
              Criteria labelCrit = new Criteria();
              labelCrit.addField("'id'");
              labelCrit.setOperation("=");
              labelCrit.setValue(entity.getId());
              Criterion crit = new Criterion(labelCrit);
              try {
                PostgresClient.getInstance(vertxContext.owner(), tenantId)
                    .get(
                        TABLE_NAME,
                        MetadataCollection.class,
                        crit,
                        true,
                        getReply -> {
                          logger.debug("Attempting to get existing metadata collection of same id");
                          if (getReply.failed()) {
                            logger.debug(
                                "Attempt to get metadata collection failed: "
                                    + getReply.cause().getMessage());
                            asyncResultHandler.handle(
                                Future.succeededFuture(
                                    PostMetadataCollectionsResponse.respond500WithTextPlain(
                                        messages.getMessage(
                                            lang, MessageConsts.InternalServerError))));
                          } else {

                            List<MetadataCollection> metadataCollections =
                                getReply.result().getResults();
                            if (metadataCollections.size() > 0) {
                              logger.debug("Metadata collection with this id already exists");
                              asyncResultHandler.handle(
                                  Future.succeededFuture(
                                      PostMetadataCollectionsResponse.respond422WithApplicationJson(
                                          ValidationHelper.createValidationErrorMessage(
                                              "'id'",
                                              entity.getId(),
                                              "Metadata collection with this id already exists"))));
                            } else {
                              PostgresClient postgresClient =
                                  PostgresClient.getInstance(vertxContext.owner(), tenantId);
                              postgresClient.save(
                                  TABLE_NAME,
                                  entity.getId(),
                                  entity,
                                  reply -> {
                                    try {
                                      if (reply.succeeded()) {
                                        logger.debug("save successful");
                                        asyncResultHandler.handle(
                                            Future.succeededFuture(
                                                PostMetadataCollectionsResponse
                                                    .respond201WithApplicationJson(
                                                        entity,
                                                        PostMetadataCollectionsResponse
                                                            .headersFor201()
                                                            .withLocation(
                                                                "/metadata-collections/"
                                                                    + entity.getId()))));
                                      } else {
                                        asyncResultHandler.handle(
                                            Future.succeededFuture(
                                                PostMetadataCollectionsResponse
                                                    .respond500WithTextPlain(
                                                        messages.getMessage(
                                                            lang,
                                                            MessageConsts.InternalServerError))));
                                      }
                                    } catch (Exception e) {
                                      asyncResultHandler.handle(
                                          io.vertx.core.Future.succeededFuture(
                                              PostMetadataCollectionsResponse
                                                  .respond500WithTextPlain(
                                                      messages.getMessage(
                                                          lang,
                                                          MessageConsts.InternalServerError))));
                                    }
                                  });
                            }
                          }
                        });
              } catch (Exception e) {
                logger.error(e.getLocalizedMessage(), e);
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        PostMetadataCollectionsResponse.respond500WithTextPlain(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
              }
            } catch (Exception e) {
              asyncResultHandler.handle(
                  Future.succeededFuture(
                      PostMetadataCollectionsResponse.respond500WithTextPlain(
                          messages.getMessage(lang, MessageConsts.InternalServerError))));
            }
          });
    } catch (Exception e) {
      asyncResultHandler.handle(
          Future.succeededFuture(
              PostMetadataCollectionsResponse.respond500WithTextPlain(
                  messages.getMessage(lang, MessageConsts.InternalServerError))));
    }
  }

  @Override
  @Validate
  public void getMetadataCollectionsById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    logger.debug("Getting single metadata collection by id: " + id);
    try {
      vertxContext.runOnContext(
          v -> {
            String tenantId = Constants.MODULE_TENANT;

            try {
              PostgresClient.getInstance(vertxContext.owner(), tenantId)
                  .getById(
                      TABLE_NAME,
                      id,
                      MetadataCollection.class,
                      reply -> {
                        if (reply.succeeded()) {
                          MetadataCollection result = reply.result();
                          if (result == null) {
                            asyncResultHandler.handle(
                                Future.succeededFuture(
                                    GetMetadataCollectionsByIdResponse.respond404WithTextPlain(
                                        "Metadata collection: "
                                            + messages.getMessage(
                                                lang, MessageConsts.ObjectDoesNotExist))));
                          } else {
                            asyncResultHandler.handle(
                                Future.succeededFuture(
                                    GetMetadataCollectionsByIdResponse
                                        .respond200WithApplicationJson(result)));
                          }
                        } else {
                          asyncResultHandler.handle(
                              Future.succeededFuture(
                                  GetMetadataCollectionsByIdResponse.respond500WithTextPlain(
                                      messages.getMessage(
                                          lang, MessageConsts.InternalServerError))));
                        }
                      });
            } catch (Exception e) {
              logger.debug("Error occured: " + e.getMessage());
              asyncResultHandler.handle(
                  Future.succeededFuture(
                      GetMetadataCollectionsByIdResponse.respond500WithTextPlain(
                          messages.getMessage(lang, MessageConsts.InternalServerError))));
            }
          });
    } catch (Exception e) {
      asyncResultHandler.handle(
          Future.succeededFuture(
              GetMetadataCollectionsByIdResponse.respond500WithTextPlain(
                  messages.getMessage(lang, MessageConsts.InternalServerError))));
    }
  }

  @Override
  @Validate
  public void deleteMetadataCollectionsById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    logger.debug("Delete metadata collection: " + id);
    try {
      vertxContext.runOnContext(
          v -> {
            String tenantId = Constants.MODULE_TENANT;
            try {
              PostgresClient.getInstance(vertxContext.owner(), tenantId)
                  .delete(
                      TABLE_NAME,
                      id,
                      deleteReply -> {
                        if (deleteReply.failed()) {
                          logger.debug("Delete failed: " + deleteReply.cause().getMessage());
                          asyncResultHandler.handle(
                              Future.succeededFuture(
                                  DeleteMetadataCollectionsByIdResponse.respond404WithTextPlain(
                                      "Not found")));
                        } else {
                          asyncResultHandler.handle(
                              Future.succeededFuture(
                                  DeleteMetadataCollectionsByIdResponse.respond204()));
                        }
                      });
            } catch (Exception e) {
              logger.debug("Delete failed: " + e.getMessage());
              asyncResultHandler.handle(
                  Future.succeededFuture(
                      DeleteMetadataCollectionsByIdResponse.respond500WithTextPlain(
                          messages.getMessage(lang, MessageConsts.InternalServerError))));
            }
          });
    } catch (Exception e) {
      asyncResultHandler.handle(
          Future.succeededFuture(
              DeleteMetadataCollectionsByIdResponse.respond500WithTextPlain(
                  messages.getMessage(lang, MessageConsts.InternalServerError))));
    }
  }

  @Override
  @Validate
  public void putMetadataCollectionsById(
      String id,
      String lang,
      MetadataCollection entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    logger.debug("Update metadata collection: " + id);
    try {
      vertxContext.runOnContext(
          v -> {
            if (!id.equals(entity.getId())) {
              asyncResultHandler.handle(
                  Future.succeededFuture(
                      PutMetadataCollectionsByIdResponse.respond400WithTextPlain(
                          "You cannot change the value of the id field")));
            } else {
              String tenantId = Constants.MODULE_TENANT;
              try {
                PostgresClient.getInstance(vertxContext.owner(), tenantId)
                    .update(
                        TABLE_NAME,
                        entity,
                        id,
                        putReply -> {
                          if (putReply.failed()) {
                            asyncResultHandler.handle(
                                Future.succeededFuture(
                                    PutMetadataCollectionsByIdResponse.respond500WithTextPlain(
                                        putReply.cause().getMessage())));
                          } else {
                            asyncResultHandler.handle(
                                Future.succeededFuture(
                                    PutMetadataCollectionsByIdResponse.respond204()));
                          }
                        });
              } catch (Exception e) {
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        PutMetadataCollectionsByIdResponse.respond500WithTextPlain(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
              }
            }
          });
    } catch (Exception e) {
      logger.debug(e.getLocalizedMessage());
      asyncResultHandler.handle(
          Future.succeededFuture(
              PutMetadataCollectionsByIdResponse.respond500WithTextPlain(
                  messages.getMessage(lang, MessageConsts.InternalServerError))));
    }
  }
}
