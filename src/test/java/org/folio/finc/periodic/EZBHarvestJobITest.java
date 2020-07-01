package org.folio.finc.periodic;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.folio.finc.dao.FileDAO;
import org.folio.finc.dao.FileDAOImpl;
import org.folio.finc.dao.SelectFileDAO;
import org.folio.finc.dao.SelectFileDAOImpl;
import org.folio.finc.dao.SelectFilterDAO;
import org.folio.finc.dao.SelectFilterDAOImpl;
import org.folio.finc.mocks.EZBServiceMock;
import org.folio.finc.model.File;
import org.folio.finc.rules.EmbeddedPostgresRule;
import org.folio.rest.jaxrs.model.Credential;
import org.folio.rest.jaxrs.model.FilterFile;
import org.folio.rest.jaxrs.model.FincSelectFilter;
import org.folio.rest.jaxrs.model.Metadata;
import org.folio.rest.persist.PostgresClient;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class EZBHarvestJobITest {

  private static Vertx vertx;
  private static Context vertxContext;
  private static final String tenant = "finc";

  @ClassRule
  public static EmbeddedPostgresRule pgRule = new EmbeddedPostgresRule(tenant);
  @Rule
  public Timeout timeout = Timeout.seconds(50);

  @BeforeClass
  public static void beforeClass() {
    vertx = Vertx.vertx();
    vertxContext = vertx.getOrCreateContext();
  }

  @Test
  public void checkThatFilterFileIsAdded(TestContext context) {
    // insert ezb credentials
    Async async = context.async(1);
    Credential credential = new Credential().withUser("user").withPassword("password")
        .withLibId("libId").withIsil(tenant);

    PostgresClient.getInstance(vertx, tenant)
        .save("ezb_credentials", credential, ar -> {
          if (ar.succeeded()) {
            EZBHarvestJob job = new EZBHarvestJob();
            job.setEZBService(new EZBServiceMock());
            job.run(vertxContext)
                .onComplete(ar2 -> {
                  if (ar2.succeeded()) {
                    getUpdatedEZBFile().onComplete(a -> {
                      if (a.succeeded()) {
                        context.assertEquals(EZBServiceMock.EZB_FILE_CONTENT, a.result());
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
    Date date = Date
        .from(LocalDate.of(1977, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
    insertEZBFile("foobar", date)
        .onSuccess(aVoid -> {
          Credential credential = new Credential().withUser("user").withPassword("password")
              .withLibId("libId").withIsil(tenant);

          PostgresClient.getInstance(vertx, tenant)
              .save("ezb_credentials", credential, ar -> {
                if (ar.succeeded()) {
                  EZBHarvestJob job = new EZBHarvestJob();
                  job.setEZBService(new EZBServiceMock());
                  job.run(vertxContext)
                      .onComplete(ar2 -> {
                        if (ar2.succeeded()) {
                          getUpdatedEZBFile().onComplete(a -> {
                            if (a.succeeded()) {
                              context.assertEquals(EZBServiceMock.EZB_FILE_CONTENT, a.result());
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
        .onFailure(throwable -> {
          context.fail(throwable);
          async.countDown();
        });
  }

  @Test
  public void checkThatFilterFileIsNotUpdated(TestContext context) {
    Async async = context.async(1);
    Date date = Date
        .from(LocalDate.of(1977, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC));

    insertEZBFile(EZBServiceMock.EZB_FILE_CONTENT, date)
        .onSuccess(aVoid -> {
          Credential credential = new Credential().withUser("user").withPassword("password")
              .withLibId("libId").withIsil(tenant);

          PostgresClient.getInstance(vertx, tenant)
              .save("ezb_credentials", credential, ar -> {
                if (ar.succeeded()) {
                  EZBHarvestJob job = new EZBHarvestJob();
                  job.setEZBService(new EZBServiceMock());
                  job.run(vertxContext)
                      .onComplete(ar2 -> {
                        if (ar2.succeeded()) {
                          getUpdatedEZBFile().onComplete(a -> {
                            if (a.succeeded()) {
                              context.assertEquals(EZBServiceMock.EZB_FILE_CONTENT, a.result());
                              getMetadataOfFilter().onComplete(mdAR -> {
                                if (mdAR.succeeded()) {
                                  context.assertEquals(date, mdAR.result().getUpdatedDate());
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
        .onFailure(throwable -> {
          context.fail(throwable);
          async.countDown();
        });
  }

  private Future<Void> insertEZBFile(String content, Date date) {
    Promise<Void> result = Promise.promise();
    SelectFileDAO fileDAO = new SelectFileDAOImpl();
    SelectFilterDAO filterDAO = new SelectFilterDAOImpl();

    String fileId = UUID.randomUUID().toString();
    File file = new File()
        .withData(Base64.getEncoder().encodeToString(content.getBytes()))
        .withIsil(EZBHarvestJobITest.tenant)
        .withId(fileId);
    fileDAO.upsert(file, fileId, vertx.getOrCreateContext())
        .onComplete(fileAR -> {
          filterDAO.getAll("label==\"EZB holdings\"", 0, 1, EZBHarvestJobITest.tenant, vertx.getOrCreateContext())
              .onComplete(filterAR -> {
                if (filterAR.succeeded()) {
                  List<FincSelectFilter> selectFilters = filterAR.result().getFincSelectFilters();
                  FincSelectFilter filter = selectFilters.get(0);
                  FilterFile ff = new FilterFile()
                      .withFileId(fileId)
                      .withFilename("EZB file")
                      .withLabel("EZB file")
                      .withId(UUID.randomUUID().toString());
                  filter.getFilterFiles().add(ff);
                  Metadata md = new Metadata()
                      .withUpdatedDate(date)
                      .withCreatedDate(date);
                  filter.setMetadata(md);
                  filterDAO.update(filter, filter.getId(), vertx.getOrCreateContext())
                      .onSuccess(fincSelectFilter -> result.complete())
                      .onFailure(result::fail);
                } else {
                  result.fail(filterAR.cause());
                }
              });
        });
    return result.future();
  }

  private Future<String> getUpdatedEZBFile() {
    Promise<String> result = Promise.promise();
    SelectFilterDAO selectFilterDAO = new SelectFilterDAOImpl();

    selectFilterDAO.getAll("label==\"EZB holdings\"", 0, 1, EZBHarvestJobITest.tenant, vertx.getOrCreateContext())
        .onComplete(filterAr -> {
          if (filterAr.succeeded()) {
            List<FincSelectFilter> filters = filterAr.result().getFincSelectFilters();
            if (filters.size() != 1) {
              result.fail(String
                  .format("Expected exactly 1 EZB holdings filter, but found %s", filters.size()));
            } else {
              List<FilterFile> filterFiles = filters.get(0).getFilterFiles();
              List<FilterFile> ezbFiles = filterFiles.stream()
                  .filter(filterFile -> filterFile.getLabel().equals("EZB file")).collect(
                      Collectors.toList());
              if (ezbFiles.size() != 1) {
                result.fail(String
                    .format("Expected exactly 1 EZB holdings file, but found %s", ezbFiles.size()));
              } else {
                String fileId = ezbFiles.get(0).getFileId();
                getFile(fileId)
                    .onComplete(fileAr -> {
                      if (fileAr.succeeded()) {
                        result.complete(fileAr.result());
                      } else {
                        result.fail(fileAr.cause());
                      }
                    });
              }
            }
          } else {
            result.fail(filterAr.cause());
          }
        });
    return result.future();
  }

  private Future<Metadata> getMetadataOfFilter() {
    Promise<Metadata> result = Promise.promise();
    SelectFilterDAO selectFilterDAO = new SelectFilterDAOImpl();

    selectFilterDAO.getAll("label==\"EZB holdings\"", 0, 1, EZBHarvestJobITest.tenant, vertx.getOrCreateContext())
        .onComplete(filterAr -> {
          if (filterAr.succeeded()) {
            List<FincSelectFilter> filters = filterAr.result().getFincSelectFilters();
            if (filters.size() != 1) {
              result.fail(String
                  .format("Expected exactly 1 EZB holdings filter, but found %s", filters.size()));
            } else {
              result.complete(filters.get(0).getMetadata());
            }
          } else {
            result.fail(filterAr.cause());
          }
        });
    return result.future();
  }

  private Future<String> getFile(String fileId) {
    Promise<String> result = Promise.promise();
    FileDAO fileDAO = new FileDAOImpl();
    fileDAO.getById(fileId, vertx.getOrCreateContext())
        .onComplete(ar -> {
          if (ar.succeeded()) {
            String actualAsBase64 = ar.result().getData();
            byte[] bytes = Base64.getDecoder().decode(actualAsBase64);
            String s = new String(bytes, StandardCharsets.UTF_8);
            result.complete(s);
          } else {
            result.fail(ar.cause());
          }
        });
    return result.future();
  }

}
