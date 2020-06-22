package org.folio.finc.periodic;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class EZBHarvestVerticle extends AbstractVerticle {

  private static final Logger log = LoggerFactory.getLogger(EZBHarvestVerticle.class);

  @Override
  public void start() {
    String user = config().getString("user");
    String password = config().getString("password");
    String libId = config().getString("libId");
    String isil = config().getString("isil");

    System.out.println(String.format("Yeah! Done: %s, %s, %s, %s", user, password, libId, isil));
  }

  private void fetchFileFromEZB() {


  }

}
