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
import javax.ws.rs.core.Response;
import org.folio.finc.select.exception.FincSelectException;
import org.folio.finc.select.isil.filter.IsilFilter;
import org.folio.finc.select.isil.filter.MetadataCollectionIsilFilter;
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.jaxrs.model.FincSelectMetadataCollection;
import org.folio.rest.jaxrs.model.FincSelectMetadataCollectionsGetOrder;
import org.folio.rest.jaxrs.model.Select;
import org.folio.rest.jaxrs.resource.FincSelectMetadataCollections.GetFincSelectMetadataCollectionsByIdResponse;
import org.folio.rest.jaxrs.resource.FincSelectMetadataCollections.GetFincSelectMetadataCollectionsResponse;
import org.folio.rest.jaxrs.resource.FincSelectMetadataCollections.PutFincSelectMetadataCollectionsSelectByIdResponse;
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

/**
 * Helper class to fetch metadata collections for finc-select. Filters out information if an item is
 * selected by resp. permitted for another organization than the requesting one.
 */
public class MetadataCollectionsHelper {
  private static final String ID_FIELD = "_id";
  private static final String TABLE_NAME = "metadata_collections";
  private final Messages messages = Messages.getInstance();
  private final Logger logger = LoggerFactory.getLogger(MetadataCollectionsHelper.class);
  private final IsilHelper isilHelper;
  private final IsilFilter<FincSelectMetadataCollection, FincConfigMetadataCollection> isilFilter;

  public MetadataCollectionsHelper(Vertx vertx, String tenantId) {
    PostgresClient.getInstance(vertx).setIdField(ID_FIELD);
    this.isilHelper = new IsilHelper(vertx, tenantId);
    this.isilFilter = new MetadataCollectionIsilFilter();
  }

  private static FincConfigMetadataCollection setSelectStatus(
      FincConfigMetadataCollection metadataCollection, Select select, String isil)
      throws FincSelectException {
    List<String> permittedFor = metadataCollection.getPermittedFor();
    boolean isPermitted = permittedFor.contains(isil);

    if (!isPermitted) {
      throw new FincSelectException("Selecting this metadata collection is not permitted");
    }

    List<String> selectedBy = metadataCollection.getSelectedBy();
    Boolean doSelect = select.getSelect();
    if (doSelect && !selectedBy.contains(isil)) {
      selectedBy.add(isil);
    } else if (!doSelect) {
      selectedBy.remove(isil);
    }
    metadataCollection.setSelectedBy(selectedBy);
    return metadataCollection;
  }

  private CQLWrapper getCQL(String query, int limit, int offset) throws FieldException {
    CQL2PgJSON cql2PgJSON = new CQL2PgJSON(Arrays.asList(TABLE_NAME + ".jsonb"));
    return new CQLWrapper(cql2PgJSON, query)
        .setLimit(new Limit(limit))
        .setOffset(new Offset(offset));
  }

