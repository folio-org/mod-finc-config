package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import java.util.Base64;
import java.util.function.Function;
import org.folio.finc.model.File;
import org.folio.rest.jaxrs.resource.support.ResponseDelegate;
import org.folio.rest.tools.utils.BinaryOutStream;

public abstract class FincFileHandler {

  protected void handleAsyncFileResponse(AsyncResult<File> ar,
      Function<BinaryOutStream, ResponseDelegate> func200,
      Function<String, ResponseDelegate> func404, Function<Throwable, ResponseDelegate> func500,
      io.vertx.core.Handler<io.vertx.core.AsyncResult<javax.ws.rs.core.Response>> asyncResultHandler) {
    if (ar.succeeded()) {
      handleFileResponse(
          ar.result(),
          func200,
          func404,
          asyncResultHandler);
    } else {
      asyncResultHandler.handle(
          Future.succeededFuture(
              func500.apply(ar.cause())));
    }
  }

  private void handleFileResponse(File file,
      Function<BinaryOutStream, ResponseDelegate> succeedFunc,
      Function<String, ResponseDelegate> failFunc,
      io.vertx.core.Handler<io.vertx.core.AsyncResult<javax.ws.rs.core.Response>> asyncResultHandler) {
    if (file == null) {
      asyncResultHandler.handle(
          Future.succeededFuture(
              failFunc.apply("Not found")));
    } else {
      String dataAsString = file.getData();
      byte[] decoded = Base64.getDecoder().decode(dataAsString);

      BinaryOutStream binaryOutStream = new BinaryOutStream();
      binaryOutStream.setData(decoded);
      asyncResultHandler.handle(
          Future.succeededFuture(
              succeedFunc.apply(binaryOutStream)));
    }
  }


}
