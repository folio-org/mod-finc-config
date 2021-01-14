package org.folio.finc.rules;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.impl.TenantAPI;
import org.folio.rest.jaxrs.model.FincSelectFilter;
import org.folio.rest.jaxrs.model.FincSelectFilter.Type;
import org.folio.rest.jaxrs.model.Isil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.VertxUtils;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EmbeddedPostgresRule implements TestRule {

  private static final Logger log = LogManager.getLogger(EmbeddedPostgresRule.class);
  Vertx vertx = VertxUtils.getVertxFromContextOrNew();
  String tenant;

  public EmbeddedPostgresRule(String tenant) {
    this.tenant = tenant;
  }

 // public EmbeddedPostgresRule() {}

  private Future<List<String>> createSchema(String tenant) {
    log.info("Creating schema for tenant: {}", tenant);
    Promise<List<String>> createSchema = Promise.promise();
    try {
      String[] sqlFile = new TenantAPI().sqlFile(tenant, false, null, null);
      PostgresClient.getInstance(vertx)
          .runSQLFile(
              String.join("\n", sqlFile),
              true,
              ar -> {
                if (ar.succeeded()) {
                  if (ar.result().size() == 0) {
                    createSchema.complete(ar.result());
                  } else {
                    createSchema.fail(tenant + ": " + ar.result().get(0));
                  }
                } else {
                  createSchema.fail(ar.cause());
                }
              });
    } catch (Exception e) {
      createSchema.fail(e);
    }
    return createSchema.future();
  }

  private Future<Void> insertIsil(String tenant) {
    log.info("Creating isil for tenant: {}", tenant);
    Promise<Void> result = Promise.promise();
    Isil isil =
        new Isil()
            .withId(UUID.randomUUID().toString())
            .withIsil(tenant)
            .withTenant(tenant)
            .withLibrary(tenant);
    PostgresClient.getInstance(vertx, tenant)
        .save(
            "isils",
            isil,
            ar -> {
              if (ar.succeeded()) {
                result.complete();
              } else {
                result.fail(ar.cause());
              }
            });
    return result.future();
  }

  private Future<Void> insertFilter(String tenant) {
    log.info("Creating filter for tenant: {}", tenant);
    Promise<Void> result = Promise.promise();
    FincSelectFilter filter =
        new FincSelectFilter()
            .withId(UUID.randomUUID().toString())
            .withLabel("EZB holdings")
            .withType(Type.WHITELIST)
            .withIsil(tenant);
    PostgresClient.getInstance(vertx, tenant)
        .save(
            "filters",
            filter,
            ar -> {
              if (ar.succeeded()) {
                result.complete();
              } else {
                result.fail(ar.cause());
              }
            });
    return result.future();
  }

  @Override
  public Statement apply(Statement base, Description description) {
    try {
      PostgresClient.getInstance(vertx).startEmbeddedPostgres();

      CompletableFuture<List<String>> future = new CompletableFuture<>();

      createSchema(tenant)
          .onComplete(
              ar -> {
                if (ar.succeeded()) {
                  future.complete(ar.result());
                } else {
                  future.completeExceptionally(ar.cause());
                }
              });
      future.get();
    } catch (Exception e) {
      return new Statement() {
        @Override
        public void evaluate() throws Throwable {
          throw e;
        }
      };
    }

    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          base.evaluate();
        } finally {
          PostgresClient.stopEmbeddedPostgres();
        }
      }
    };
  }
}