  public void getFincSelectMetadataCollections(
      String query,
      String orderBy,
      FincSelectMetadataCollectionsGetOrder order,
      int offset,
      int limit,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    logger.debug("Getting metadata collections for select");
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
                      FincConfigMetadataCollection.class,
                      fieldList,
                      cql,
                      true,
                      false,
                      reply -> {
                        try {
                          if (reply.succeeded()) {
                            org.folio.rest.jaxrs.model.FincSelectMetadataCollections
                                collectionsCollection =
                                    new org.folio.rest.jaxrs.model.FincSelectMetadataCollections();
                            List<FincConfigMetadataCollection> results =
                                reply.result().getResults();
                            isilHelper
                                .getIsilForTenant(tenantId, okapiHeaders, vertxContext)
                                .setHandler(
                                    isilResult -> {
                                      if (isilResult.succeeded()) {
                                        String isil = isilResult.result();
                                        List<FincSelectMetadataCollection> transformedCollections =
                                            isilFilter.filterForIsil(results, isil);
                                        collectionsCollection.setFincSelectMetadataCollections(
                                            transformedCollections);
                                        collectionsCollection.setTotalRecords(
                                            transformedCollections.size());
                                        asyncResultHandler.handle(
                                            Future.succeededFuture(
                                                GetFincSelectMetadataCollectionsResponse
                                                    .respond200WithApplicationJson(
                                                        collectionsCollection)));
                                      } else {
                                        asyncResultHandler.handle(
                                            Future.succeededFuture(
                                                GetFincSelectMetadataCollectionsResponse
                                                    .respond500WithTextPlain(
                                                        isilResult.cause().getMessage())));
                                      }
                                    });
                          } else {
                            asyncResultHandler.handle(
                                Future.succeededFuture(
                                    GetFincSelectMetadataCollectionsResponse
                                        .respond500WithTextPlain(
                                            messages.getMessage(
                                                lang, MessageConsts.InternalServerError))));
                          }
                        } catch (Exception e) {
                          logger.debug(e.getLocalizedMessage());
                          asyncResultHandler.handle(
                              Future.succeededFuture(
                                  GetFincSelectMetadataCollectionsResponse.respond500WithTextPlain(
                                      messages.getMessage(
                                          lang, MessageConsts.InternalServerError))));
                        }
                      });
            } catch (IllegalStateException e) {
              logger.debug("IllegalStateException: " + e.getLocalizedMessage());
              asyncResultHandler.handle(
                  Future.succeededFuture(
                      GetFincSelectMetadataCollectionsResponse.respond400WithTextPlain(
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
                        GetFincSelectMetadataCollectionsResponse.respond400WithTextPlain(
                            "CQL Parsing Error for '" + "" + "': " + cause.getLocalizedMessage())));
              } else {
                asyncResultHandler.handle(
                    io.vertx.core.Future.succeededFuture(
                        GetFincSelectMetadataCollectionsResponse.respond500WithTextPlain(
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
                GetFincSelectMetadataCollectionsResponse.respond400WithTextPlain(
                    "CQL Parsing Error for '" + "" + "': " + e.getLocalizedMessage())));
      } else {
        asyncResultHandler.handle(
            io.vertx.core.Future.succeededFuture(
                GetFincSelectMetadataCollectionsResponse.respond500WithTextPlain(
                    messages.getMessage(lang, MessageConsts.InternalServerError))));
      }
    }
  }

  public void getFincSelectMetadataCollectionsById(
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
                      FincConfigMetadataCollection.class,
                      criterion,
                      true,
                      false,
                      getReply -> {
                        if (getReply.failed()) {
                          asyncResultHandler.handle(
                              Future.succeededFuture(
                                  GetFincSelectMetadataCollectionsByIdResponse
                                      .respond500WithTextPlain(
                                          messages.getMessage(
                                              lang, MessageConsts.InternalServerError))));
                        } else {
                          List<FincConfigMetadataCollection> metadataCollections =
                              getReply.result().getResults();
                          if (metadataCollections.size() < 1) {
                            asyncResultHandler.handle(
                                Future.succeededFuture(
                                    GetFincSelectMetadataCollectionsByIdResponse
                                        .respond404WithTextPlain(
                                            "Metadata Collection "
                                                + messages.getMessage(
                                                    lang, MessageConsts.ObjectDoesNotExist))));
                          } else if (metadataCollections.size() > 1) {
                            logger.debug("Multiple metadata collections found with the same id");
                            asyncResultHandler.handle(
                                Future.succeededFuture(
                                    GetFincSelectMetadataCollectionsByIdResponse
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
                                        FincSelectMetadataCollection result =
                                            isilFilter.filterForIsil(
                                                metadataCollections.get(0), isil);
                                        asyncResultHandler.handle(
                                            Future.succeededFuture(
                                                GetFincSelectMetadataCollectionsByIdResponse
                                                    .respond200WithApplicationJson(result)));
                                      } else {
                                        asyncResultHandler.handle(
                                            Future.succeededFuture(
                                                GetFincSelectMetadataCollectionsByIdResponse
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
                      GetFincSelectMetadataCollectionsByIdResponse.respond500WithTextPlain(
                          messages.getMessage(lang, MessageConsts.InternalServerError))));
            }
          });
    } catch (Exception e) {
      asyncResultHandler.handle(
          Future.succeededFuture(
              GetFincSelectMetadataCollectionsByIdResponse.respond500WithTextPlain(
                  messages.getMessage(lang, MessageConsts.InternalServerError))));
    }
  }

  public void putFincSelectMetadataCollectionsSelectById(
      String id,
      String lang,
      Select entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    try {
      vertxContext.runOnContext(
          v -> {
            String fincId = Constants.MODULE_TENANT;
            String tenantId =
                TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
            Criteria idCrit =
                new Criteria()
                    .addField(ID_FIELD)
                    .setJSONB(false)
                    .setOperation("=")
                    .setValue("'" + id + "'");
            Criterion criterion = new Criterion(idCrit);
            logger.debug("Using criterion: " + criterion.toString());
            this.isilHelper
                .getIsilForTenant(tenantId, okapiHeaders, vertxContext)
                .setHandler(
                    isilResult -> {
                      if (isilResult.succeeded()) {
                        String isil = isilResult.result();
                        try {
                          PostgresClient.getInstance(vertxContext.owner(), fincId)
                              .get(
                                  TABLE_NAME,
                                  FincConfigMetadataCollection.class,
                                  criterion,
                                  true,
                                  false,
                                  getReply -> {
                                    if (getReply.failed()) {
                                      logger.debug(
                                          "Error querying existing metadata collections: "
                                              + getReply.cause().getLocalizedMessage());
                                      asyncResultHandler.handle(
                                          Future.succeededFuture(
                                              PutFincSelectMetadataCollectionsSelectByIdResponse
                                                  .respond500WithTextPlain(
                                                      messages.getMessage(
                                                          lang,
                                                          MessageConsts.InternalServerError))));
                                    } else {
                                      List<FincConfigMetadataCollection> mdCollections =
                                          getReply.result().getResults();
                                      FincConfigMetadataCollection metadataCollection =
                                          mdCollections.get(0);
                                      FincConfigMetadataCollection updated;
                                      try {
                                        updated = setSelectStatus(metadataCollection, entity, isil);
                                      } catch (FincSelectException e) {
                                        asyncResultHandler.handle(
                                            Future.succeededFuture(
                                                PutFincSelectMetadataCollectionsSelectByIdResponse
                                                    .respond404WithTextPlain(e.getMessage())));
                                        return;
                                      }
                                      try {
                                        PostgresClient.getInstance(vertxContext.owner(), fincId)
                                            .update(
                                                TABLE_NAME,
                                                updated,
                                                id,
                                                putReply -> {
                                                  try {
                                                    if (putReply.failed()) {
                                                      asyncResultHandler.handle(
                                                          Future.succeededFuture(
                                                              PutFincSelectMetadataCollectionsSelectByIdResponse
                                                                  .respond500WithTextPlain(
                                                                      putReply
                                                                          .cause()
                                                                          .getMessage())));
                                                    } else {
                                                      asyncResultHandler.handle(
                                                          Future.succeededFuture(
                                                              PutFincSelectMetadataCollectionsSelectByIdResponse
                                                                  .respond204()));
                                                    }
                                                  } catch (Exception e) {
                                                    asyncResultHandler.handle(
                                                        Future.succeededFuture(
                                                            PutFincSelectMetadataCollectionsSelectByIdResponse
                                                                .respond500WithTextPlain(
                                                                    messages.getMessage(
                                                                        lang,
                                                                        MessageConsts
                                                                            .InternalServerError))));
                                                  }
                                                });
                                      } catch (Exception e) {
                                        asyncResultHandler.handle(
                                            Future.succeededFuture(
                                                PutFincSelectMetadataCollectionsSelectByIdResponse
                                                    .respond500WithTextPlain(
                                                        messages.getMessage(
                                                            lang,
                                                            MessageConsts.InternalServerError))));
                                      }
                                    }
                                  });
                        } catch (Exception e) {
                          logger.debug(e.getLocalizedMessage());
                          asyncResultHandler.handle(
                              Future.succeededFuture(
                                  PutFincSelectMetadataCollectionsSelectByIdResponse
                                      .respond500WithTextPlain(
                                          messages.getMessage(
                                              lang, MessageConsts.InternalServerError))));
                        }
                      } else {
                        asyncResultHandler.handle(
                            Future.succeededFuture(
                                PutFincSelectMetadataCollectionsSelectByIdResponse
                                    .respond500WithTextPlain(
                                        messages.getMessage(
                                            lang, MessageConsts.InternalServerError))));
                      }
                    });
          });
    } catch (Exception e) {
      logger.debug(e.getLocalizedMessage());
      asyncResultHandler.handle(
          Future.succeededFuture(
              PutFincSelectMetadataCollectionsSelectByIdResponse.respond500WithTextPlain(
                  messages.getMessage(lang, MessageConsts.InternalServerError))));
    }
  }
}
