package org.folio.rest.utils;

import io.vertx.core.json.JsonObject;

import java.util.Map;

public enum ModuleEnvs {
  MOD_USERNAME;

  private static final String PORT = "port";
  private static final String TIMEOUT = "queryTimeout";
  private static final String MAXPOOL = "maxPoolSize";
  private static Map<String, String> env = System.getenv();

  private ModuleEnvs() {}

  static void setEnv(Map<String, String> env) {
    env = env;
  }

  public static String getEnv(ModuleEnvs key) {
    return (String) env.get(key.name());
  }

  public static JsonObject allMODConfs() {
    JsonObject obj = new JsonObject();
    env.forEach(
        (key, value) -> {
          if (key.startsWith("mod.") || key.startsWith("MOD_")) {
            key = key.substring(4).toLowerCase();
            obj.put(key, value);
          }
        });
    return obj;
  }
}
