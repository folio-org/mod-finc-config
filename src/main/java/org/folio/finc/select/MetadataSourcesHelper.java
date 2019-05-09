package org.folio.finc.select;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.FincConfigMetadataSource;
import org.folio.rest.jaxrs.model.FincSelectMetadataSource;
import org.folio.rest.jaxrs.model.FincSelectMetadataSourcesGetOrder;
import org.folio.rest.jaxrs.resource.FincSelectMetadataSources.GetFincSelectMetadataSourcesByIdResponse;
import org.folio.rest.jaxrs.resource.FincSelectMetadataSources.GetFincSelectMetadataSourcesResponse;
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

  public MetadataSourcesHelper(Vertx vertx, String tenantId) {
    PostgresClient.getInstance(vertx).setIdField(ID_FIELD);
    this.isilHelper = new IsilHelper(vertx, tenantId);
  }

  public static List<FincSelectMetadataSource> filterForIsil(
      List<FincConfigMetadataSource> metadataSources, String isil) {
    return metadataSources.stream()
        .map(metadataSource -> MetadataSourcesHelper.filterForIsil(metadataSource, isil))
        .collect(Collectors.toList());
  }

  /**
   * Filters selected and permitted status for given isil and hides information about other isils
   *
   * @param metadataSource
   * @param isil
   * @return
   */
  private static FincSelectMetadataSource filterForIsil(
      FincConfigMetadataSource metadataSource, String isil) {
    List<String> selectedBy = metadataSource.getSelectedBy();
    boolean selected = selectedBy.contains(isil);
    metadataSource.setSelectedBy(null);

    FincSelectMetadataSource metadataSourceSelect =
        Json.mapper.convertValue(metadataSource, FincSelectMetadataSource.class);
    metadataSourceSelect.setSelected(selected);
    return metadataSourceSelect;
  }

  private CQLWrapper getCQL(String query, int limit, int offset) throws FieldException {
    CQL2PgJSON cql2PgJSON = new CQL2PgJSON(Arrays.asList(TABLE_NAME + ".jsonb"));
    return new CQLWrapper(cql2PgJSON, query)
        .setLimit(new Limit(limit))
        .setOffset(new Offset(offset));
  }

  public void getFincSelectMetadataSources(
      String query,
      String orderBy,
      FincSelectMetadataSourcesGetOrder order,
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
                                            filterForIsil(results, isil);
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
                                                        messages.getMessage(
                                                            lang,
                                                            MessageConsts.InternalServerError))));
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
                          if (metadataSources.size() < 1) {
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
                                            MetadataSourcesHelper.filterForIsil(
                                                metadataSources.get(0), isil);
                                        asyncResultHandler.handle(
                                            Future.succeededFuture(
                                                GetFincSelectMetadataSourcesByIdResponse
                                                    .respond200WithApplicationJson(result)));
                                      } else {
                                        asyncResultHandler.handle(
                                            Future.succeededFuture(
                                                GetFincSelectMetadataSourcesByIdResponse
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
}
