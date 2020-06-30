package org.folio.finc.periodic.ezb;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

public interface EZBService {

  Future<String> fetchEZBFile(String user, String password, String libId, Vertx vertx);

}
