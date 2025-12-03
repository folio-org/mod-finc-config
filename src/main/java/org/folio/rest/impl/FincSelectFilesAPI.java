package org.folio.rest.impl;

import static org.folio.rest.utils.Constants.*;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.finc.dao.IsilDAO;
import org.folio.finc.dao.IsilDAOImpl;
import org.folio.finc.dao.SelectFileDAO;
import org.folio.finc.dao.SelectFileDAOImpl;
import org.folio.finc.model.File;
import org.folio.finc.select.IsilHelper;
import org.folio.rest.RestVerticle;
import org.folio.rest.annotations.Stream;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.resource.FincSelectFiles;
import org.folio.rest.tools.utils.TenantTool;

/** Manages files for ui-finc-select, hence depends on isil/tenant. */
public class FincSelectFilesAPI extends FincFileHandler implements FincSelectFiles {
  private static final Logger log = LogManager.getLogger(FincSelectFilesAPI.class);

  private final IsilHelper isilHelper;
  private final IsilDAO isilDAO;
  private final SelectFileDAO selectFileDAO;

  public FincSelectFilesAPI() {
    this.isilHelper = new IsilHelper();
    this.selectFileDAO = new SelectFileDAOImpl();
    this.isilDAO = new IsilDAOImpl();
  }

  @Override
  public void getFincSelectFilesById(
      String id,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {

    String tenantId =
        TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
    isilDAO
        .withIsilForTenant(tenantId, vertxContext)
        .compose(isil -> selectFileDAO.getById(id, isil, vertxContext))
        .onComplete(
            ar ->
                handleAsyncFileResponse(
                    ar,
                    GetFincSelectFilesByIdResponse::respond200WithApplicationOctetStream,
                    GetFincSelectFilesByIdResponse::respond404WithTextPlain,
                    GetFincSelectFilesByIdResponse::respond500WithTextPlain,
                    asyncResultHandler));
  }

  @Override
  @Validate
  @Stream
  public void postFincSelectFiles(
      InputStream entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {

    handleStreamUpload(
        entity,
        okapiHeaders,
        asyncResultHandler,
        vertxContext,
        new StreamUploadResponses() {
          @Override
          public Response streamAborted() {
            return PostFincSelectFilesResponse.respond400WithTextPlain("Stream aborted");
          }

          @Override
          public Response fileSizeExceeded() {
            return PostFincSelectFilesResponse.respond413WithTextPlain(
                "File size exceeds maximum allowed size of " + MAX_UPLOAD_FILE_SIZE_MB + " MB");
          }

          @Override
          public Response internalError() {
            return PostFincSelectFilesResponse.respond500WithTextPlain("Internal server error");
          }
        },
        IOUtils::toByteArray,
        this::createFile);
  }

  private void createFile(
      String streamId,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    ByteArrayOutputStream baos = getAndRemoveStream(streamId);

    if (baos == null) {
      log.error("No data found for stream {}", streamId);
      asyncResultHandler.handle(
          Future.succeededFuture(
              PostFincSelectFilesResponse.respond500WithTextPlain("No upload data found")));
      return;
    }

    byte[] bytes = baos.toByteArray();
    String base64Data = Base64.getEncoder().encodeToString(bytes);
    String uuid = UUID.randomUUID().toString();
    String tenantId =
        TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
    isilHelper.fetchIsil(tenantId, vertxContext);

    isilDAO
        .withIsilForTenant(tenantId, vertxContext)
        .compose(
            isil -> {
              File file = new File().withData(base64Data).withId(uuid).withIsil(isil);
              return selectFileDAO.upsert(file, uuid, vertxContext);
            })
        .onComplete(
            ar -> {
              if (ar.succeeded()) {
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        PostFincSelectFilesResponse.respond200WithTextPlain(uuid)));
              } else {
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        PostFincSelectFilesResponse.respond500WithTextPlain(
                            "Internal server error")));
              }
            });
  }

  @Override
  public void deleteFincSelectFilesById(
      String id,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {

    String tenantId =
        TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));

    isilDAO
        .withIsilForTenant(tenantId, vertxContext)
        .compose(isil -> selectFileDAO.deleteById(id, isil, vertxContext))
        .onComplete(
            ar -> {
              if (ar.succeeded()) {
                asyncResultHandler.handle(
                    Future.succeededFuture(DeleteFincSelectFilesByIdResponse.respond204()));
              } else {
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        DeleteFincSelectFilesByIdResponse.respond500WithTextPlain(ar.cause())));
              }
            });
  }
}
