package org.folio.finc.periodic;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.finc.dao.SelectFileDAO;
import org.folio.finc.dao.SelectFileDAOImpl;
import org.folio.finc.dao.SelectFilterDAO;
import org.folio.finc.dao.SelectFilterDAOImpl;
import org.folio.finc.mocks.EZBServiceMock;
import org.folio.finc.model.File;
import org.folio.okapi.common.GenericCompositeFuture;
import org.folio.rest.jaxrs.model.Credential;
import org.folio.rest.jaxrs.model.FilterFile;
import org.folio.rest.jaxrs.model.FincSelectFilter;
import org.folio.rest.jaxrs.model.FincSelectFilter.Type;
import org.folio.rest.jaxrs.model.Metadata;
import org.folio.rest.persist.PostgresClient;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@RunWith(VertxUnitRunner.class)
public class EZBHarvestJobWithFilterITest extends AbstractEZBHarvestJobTest {

  private static final String filterId = UUID.randomUUID().toString();

  @BeforeClass
  public static void beforeClass() {
    vertx = Vertx.vertx();
    vertxContext = vertx.getOrCreateContext();
  }

  @Before
  public void setUp(TestContext context) {
    vertx = Vertx.vertx();
    vertxContext = vertx.getOrCreateContext();
    Async async = context.async();
    try {
      PostgresClient.getInstance(vertx).startEmbeddedPostgres();

      createSchema(tenant)
          .compose(s -> insertIsil(tenant))
          .compose(aVoid -> insertFilter(tenant))
          .onComplete(
              ar -> {
                if (ar.succeeded()) {
                  async.complete();
                } else {
                  context.fail(ar.cause());
                }
              });
    } catch (Exception e) {
      context.fail(e);
    }
  }

  @After
  public void cleanUp(TestContext context) {
    PostgresClient.stopEmbeddedPostgres();
  }

  @Test
  public void checkThatFilterFileIsAdded(TestContext context) {
    // insert ezb credentials
    Async async = context.async(1);
    Credential credential =
        new Credential()
            .withUser("user")
            .withPassword("password")
            .withLibId("libId")
            .withIsil(tenant);

    PostgresClient.getInstance(vertx, tenant)
        .save(
            "ezb_credentials",
            credential,
            ar -> {
              if (ar.succeeded()) {
                EZBHarvestJob job = new EZBHarvestJob();
                job.setEZBService(new EZBServiceMock());
                job.run(vertxContext)
                    .onComplete(
                        ar2 -> {
                          if (ar2.succeeded()) {
                            getUpdatedEZBFile()
                                .onComplete(
                                    a -> {
                                      if (a.succeeded()) {
                                        context.assertEquals(
                                            EZBServiceMock.EZB_FILE_CONTENT, a.result());
                                      } else {
                                        context.fail(a.cause());
                                      }
                                      async.countDown();
                                    });
                          } else {
                            context.fail();
                            async.countDown();
                          }
                        });
              } else {
                context.fail();
                async.countDown();
              }
            });
  }

