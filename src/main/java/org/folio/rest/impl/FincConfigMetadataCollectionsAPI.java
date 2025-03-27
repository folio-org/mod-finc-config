package org.folio.rest.impl;

import static org.folio.okapi.common.XOkapiHeaders.TENANT;
import static org.folio.rest.utils.Constants.MODULE_TENANT;

import io.vertx.core.*;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.finc.dao.MetadataSourcesDAO;
import org.folio.finc.dao.MetadataSourcesDAOImpl;
import org.folio.rest.RestVerticle;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollectionWithFilters;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollectionWithFiltersCollection;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollectionsGetOrder;
import org.folio.rest.jaxrs.model.FincConfigMetadataSource;
import org.folio.rest.jaxrs.model.MdSource;
import org.folio.rest.jaxrs.resource.FincConfigMetadataCollections;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.utils.Constants;

/**
 * Manages metadata collections for ui-finc-config
 *
 * <p>ATTENTION: API works tenant agnostic. Thus, don't use 'x-okapi-tenant' header, but {@value
 * Constants#MODULE_TENANT} as tenant.
 */
public class FincConfigMetadataCollectionsAPI implements FincConfigMetadataCollections {

  private static final String TABLE_NAME = "metadata_collections";
  private static final String TABLE_NAME_W_FILTERS = "metadata_collections_w_filters";
  private static final MetadataSourcesDAO metadataSourcesDAO = new MetadataSourcesDAOImpl();
  private final Logger logger = LogManager.getLogger(FincConfigMetadataCollectionsAPI.class);

  private String determineTableName(boolean includeFilteredBy) {
    return includeFilteredBy ? TABLE_NAME_W_FILTERS : TABLE_NAME;
  }

  private Class<?> determineClass(boolean includeFilteredBy) {
    return includeFilteredBy
        ? FincConfigMetadataCollectionWithFilters.class
        : FincConfigMetadataCollection.class;
  }

  private Class<?> determineCollectionClass(boolean includeFilteredBy) {
    return includeFilteredBy
        ? FincConfigMetadataCollectionWithFiltersCollection.class
        : org.folio.rest.jaxrs.model.FincConfigMetadataCollections.class;
  }

  private String determineTotalRecords(boolean includeFilteredBy, String totalRecords) {
    return includeFilteredBy ? "none" : totalRecords;
  }

  @Override
  @Validate
  public void getFincConfigMetadataCollections(
      String query,
      String orderBy,
      FincConfigMetadataCollectionsGetOrder order,
      String totalRecords,
      int offset,
      int limit,
      boolean includeFilteredBy,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {

    logger.debug("Getting metadata collections");

    okapiHeaders.put(TENANT, MODULE_TENANT);
    PgUtil.get(
        determineTableName(includeFilteredBy),
        determineClass(includeFilteredBy),
        determineCollectionClass(includeFilteredBy),
        query,
        determineTotalRecords(includeFilteredBy, totalRecords),
        offset,
        limit,
        okapiHeaders,
        vertxContext,
        org.folio.rest.impl.GetFincConfigMetadataCollectionsResponse.class,
        asyncResultHandler);
  }

  @Override
  @Validate
  public void postFincConfigMetadataCollections(
      FincConfigMetadataCollection entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    logger.debug("Posting metadata collection");
    okapiHeaders.put(RestVerticle.OKAPI_HEADER_TENANT, MODULE_TENANT);

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
      boolean includeFilteredBy,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    logger.debug("Getting single metadata collection by id: {}", id);
    okapiHeaders.put(RestVerticle.OKAPI_HEADER_TENANT, MODULE_TENANT);
    PgUtil.getById(
        determineTableName(includeFilteredBy),
        determineClass(includeFilteredBy),
        id,
        okapiHeaders,
        vertxContext,
        org.folio.rest.impl.GetFincConfigMetadataCollectionsResponse.class,
        asyncResultHandler);
  }

  @Override
  @Validate
  public void deleteFincConfigMetadataCollectionsById(
      String id,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    logger.debug("Delete metadata collection: {}", id);
    okapiHeaders.put(RestVerticle.OKAPI_HEADER_TENANT, MODULE_TENANT);
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
      FincConfigMetadataCollection entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    logger.debug("Update metadata collection: {}", id);
    okapiHeaders.put(RestVerticle.OKAPI_HEADER_TENANT, MODULE_TENANT);

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
