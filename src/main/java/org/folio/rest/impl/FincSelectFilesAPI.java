package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.folio.finc.dao.FileDAO;
import org.folio.finc.dao.FileDAOImpl;
import org.folio.finc.model.File;
import org.folio.finc.select.IsilHelper;
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.resource.FincSelectFiles;
import org.folio.rest.tools.utils.BinaryOutStream;
import org.folio.rest.tools.utils.TenantTool;

public class FincSelectFilesAPI implements FincSelectFiles {

  private final IsilHelper isilHelper;
  private final FileDAO fileDAO;

  public FincSelectFilesAPI(Vertx vertx, String tenantId) {
    this.isilHelper = new IsilHelper(vertx, tenantId);
    this.fileDAO = new FileDAOImpl();
  }

  /* @Override
  public void postFincSelectFiles(
      InputStream entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    String fincId = Constants.MODULE_TENANT;
    String tenantId =
        TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));

    byte[] bytes = new byte[0];
    try {
      bytes = IOUtils.toByteArray(entity);
    } catch (IOException e) {
      e.printStackTrace();
    }
    //    byte[] base64Data = Base64.encodeBase64(bytes);
    String base64Data = Base64.getEncoder().encodeToString(bytes);

    try {
      System.out.println(
          new String(bytes, StandardCharsets.ISO_8859_1)
              + " ---- "
              + new String(Base64.getDecoder().decode(base64Data), "utf-8"));
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }

    FileInfo fileInfo =
        new FileInfo()
            .withId(UUID.randomUUID().toString())
            .withIsil("DE-15")
            .withModified(LocalDateTime.now().toString())
            .withData(base64Data)
            .withName("foo");
    fileInfo.getId();
  }*/

  /*  @Override
  public void postFincSelectFiles(
      Object entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    String fincId = Constants.MODULE_TENANT;
    String tenantId =
        TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));

    byte[] bytes = new byte[0];
    */
  /*try {
    bytes = IOUtils.toByteArray(entity);
  } catch (IOException e) {
    e.printStackTrace();
  }*/
  /*
    String base64Data = Base64.getEncoder().encodeToString(bytes);

    try {
      System.out.println(
          new String(bytes, StandardCharsets.ISO_8859_1)
              + " ---- "
              + new String(Base64.getDecoder().decode(base64Data), "utf-8"));
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }

    FileInfo fileInfo =
        new FileInfo()
            .withId(UUID.randomUUID().toString())
            .withIsil("DE-15")
            .withModified(LocalDateTime.now().toString())
            .withData(base64Data)
            .withName("foo");
    fileInfo.getId();
  }*/

  @Override
  public void getFincSelectFilesById(
      String id,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {

    String tenantId =
        TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));

    isilHelper
        .fetchIsil(tenantId, vertxContext)
        .compose(isil -> fileDAO.getById(id, isil, vertxContext))
        .setHandler(
            ar -> {
              if (ar.succeeded()) {
                File file = ar.result();
                if (file == null) {
                  asyncResultHandler.handle(
                      Future.succeededFuture(
                          GetFincSelectFilesByIdResponse.respond500WithTextPlain("Not found")));
                } else {
                  String dataAsString = file.getData();
                  byte[] decoded = Base64.getDecoder().decode(dataAsString);

                  BinaryOutStream binaryOutStream = new BinaryOutStream();
                  binaryOutStream.setData(decoded);
                  asyncResultHandler.handle(
                      Future.succeededFuture(
                          GetFincSelectFilesByIdResponse.respond200WithApplicationOctetStream(
                              binaryOutStream)));
                }
              } else {
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        GetFincSelectFilesByIdResponse.respond500WithTextPlain(ar.cause())));
              }
            });
  }

  @Override
  public void putFincSelectFilesById(
      String id,
      InputStream entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {

    System.out.println("HERE");

    /*String tenantId =
        TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));

    byte[] bytes = new byte[0];
    try {
      bytes = IOUtils.toByteArray(entity);
    } catch (IOException e) {
      e.printStackTrace();
    }
    String base64Data = Base64.getEncoder().encodeToString(bytes);

    isilHelper
        .fetchIsil(tenantId, vertxContext)
        .compose(
            isil -> {
              File file = new File().withData(base64Data).withId(id).withIsil(isil);
              return fileDAO.upsert(file, id, vertxContext);
            })
        .setHandler(
            ar -> {
              if (ar.succeeded()) {
                asyncResultHandler.handle(
                    Future.succeededFuture(PutFincSelectFilesByIdResponse.respond204()));
              } else {
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        PutFincSelectFilesByIdResponse.respond500WithTextPlain(
                            "Internal server error")));
              }
            });*/
  }

  @Override
  public void deleteFincSelectFilesById(
      String id,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {}
}
