package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
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

import static org.folio.rest.utils.Constants.*;

public class FincSelectFilesAPI extends FincFileHandler implements FincSelectFiles {

  private final IsilHelper isilHelper;
  private final IsilDAO isilDAO;
  private final SelectFileDAO selectFileDAO;
  private Map<String, byte[]> requestedBytes = new HashMap<>();

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
        .getIsilForTenant(tenantId, vertxContext)
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
    String streamId = okapiHeaders.get(STREAM_ID);
    try (InputStream bis = new BufferedInputStream(entity)) {
      if (Objects.isNull(okapiHeaders.get(STREAM_COMPLETE))) {
        processBytesArrayFromStream(bis, streamId);
      } else if (Objects.nonNull(okapiHeaders.get(STREAM_ABORT))) {
        asyncResultHandler.handle(
            Future.succeededFuture(
                PostFincSelectFilesResponse.respond400WithTextPlain("Stream aborted")));
      } else {
        // stream is completed
        createFile(streamId, okapiHeaders, asyncResultHandler, vertxContext);
      }
    } catch (IOException e) {
      asyncResultHandler.handle(
          Future.succeededFuture(
              PostFincSelectFilesResponse.respond500WithTextPlain("Internal server error")));
    }
  }

  private void createFile(
      String streamId,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    byte[] bytes = requestedBytes.get(streamId);
    requestedBytes.remove(streamId);
    String base64Data = Base64.getEncoder().encodeToString(bytes);
    String uuid = UUID.randomUUID().toString();
    String tenantId =
        TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
    isilHelper.fetchIsil(tenantId, vertxContext);

    isilDAO
        .getIsilForTenant(tenantId, vertxContext)
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
        .getIsilForTenant(tenantId, vertxContext)
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

  private void processBytesArrayFromStream(InputStream is, String streamId) throws IOException {
    byte[] requestBytesArray = requestedBytes.get(streamId);
    if (requestBytesArray == null) {
      requestBytesArray = new byte[0];
    }
    requestBytesArray = ArrayUtils.addAll(requestBytesArray, IOUtils.toByteArray(is));
    requestedBytes.put(streamId, requestBytesArray);
  }
}
