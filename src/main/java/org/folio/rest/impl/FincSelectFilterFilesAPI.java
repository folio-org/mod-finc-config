package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import org.folio.finc.dao.FilterFileDAO;
import org.folio.finc.dao.FilterFileDAOImpl;
import org.folio.finc.select.IsilHelper;
import org.folio.rest.RestVerticle;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.FincSelectFilterFile;
import org.folio.rest.jaxrs.model.FincSelectFilterFilesGetOrder;
import org.folio.rest.jaxrs.resource.FincSelectFilterFiles;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.messages.MessageConsts;
import org.folio.rest.tools.messages.Messages;
import org.folio.rest.tools.utils.TenantTool;

public class FincSelectFilterFilesAPI implements FincSelectFilterFiles {

  private static final String ID_FIELD = "id";
  private static final String TABLE_NAME = "filter_files";
  private final Logger logger = LoggerFactory.getLogger(FincSelectFilterFilesAPI.class);
  private final Messages messages = Messages.getInstance();
  private final IsilHelper isilHelper;
  private final FilterFileDAO filterFileDAO;

  public FincSelectFilterFilesAPI(Vertx vertx, String tenantId) {
    PostgresClient.getInstance(vertx);
    this.isilHelper = new IsilHelper(vertx, tenantId);
    this.filterFileDAO = new FilterFileDAOImpl();
  }

  @Override
  @Validate
  public void getFincSelectFilterFiles(
      String query,
      String orderBy,
      FincSelectFilterFilesGetOrder order,
      int offset,
      int limit,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {

    logger.debug("Getting finc select filter files");

    String tenantId =
        TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));

    isilHelper
        .fetchIsil(tenantId, vertxContext)
        .compose(isil -> filterFileDAO.getAll(query, offset, limit, isil, vertxContext))
        .setHandler(
            ar -> {
              if (ar.succeeded()) {
                org.folio.rest.jaxrs.model.FincSelectFilterFiles filterFiles = ar.result();
                List<FincSelectFilterFile> withoutIsils =
                    filterFiles.getFincSelectFilterFiles().stream()
                        .map(
                            filter -> {
                              filter.setIsil(null);
                              return filter;
                            })
                        .collect(Collectors.toList());
                filterFiles.setFincSelectFilterFiles(withoutIsils);
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        GetFincSelectFilterFilesResponse.respond200WithApplicationJson(
                            filterFiles)));
              } else {
                asyncResultHandler.handle(
                    io.vertx.core.Future.succeededFuture(
                        GetFincSelectFilterFilesResponse.respond500WithTextPlain(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
              }
            });
  }

  @Override
  @Validate
  public void postFincSelectFilterFiles(
      String lang,
      FincSelectFilterFile entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {

    logger.debug("Posting finc select filter file");

    String tenantId =
        TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));

    isilHelper
        .fetchIsil(tenantId, vertxContext)
        .compose(isil -> addMetadataAndSave(entity, isil, vertxContext))
        .setHandler(
            ar -> {
              if (ar.succeeded()) {
                FincSelectFilterFile fincSelectFilterFile = ar.result();
                fincSelectFilterFile.setIsil(null);
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        PostFincSelectFilterFilesResponse.respond201WithApplicationJson(
                            entity, PostFincSelectFilterFilesResponse.headersFor201())));
              } else {
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        GetFincSelectFilterFilesResponse.respond500WithTextPlain(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
              }
            });
  }

  @Override
  @Validate
  public void getFincSelectFilterFilesById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    logger.debug("Get single select filter file by id");

    String tenantId =
        TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));

    isilHelper
        .fetchIsil(tenantId, vertxContext)
        .compose(isil -> filterFileDAO.getById(id, isil, vertxContext))
        .setHandler(
            ar -> {
              if (ar.succeeded()) {
                FincSelectFilterFile result = ar.result();
                if (result == null) {
                  asyncResultHandler.handle(
                      Future.succeededFuture(
                          GetFincSelectFilterFilesByIdResponse.respond404WithTextPlain(
                              messages.getMessage(lang, MessageConsts.ObjectDoesNotExist))));
                } else {
                  result.setIsil(null);
                  asyncResultHandler.handle(
                      Future.succeededFuture(
                          GetFincSelectFilterFilesByIdResponse.respond200WithApplicationJson(
                              result)));
                }
              } else {
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        GetFincSelectFilterFilesByIdResponse.respond500WithTextPlain(ar.cause())));
              }
            });
  }

  @Override
  @Validate
  public void deleteFincSelectFilterFilesById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    logger.debug("Delete single select filter file by id");

    String tenantId =
        TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
    isilHelper
        .fetchIsil(tenantId, vertxContext)
        .compose(isil -> filterFileDAO.deleteById(id, isil, vertxContext))
        .setHandler(
            ar -> {
              if (ar.succeeded()) {
                Integer updated = ar.result();
                if (updated == 0) {
                  asyncResultHandler.handle(
                      Future.succeededFuture(
                          DeleteFincSelectFilterFilesByIdResponse.respond404WithTextPlain(
                              messages.getMessage(lang, MessageConsts.ObjectDoesNotExist))));
                } else {
                  asyncResultHandler.handle(
                      Future.succeededFuture(DeleteFincSelectFilterFilesByIdResponse.respond204()));
                }
              } else {
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        DeleteFincSelectFilterFilesByIdResponse.respond500WithTextPlain(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
              }
            });
  }

  @Override
  @Validate
  public void putFincSelectFilterFilesById(
      String id,
      String lang,
      FincSelectFilterFile entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    asyncResultHandler.handle(
        Future.succeededFuture(PutFincSelectFilterFilesByIdResponse.status(501).build()));
  }

  private Future<FincSelectFilterFile> addMetadataAndSave(
      FincSelectFilterFile entity, String isil, Context vertxContext) {
    FincSelectFilterFile file =
        entity
            .withIsil(isil)
            .withCreated(LocalDateTime.now().toString());
    return filterFileDAO.insert(file, vertxContext);
  }
}
