package org.folio.rest.impl;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.MetadataSource;
import org.folio.rest.jaxrs.model.MetadataSourcesGetOrder;
import org.folio.rest.jaxrs.resource.MetadataSources;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.messages.MessageConsts;
import org.folio.rest.tools.messages.Messages;
import org.folio.rest.tools.utils.ValidationHelper;
import org.folio.rest.utils.ModuleEnvs;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MetadataSourcesAPI implements MetadataSources {

  private static final String ID_FIELD = "_id";
  private static final String TABLE_NAME = "metadata_sources";
  private final Messages messages = Messages.getInstance();
  private final Logger logger = LoggerFactory.getLogger(MetadataSourcesAPI.class);
  private JsonObject moduleVariables;

  public MetadataSourcesAPI(Vertx vertx, String tenantId) {
    PostgresClient.getInstance(vertx).setIdField(ID_FIELD);
    moduleVariables = ModuleEnvs.allMODConfs();
  }

  @Override
  @Validate
  public void getMetadataSources(
      String query,
      String orderBy,
      MetadataSourcesGetOrder order,
      int offset,
      int limit,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    logger.debug("Getting metadata sources");
    try {
      vertxContext.runOnContext(
          v -> {
            String tenantId = moduleVariables.getString("username");
            logger.debug("Headers present are: " + okapiHeaders.keySet().toString());
            logger.debug("tenantId = " + tenantId);

            String field = "*";
            String[] fieldList = {field};
            try {
              PostgresClient.getInstance(vertxContext.owner(), tenantId)
                  .get(
                      TABLE_NAME,
                      MetadataSource.class,
                      fieldList,
                      "",
                      true,
                      false,
                      reply -> {
                        try {
                          if (reply.succeeded()) {
                            org.folio.rest.jaxrs.model.MetadataSources sourcesCollection =
                                new org.folio.rest.jaxrs.model.MetadataSources();
                            List<MetadataSource> sources = reply.result().getResults();
                            sourcesCollection.setMetadataSources(sources);
                            sourcesCollection.setTotalRecords(
                                reply.result().getResultInfo().getTotalRecords());

                            asyncResultHandler.handle(
                                Future.succeededFuture(
                                    GetMetadataSourcesResponse.respond200WithApplicationJson(
                                        sourcesCollection)));
                          } else {
                            asyncResultHandler.handle(
                                Future.succeededFuture(
                                    GetMetadataSourcesResponse.respond500WithTextPlain(
                                        reply.cause().getMessage())));
                          }
                        } catch (Exception e) {
                          logger.debug(e.getLocalizedMessage());
                          asyncResultHandler.handle(
                              Future.succeededFuture(
                                  GetMetadataSourcesResponse.respond500WithTextPlain(
                                      reply.cause().getMessage())));
                        }
                      });
            } catch (IllegalStateException e) {
              logger.debug("IllegalStateException: " + e.getLocalizedMessage());
              asyncResultHandler.handle(
                  Future.succeededFuture(
                      GetMetadataSourcesResponse.respond400WithTextPlain(
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
                        GetMetadataSourcesResponse.respond400WithTextPlain(
                            "CQL Parsing Error for '" + "" + "': " + cause.getLocalizedMessage())));
              } else {
                asyncResultHandler.handle(
                    io.vertx.core.Future.succeededFuture(
                        GetMetadataSourcesResponse.respond500WithTextPlain(
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
                GetMetadataSourcesResponse.respond400WithTextPlain(
                    "CQL Parsing Error for '" + "" + "': " + e.getLocalizedMessage())));
      } else {
        asyncResultHandler.handle(
            io.vertx.core.Future.succeededFuture(
                GetMetadataSourcesResponse.respond500WithTextPlain(
                    messages.getMessage(lang, MessageConsts.InternalServerError))));
      }
    }
  }

  @Override
  public void postMetadataSources(
      String lang,
      MetadataSource entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    try {
      vertxContext.runOnContext(
          v -> {
            String tenantId = moduleVariables.getString("username");
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
                        MetadataSource.class,
                        crit,
                        true,
                        getReply -> {
                          logger.debug("Attempting to get existing metadata source of same id");
                          if (getReply.failed()) {
                            logger.debug(
                                "Attempt to get metadata source failed: "
                                    + getReply.cause().getMessage());
                            asyncResultHandler.handle(
                                Future.succeededFuture(
                                    PostMetadataSourcesResponse.respond500WithTextPlain(
                                        getReply.cause().getMessage())));
                          } else {
                            List<MetadataSource> sourceList = getReply.result().getResults();
                            if (sourceList.size() > 0) {
                              logger.debug("Metadata source with this id already exists");
                              asyncResultHandler.handle(
                                  Future.succeededFuture(
                                      PostMetadataSourcesResponse.respond422WithApplicationJson(
                                          ValidationHelper.createValidationErrorMessage(
                                              "'id'",
                                              entity.getId(),
                                              "Metadata source with this id already exists"))));
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
                                                PostMetadataSourcesResponse
                                                    .respond201WithApplicationJson(
                                                        entity,
                                                        PostMetadataSourcesResponse.headersFor201()
                                                            .withLocation(
                                                                "/metadata-sources/"
                                                                    + entity.getId()))));
                                      } else {
                                        asyncResultHandler.handle(
                                            Future.succeededFuture(
                                                PostMetadataSourcesResponse.respond500WithTextPlain(
                                                    reply.cause().toString())));
                                      }
                                    } catch (Exception e) {
                                      asyncResultHandler.handle(
                                          io.vertx.core.Future.succeededFuture(
                                              PostMetadataSourcesResponse.respond500WithTextPlain(
                                                  e.getMessage())));
                                    }
                                  });
                            }
                          }
                        });
              } catch (Exception e) {
                logger.error(e.getLocalizedMessage(), e);
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        PostMetadataSourcesResponse.respond500WithTextPlain(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
              }
            } catch (Exception e) {
              asyncResultHandler.handle(
                  Future.succeededFuture(
                      PostMetadataSourcesResponse.respond500WithTextPlain(
                          messages.getMessage(lang, MessageConsts.InternalServerError))));
            }
          });
    } catch (Exception e) {
      asyncResultHandler.handle(
          Future.succeededFuture(
                  PostMetadataSourcesResponse.respond500WithTextPlain(
                  messages.getMessage(lang, MessageConsts.InternalServerError))));
    }
  }

  @Override
  public void getMetadataSourcesById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {}

  @Override
  public void deleteMetadataSourcesById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {}

  @Override
  public void putMetadataSourcesById(
      String id,
      String lang,
      MetadataSource entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {}
}
