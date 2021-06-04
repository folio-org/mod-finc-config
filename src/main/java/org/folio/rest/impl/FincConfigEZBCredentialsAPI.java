package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import org.folio.finc.dao.EZBCredentialsDAO;
import org.folio.finc.dao.EZBCredentialsDAOImpl;
import org.folio.finc.dao.EZBCredentialsDAOImpl.EZBCredentialsException;
import org.folio.rest.RestVerticle;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.Credential;
import org.folio.rest.jaxrs.model.Credentials;
import org.folio.rest.jaxrs.resource.FincConfigEzbCredentials;
import org.folio.rest.utils.Constants;

/**
 * Manages EZB credentials for ui-finc-config
 */
public class FincConfigEZBCredentialsAPI implements FincConfigEzbCredentials {

  private final EZBCredentialsDAO ezbCredentialsDAO;

  public FincConfigEZBCredentialsAPI() {
    this.ezbCredentialsDAO = new EZBCredentialsDAOImpl();
  }

  @Override
  @Validate
  public void getFincConfigEzbCredentials(String query, int offset, int limit, String lang,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    ezbCredentialsDAO.getAll(query, offset, limit, vertxContext)
        .onComplete(ar -> {
          if (ar.succeeded()) {
            Credentials creds = ar.result();
            List<Credential> credentialsWithouId = creds.getCredentials().stream()
                .map(c -> c.withId(null))
                .collect(Collectors.toList());
            creds.setCredentials(credentialsWithouId);
            asyncResultHandler.handle(Future.succeededFuture(
                GetFincConfigEzbCredentialsResponse.respond200WithApplicationJson(creds)));
          } else {
            asyncResultHandler.handle(Future.succeededFuture(
                GetFincConfigEzbCredentialsResponse.respond500WithTextPlain(ar.cause())));
          }
        });
  }

  @Override
  @Validate
  public void postFincConfigEzbCredentials(String lang, Credential entity,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    okapiHeaders.put(RestVerticle.OKAPI_HEADER_TENANT, Constants.MODULE_TENANT);

    ezbCredentialsDAO.insert(entity, vertxContext)
        .onComplete(ar -> {
          if (ar.succeeded()) {
            asyncResultHandler.handle(Future.succeededFuture(PostFincConfigEzbCredentialsResponse
                .respond201WithApplicationJson(entity,
                    PostFincConfigEzbCredentialsResponse.headersFor201())));
          } else {
            if (ar.cause() instanceof EZBCredentialsException) {
              asyncResultHandler.handle(Future.succeededFuture(
                  PostFincConfigEzbCredentialsResponse
                      .respond400WithTextPlain(ar.cause().getLocalizedMessage())));
            } else {
              asyncResultHandler.handle(Future.succeededFuture(
                  PostFincConfigEzbCredentialsResponse.respond500WithTextPlain(ar.cause())));
            }
          }
        });
  }

  @Override
  @Validate
  public void getFincConfigEzbCredentialsByIsil(String isil, String lang,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {

    ezbCredentialsDAO.getByIsil(isil, vertxContext)
        .onComplete(ar -> {
          if (ar.succeeded()) {
            Credential cred = ar.result();
            if (cred == null) {
              asyncResultHandler.handle(Future.succeededFuture(
                  GetFincConfigEzbCredentialsByIsilResponse
                      .respond404WithTextPlain("Not found.")));
            } else {
              asyncResultHandler.handle(Future.succeededFuture(
                  GetFincConfigEzbCredentialsByIsilResponse
                      .respond200WithApplicationJson(cred.withId(null))));
            }
          } else {
            asyncResultHandler.handle(Future.succeededFuture(
                GetFincConfigEzbCredentialsByIsilResponse.respond500WithTextPlain(ar.cause())));
          }
        });
  }

  @Override
  @Validate
  public void deleteFincConfigEzbCredentialsByIsil(String isil, String lang,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {

    ezbCredentialsDAO.deleteByIsil(isil, vertxContext)
        .onComplete(ar -> {
          if (ar.succeeded()) {
            asyncResultHandler.handle(Future.succeededFuture(
                DeleteFincConfigEzbCredentialsByIsilResponse.respond204()));
          } else {
            asyncResultHandler.handle(Future.succeededFuture(
                DeleteFincConfigEzbCredentialsByIsilResponse.respond500WithTextPlain(ar.cause())));
          }
        });
  }

  @Override
  @Validate
  public void putFincConfigEzbCredentialsByIsil(String isil, String lang, Credential entity,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {

    if (!isil.equals(entity.getIsil())) {
      asyncResultHandler.handle(Future.succeededFuture(
          PutFincConfigEzbCredentialsByIsilResponse.respond400WithTextPlain("Isils not equal.")));
    } else {

      ezbCredentialsDAO.getByIsil(isil, vertxContext)
          .compose(credential -> {
            if (credential == null) {
              return Future.failedFuture("");
            } else {
              return ezbCredentialsDAO.deleteByIsil(isil, vertxContext)
                  .compose(
                      integer -> ezbCredentialsDAO.insert(entity, vertxContext)
                  );
            }
          }).onComplete(ar -> {
        if (ar.succeeded()) {
          asyncResultHandler
              .handle(
                  Future.succeededFuture(PutFincConfigEzbCredentialsByIsilResponse.respond204()));
        } else {
          asyncResultHandler.handle(Future.succeededFuture(
              PutFincConfigEzbCredentialsByIsilResponse.respond500WithTextPlain(ar.cause())));
        }
      });
    }
  }
}
