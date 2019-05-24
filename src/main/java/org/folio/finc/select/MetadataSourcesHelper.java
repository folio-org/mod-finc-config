package org.folio.finc.select;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.folio.finc.select.isil.filter.IsilFilter;
import org.folio.finc.select.isil.filter.MetadataSourcesIsilFilter;
import org.folio.finc.select.verticles.AbstractSelectMetadataSourceVerticle;
import org.folio.finc.select.verticles.factory.SelectMetadataSourceVerticleFactory;
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.FincConfigMetadataSource;
import org.folio.rest.jaxrs.model.FincSelectMetadataSource;
import org.folio.rest.jaxrs.model.FincSelectMetadataSourcesGetOrder;
import org.folio.rest.jaxrs.model.Select;
import org.folio.rest.jaxrs.resource.FincSelectMetadataSources.GetFincSelectMetadataSourcesByIdResponse;
import org.folio.rest.jaxrs.resource.FincSelectMetadataSources.GetFincSelectMetadataSourcesResponse;
import org.folio.rest.jaxrs.resource.FincSelectMetadataSources.PutFincSelectMetadataSourcesCollectionsSelectAllByIdResponse;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.tools.messages.MessageConsts;
import org.folio.rest.tools.messages.Messages;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.rest.utils.Constants;
import org.z3950.zing.cql.cql2pgjson.CQL2PgJSON;
import org.z3950.zing.cql.cql2pgjson.FieldException;

/** Helper class to fetch metadata sources for finc-select. */
public class MetadataSourcesHelper {
  private static final String ID_FIELD = "_id";
  private static final String TABLE_NAME = "metadata_sources";
  private final Messages messages = Messages.getInstance();
  private final Logger logger = LoggerFactory.getLogger(MetadataSourcesHelper.class);
  private final IsilHelper isilHelper;
  private final IsilFilter<FincSelectMetadataSource, FincConfigMetadataSource> isilFilter;

  public MetadataSourcesHelper(Vertx vertx, String tenantId) {
    PostgresClient.getInstance(vertx).setIdField(ID_FIELD);
    this.isilHelper = new IsilHelper(vertx, tenantId);
    this.isilFilter = new MetadataSourcesIsilFilter();
  }

  private CQLWrapper getCQL(String query, int limit, int offset) throws FieldException {
    CQL2PgJSON cql2PgJSON = new CQL2PgJSON(Arrays.asList(TABLE_NAME + ".jsonb"));
    return new CQLWrapper(cql2PgJSON, query)
        .setLimit(new Limit(limit))
        .setOffset(new Offset(offset));
  }

