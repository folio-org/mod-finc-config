package org.folio.finc.mocks;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.folio.finc.periodic.ezb.EZBService;

public class EZBServiceMock implements EZBService {

  public static final String EZB_FILE_CONTENT = "Successfully fetched ezb file.";

  @Override
  public Future<String> fetchEZBFile(String user, String password, String libId, Vertx vertx) {
    return Future.succeededFuture(EZB_FILE_CONTENT);
  }
}
