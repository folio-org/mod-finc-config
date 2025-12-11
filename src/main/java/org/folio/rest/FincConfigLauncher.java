package org.folio.rest;

import io.vertx.core.VertxOptions;
import org.folio.rest.utils.JacksonConfigUtil;

public class FincConfigLauncher extends RestLauncher {

  static {
    JacksonConfigUtil.configureJacksonConstraints();
  }

  public static void main(String[] args) {
    new FincConfigLauncher().dispatch(args);
  }

  @Override
  public void beforeStartingVertx(VertxOptions options) {
    super.beforeStartingVertx(options);
  }
}
