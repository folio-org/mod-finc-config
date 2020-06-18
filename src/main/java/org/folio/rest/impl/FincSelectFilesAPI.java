package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.folio.finc.dao.IsilDAO;
import org.folio.finc.dao.IsilDAOImpl;
import org.folio.finc.dao.SelectFileDAO;
import org.folio.finc.dao.SelectFileDAOImpl;
import org.folio.finc.model.File;
import org.folio.finc.select.IsilHelper;
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.resource.FincSelectFiles;
import org.folio.rest.tools.utils.TenantTool;

public class FincSelectFilesAPI extends FincFileHandler implements FincSelectFiles {

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
        .getIsilForTenant(tenantId, vertxContext)
        .compose(isil -> selectFileDAO.getById(id, isil, vertxContext))
        .setHandler(
            ar ->
                handleAsyncFileReponse(
                    ar,
                    GetFincSelectFilesByIdResponse::respond200WithApplicationOctetStream,
                    GetFincSelectFilesByIdResponse::respond404WithTextPlain,
                    GetFincSelectFilesByIdResponse::respond500WithTextPlain,
                    asyncResultHandler)
        );
  }

  @Override
  public void postFincSelectFiles(
      InputStream entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    String tenantId =
        TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));

    byte[] bytes = new byte[0];
    try {
      bytes = IOUtils.toByteArray(entity);
    } catch (IOException e) {
      asyncResultHandler.handle(
          Future.succeededFuture(
              PostFincSelectFilesResponse.respond404WithTextPlain("Cannot read file")));
    }
    String base64Data = Base64.getEncoder().encodeToString(bytes);
    String uuid = UUID.randomUUID().toString();

    isilHelper.fetchIsil(tenantId, vertxContext);

    isilDAO
        .getIsilForTenant(tenantId, vertxContext)
        .compose(
            isil -> {
              File file = new File().withData(base64Data).withId(uuid).withIsil(isil);
              return selectFileDAO.upsert(file, uuid, vertxContext);
            })
        .setHandler(
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
        .setHandler(
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