  @Test
  public void checkThatFilterFileIsUpdated(TestContext context) {
    Async async = context.async(1);
    String fileId = UUID.randomUUID().toString();
    Date date = Date.from(LocalDate.of(1977, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
    insertEZBFile("foobar", fileId, date)
        .onSuccess(
            aVoid -> {
              Credential credential =
                  new Credential()
                      .withUser("user")
                      .withPassword("password")
                      .withLibId("libId")
                      .withIsil(tenant);

              PostgresClient.getInstance(vertx, tenant)
                  .save(
                      "ezb_credentials",
                      credential,
                      ar -> {
                        if (ar.succeeded()) {
                          EZBHarvestJob job = new EZBHarvestJob();
                          job.setEZBService(new EZBServiceMock());
                          job.run(vertxContext)
                              .onComplete(
                                  ar2 -> {
                                    if (ar2.succeeded()) {
                                      getUpdatedEZBFile()
                                          .onComplete(
                                              a -> {
                                                if (a.succeeded()) {
                                                  context.assertEquals(
                                                      EZBServiceMock.EZB_FILE_CONTENT, a.result());
                                                } else {
                                                  context.fail(a.cause());
                                                }
                                                async.countDown();
                                              });
                                    } else {
                                      context.fail();
                                      async.countDown();
                                    }
                                  });
                        } else {
                          context.fail();
                          async.countDown();
                        }
                      });
            })
        .onFailure(
            throwable -> {
              context.fail(throwable);
              async.countDown();
            });
  }

  @Test
  public void checkThatFilterFileIsUpdatedForMultiFiles(TestContext context) {
    Async async = context.async(1);
    String firstFileId = UUID.randomUUID().toString();
    String secondFileId = UUID.randomUUID().toString();
    Date date = Date.from(LocalDate.of(1977, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC));

    Future<Void> firstInsert = insertEZBFile("foo", firstFileId, date);
    Future<Void> secondInsert = insertEZBFile("bar", secondFileId, date);
    GenericCompositeFuture.all(Arrays.asList(firstInsert, secondInsert))
        .onSuccess(
            aVoid -> {
              Credential credential =
                  new Credential()
                      .withUser("user")
                      .withPassword("password")
                      .withLibId("libId")
                      .withIsil(tenant);

              PostgresClient.getInstance(vertx, tenant)
                  .save(
                      "ezb_credentials",
                      credential,
                      ar -> {
                        if (ar.succeeded()) {
                          EZBHarvestJob job = new EZBHarvestJob();
                          job.setEZBService(new EZBServiceMock());
                          job.run(vertxContext)
                              .onComplete(
                                  ar2 -> {
                                    if (ar2.succeeded()) {
                                      getUpdatedEZBFile()
                                          .onComplete(
                                              a -> {
                                                if (a.succeeded()) {
                                                  context.assertEquals(
                                                      EZBServiceMock.EZB_FILE_CONTENT, a.result());
                                                } else {
                                                  context.fail(a.cause());
                                                }
                                                async.countDown();
                                              });
                                    } else {
                                      context.fail();
                                      async.countDown();
                                    }
                                  });
                        } else {
                          context.fail();
                          async.countDown();
                        }
                      });
            })
        .onFailure(
            throwable -> {
              context.fail(throwable);
              async.countDown();
            });
  }

  @Test
  public void checkThatFilterFileIsNotUpdated(TestContext context) {
    Async async = context.async(1);
    Date date = Date.from(LocalDate.of(1977, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
    String fileId = UUID.randomUUID().toString();
    insertEZBFile(EZBServiceMock.EZB_FILE_CONTENT, fileId, date)
        .onSuccess(
            aVoid -> {
              Credential credential =
                  new Credential()
                      .withUser("user")
                      .withPassword("password")
                      .withLibId("libId")
                      .withIsil(tenant);

              PostgresClient.getInstance(vertx, tenant)
                  .save(
                      "ezb_credentials",
                      credential,
                      ar -> {
                        if (ar.succeeded()) {
                          EZBHarvestJob job = new EZBHarvestJob();
                          job.setEZBService(new EZBServiceMock());
                          job.run(vertxContext)
                              .onComplete(
                                  ar2 -> {
                                    if (ar2.succeeded()) {
                                      getUpdatedEZBFile()
                                          .onComplete(
                                              a -> {
                                                if (a.succeeded()) {
                                                  context.assertEquals(
                                                      EZBServiceMock.EZB_FILE_CONTENT, a.result());
                                                  getMetadataOfFilter()
                                                      .onComplete(
                                                          mdAR -> {
                                                            if (mdAR.succeeded()) {
                                                              context.assertEquals(
                                                                  date,
                                                                  mdAR.result().getUpdatedDate());
                                                            } else {
                                                              context.fail(mdAR.cause());
                                                            }
                                                            async.countDown();
                                                          });
                                                } else {
                                                  context.fail(a.cause());
                                                  async.countDown();
                                                }
                                              });
                                    } else {
                                      context.fail();
                                      async.countDown();
                                    }
                                  });
                        } else {
                          context.fail();
                          async.countDown();
                        }
                      });
            })
        .onFailure(
            throwable -> {
              context.fail(throwable);
              async.countDown();
            });
  }

  private Future<Void> insertEZBFile(String content, String fileId, Date date) {
    Promise<Void> result = Promise.promise();
    SelectFileDAO fileDAO = new SelectFileDAOImpl();
    SelectFilterDAO filterDAO = new SelectFilterDAOImpl();

    File file =
        new File()
            .withData(Base64.getEncoder().encodeToString(content.getBytes()))
            .withIsil(EZBHarvestJobWithFilterITest.tenant)
            .withId(fileId);
    fileDAO
        .upsert(file, fileId, vertx.getOrCreateContext())
        .onComplete(
            fileAR ->
                filterDAO
                    .getAll(
                        "label==\"EZB holdings\"",
                        0,
                        1,
                        EZBHarvestJobWithFilterITest.tenant,
                        vertx.getOrCreateContext())
                    .onComplete(
                        filterAR -> {
                          if (filterAR.succeeded()) {
                            List<FincSelectFilter> selectFilters =
                                filterAR.result().getFincSelectFilters();
                            FincSelectFilter filter = selectFilters.get(0);
                            FilterFile ff =
                                new FilterFile()
                                    .withFileId(fileId)
                                    .withFilename("EZB file")
                                    .withLabel("EZB file")
                                    .withId(UUID.randomUUID().toString());
                            filter.getFilterFiles().add(ff);
                            Metadata md =
                                new Metadata().withUpdatedDate(date).withCreatedDate(date);
                            filter.setMetadata(md);
                            filterDAO
                                .update(filter, filter.getId(), vertx.getOrCreateContext())
                                .onSuccess(fincSelectFilter -> result.complete())
                                .onFailure(result::fail);
                          } else {
                            result.fail(filterAR.cause());
                          }
                        }));
    return result.future();
  }

  private Future<Metadata> getMetadataOfFilter() {
    Promise<Metadata> result = Promise.promise();
    SelectFilterDAO selectFilterDAO = new SelectFilterDAOImpl();

    selectFilterDAO
        .getAll(
            "label==\"EZB holdings\"",
            0,
            1,
            EZBHarvestJobWithFilterITest.tenant,
            vertx.getOrCreateContext())
        .onComplete(
            filterAr -> {
              if (filterAr.succeeded()) {
                List<FincSelectFilter> filters = filterAr.result().getFincSelectFilters();
                if (filters.size() != 1) {
                  result.fail(
                      String.format(
                          "Expected exactly 1 EZB holdings filter, but found %s", filters.size()));
                } else {
                  result.complete(filters.get(0).getMetadata());
                }
              } else {
                result.fail(filterAr.cause());
              }
            });
    return result.future();
  }

  private static Future<Void> insertFilter(String tenant) {
    Promise<Void> result = Promise.promise();
    FincSelectFilter filter =
        new FincSelectFilter()
            .withId(filterId)
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
}