  public void getFincSelectMetadataSources(
      String query,
      int offset,
      int limit,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    logger.debug("Getting metadata sources for select");
    try {
      CQLWrapper cql = getCQL(query, limit, offset);
      vertxContext.runOnContext(
          v -> {
            String field = "*";
            String[] fieldList = {field};
            try {
              String fincId = Constants.MODULE_TENANT;
              String tenantId =
                  TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
              PostgresClient.getInstance(vertxContext.owner(), fincId)
                  .get(
                      TABLE_NAME,
                      FincConfigMetadataSource.class,
                      fieldList,
                      cql,
                      true,
                      false,
                      reply -> {
                        try {
                          if (reply.succeeded()) {
                            org.folio.rest.jaxrs.model.FincSelectMetadataSources sourcesCollection =
                                new org.folio.rest.jaxrs.model.FincSelectMetadataSources();
                            List<FincConfigMetadataSource> results = reply.result().getResults();
                            isilHelper
                                .getIsilForTenant(tenantId, okapiHeaders, vertxContext)
                                .setHandler(
                                    isilResult -> {
                                      if (isilResult.succeeded()) {
                                        String isil = isilResult.result();
                                        List<FincSelectMetadataSource> transformedSources =
                                            isilFilter.filterForIsil(results, isil);
                                        sourcesCollection.setFincSelectMetadataSources(
                                            transformedSources);
                                        sourcesCollection.setTotalRecords(
                                            transformedSources.size());
                                        asyncResultHandler.handle(
                                            Future.succeededFuture(
                                                GetFincSelectMetadataSourcesResponse
                                                    .respond200WithApplicationJson(
                                                        sourcesCollection)));
                                      } else {
                                        asyncResultHandler.handle(
                                            Future.succeededFuture(
                                                GetFincSelectMetadataSourcesResponse
                                                    .respond500WithTextPlain(
                                                        isilResult.cause().getMessage())));
                                      }
                                    });
                          } else {
                            asyncResultHandler.handle(
                                Future.succeededFuture(
                                    GetFincSelectMetadataSourcesResponse.respond500WithTextPlain(
                                        messages.getMessage(
                                            lang, MessageConsts.InternalServerError))));
                          }
                        } catch (Exception e) {
                          logger.debug(e.getLocalizedMessage());
                          asyncResultHandler.handle(
                              Future.succeededFuture(
                                  GetFincSelectMetadataSourcesResponse.respond500WithTextPlain(
                                      messages.getMessage(
                                          lang, MessageConsts.InternalServerError))));
                        }
                      });
            } catch (IllegalStateException e) {
              logger.debug("IllegalStateException: " + e.getLocalizedMessage());
              asyncResultHandler.handle(
                  Future.succeededFuture(
                      GetFincSelectMetadataSourcesResponse.respond400WithTextPlain(
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
                        GetFincSelectMetadataSourcesResponse.respond400WithTextPlain(
                            "CQL Parsing Error for '" + "" + "': " + cause.getLocalizedMessage())));
              } else {
                asyncResultHandler.handle(
                    io.vertx.core.Future.succeededFuture(
                        GetFincSelectMetadataSourcesResponse.respond500WithTextPlain(
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
                GetFincSelectMetadataSourcesResponse.respond400WithTextPlain(
                    "CQL Parsing Error for '" + "" + "': " + e.getLocalizedMessage())));
      } else {
        asyncResultHandler.handle(
            io.vertx.core.Future.succeededFuture(
                GetFincSelectMetadataSourcesResponse.respond500WithTextPlain(
                    messages.getMessage(lang, MessageConsts.InternalServerError))));
      }
    }
  }

  public void getFincSelectMetadataSourcesById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    try {
      vertxContext.runOnContext(
          v -> {
            String fincId = Constants.MODULE_TENANT;
            String tenantId =
                TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
            try {
              Criteria idCrit =
                  new Criteria()
                      .addField(ID_FIELD)
                      .setJSONB(false)
                      .setOperation("=")
                      .setValue("'" + id + "'");
              Criterion criterion = new Criterion(idCrit);
              logger.debug("Using criterion: " + criterion.toString());
              PostgresClient.getInstance(vertxContext.owner(), fincId)
                  .get(
                      TABLE_NAME,
                      FincConfigMetadataSource.class,
                      criterion,
                      true,
                      false,
                      getReply -> {
                        if (getReply.failed()) {
                          asyncResultHandler.handle(
                              Future.succeededFuture(
                                  GetFincSelectMetadataSourcesByIdResponse.respond500WithTextPlain(
                                      messages.getMessage(
                                          lang, MessageConsts.InternalServerError))));
                        } else {
                          List<FincConfigMetadataSource> metadataSources =
                              getReply.result().getResults();
                          if (metadataSources.isEmpty()) {
                            asyncResultHandler.handle(
                                Future.succeededFuture(
                                    GetFincSelectMetadataSourcesByIdResponse
                                        .respond404WithTextPlain(
                                            "Metadata Source "
                                                + messages.getMessage(
                                                    lang, MessageConsts.ObjectDoesNotExist))));
                          } else if (metadataSources.size() > 1) {
                            logger.debug("Multiple metadata sources found with the same id");
                            asyncResultHandler.handle(
                                Future.succeededFuture(
                                    GetFincSelectMetadataSourcesByIdResponse
                                        .respond500WithTextPlain(
                                            messages.getMessage(
                                                lang, MessageConsts.InternalServerError))));
                          } else {

                            this.isilHelper
                                .getIsilForTenant(tenantId, okapiHeaders, vertxContext)
                                .setHandler(
                                    isilResult -> {
                                      if (isilResult.succeeded()) {
                                        String isil = isilResult.result();
                                        FincSelectMetadataSource result =
                                            isilFilter.filterForIsil(metadataSources.get(0), isil);
                                        asyncResultHandler.handle(
                                            Future.succeededFuture(
                                                GetFincSelectMetadataSourcesByIdResponse
                                                    .respond200WithApplicationJson(result)));
                                      } else {
                                        asyncResultHandler.handle(
                                            Future.succeededFuture(
                                                GetFincSelectMetadataSourcesByIdResponse
                                                    .respond500WithTextPlain(
                                                        isilResult.cause().getMessage())));
                                      }
                                    });
                          }
                        }
                      });
            } catch (Exception e) {
              logger.debug("Error occurred: " + e.getMessage());
              asyncResultHandler.handle(
                  Future.succeededFuture(
                      GetFincSelectMetadataSourcesByIdResponse.respond500WithTextPlain(
                          messages.getMessage(lang, MessageConsts.InternalServerError))));
            }
          });
    } catch (Exception e) {
      asyncResultHandler.handle(
          Future.succeededFuture(
              GetFincSelectMetadataSourcesByIdResponse.respond500WithTextPlain(
                  messages.getMessage(lang, MessageConsts.InternalServerError))));
    }
  }

  public void putFincSelectMetadataSourcesCollectionsSelectAllById(
      String id,
      Select entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    try {
      vertxContext.runOnContext(
          v -> {
            String tenantId =
                TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
            String msg =
                String.format(
                    "Will (un)select metadata collections of metadata source %s for tenant %s.",
                    id, tenantId);
            logger.info(msg);
            deploySelectSourceVerticle(vertxContext.owner(), id, tenantId, entity);
            String result = new JsonObject().put("message", msg).toString();
            asyncResultHandler.handle(
                Future.succeededFuture(
                    Response.ok(result, MediaType.APPLICATION_JSON_TYPE).build()));
          });
    } catch (Exception e) {
      asyncResultHandler.handle(
          Future.succeededFuture(
              PutFincSelectMetadataSourcesCollectionsSelectAllByIdResponse.respond500WithTextPlain(
                  e.getCause())));
    }
  }

  public void deploySelectSourceVerticle(
      Vertx vertx, String metadataSourceId, String tenantId, Select select) {

    AbstractSelectMetadataSourceVerticle verticle =
        SelectMetadataSourceVerticleFactory.create(vertx, vertx.getOrCreateContext(), select);

    vertx = Vertx.vertx();
    JsonObject cfg = vertx.getOrCreateContext().config();
    cfg.put("tenantId", tenantId);
    cfg.put("metadataSourceId", metadataSourceId);
    Future<String> deploy = Future.future();
    vertx.deployVerticle(
        verticle, new DeploymentOptions().setConfig(cfg).setWorker(true), deploy.completer());

    deploy.setHandler(
        ar -> {
          if (ar.failed()) {
            logger.error(
                String.format(
                    "Failed to deploy SelectVerticle for metadata source %s and for tenant %s: %s",
                    metadataSourceId, tenantId, ar.cause().getMessage()),
                ar.cause());
          }
        });
  }
}
