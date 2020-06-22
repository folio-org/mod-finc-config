package org.folio.finc.select;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import org.folio.finc.ApiTestBase;
import org.folio.finc.model.File;
import org.folio.rest.jaxrs.model.FilterFile;
import org.folio.rest.jaxrs.model.FincSelectFilter;
import org.folio.rest.jaxrs.model.FincSelectFilter.Type;
import org.folio.rest.jaxrs.model.Isil;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.utils.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class FilterHelperTest extends ApiTestBase {

  private static final String TEST_CONTENT = "This is the test content!!!!";
  @Rule
  public Timeout timeout = Timeout.seconds(10);
  private FilterHelper cut;
  private Isil isilUBL;
  private Isil isilDiku;
  private FincSelectFilter filter;
  private Vertx vertx;

  @Before
  public void init() {
    isilUBL = loadIsilUbl();
    isilDiku = loadIsilDiku();

    filter =
        new FincSelectFilter()
            .withLabel("Holdings 1")
            .withId(UUID.randomUUID().toString())
            .withType(Type.WHITELIST);

    vertx = Vertx.vertx();
    cut = new FilterHelper();
  }

  @After
  public void tearDown() {
    deleteIsil(isilDiku.getId());
    deleteIsil(isilUBL.getId());
  }

  @Test
  public void testDeleteFilesOfFilter(TestContext context) {
    // POST File
    Response postResponse =
        given()
            .body(TEST_CONTENT.getBytes())
            .header("X-Okapi-Tenant", TENANT_UBL)
            .header("content-type", ContentType.BINARY)
            .post(FINC_SELECT_FILES_ENDPOINT)
            .then()
            .statusCode(200)
            .extract()
            .response();
    String id = postResponse.getBody().print();
    FilterFile filterFile = new FilterFile().withFileId(id).withLabel("This is a file");

    filter.setId(UUID.randomUUID().toString());
    filter.setFilterFiles(Collections.singletonList(filterFile));

    // POST Filter
    given()
        .body(Json.encode(filter))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .post(FINC_SELECT_FILTERS_ENDPOINT)
        .then()
        .statusCode(201)
        .body("id", equalTo(filter.getId()))
        .body("label", equalTo(filter.getLabel()))
        .body("type", equalTo(filter.getType().value()));

    Async async = context.async();
    cut.deleteFilesOfFilter(filter.getId(), isilUBL.getIsil(), Vertx.vertx().getOrCreateContext())
        .onComplete(ar -> {
          if (ar.succeeded()) {
            Criteria idCrit =
                new Criteria()
                    .addField("'id'")
                    .setJSONB(true)
                    .setOperation("=")
                    .setVal(id);
            Criterion criterion = new Criterion(idCrit);
            PostgresClient.getInstance(vertx, Constants.MODULE_TENANT)
                .get(
                    "files",
                    File.class,
                    criterion,
                    true,
                    true,
                    arDB -> {
                      if (arDB.succeeded()) {
                        if (arDB.result() != null) {
                          context.assertEquals(0, arDB.result().getResults().size());
                        } else {
                          context.fail("Cannot get files.");
                        }
                        async.complete();
                      } else {
                        context.fail(ar.cause().toString());
                      }
                    });
          } else {
            context.fail("Failed deleting files. " + ar.cause());
          }
        });

    // DELETE Filter
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .delete(FINC_SELECT_FILTERS_ENDPOINT + "/" + filter.getId())
        .then()
        .statusCode(204);
  }

  @Test
  public void testRemoveFilesToDelete(TestContext context) {
    // POST File 1
    Response postResponse1 =
        given()
            .body(TEST_CONTENT.getBytes())
            .header("X-Okapi-Tenant", TENANT_UBL)
            .header("content-type", ContentType.BINARY)
            .post(FINC_SELECT_FILES_ENDPOINT)
            .then()
            .statusCode(200)
            .extract()
            .response();
    String id1 = postResponse1.getBody().print();
    FilterFile filterFile1 = new FilterFile().withFileId(id1).withLabel("This is a file");

    // POST File 2
    Response postResponse2 =
        given()
            .body(TEST_CONTENT.getBytes())
            .header("X-Okapi-Tenant", TENANT_UBL)
            .header("content-type", ContentType.BINARY)
            .post(FINC_SELECT_FILES_ENDPOINT)
            .then()
            .statusCode(200)
            .extract()
            .response();
    String id2 = postResponse2.getBody().print();
    FilterFile filterFile2 = new FilterFile().withFileId(id2).withLabel("This is a second file");

    filter.setId(UUID.randomUUID().toString());
    filter.setFilterFiles(Arrays.asList(filterFile1, filterFile2));

    // POST Filter
    given()
        .body(Json.encode(filter))
        .header("X-Okapi-Tenant", TENANT_UBL)
        .header("content-type", ContentType.JSON)
        .header("accept", ContentType.JSON)
        .post(FINC_SELECT_FILTERS_ENDPOINT)
        .then()
        .statusCode(201)
        .body("id", equalTo(filter.getId()))
        .body("label", equalTo(filter.getLabel()))
        .body("type", equalTo(filter.getType().value()));

    filterFile2.setDelete(true);
    Async async = context.async();
    cut.removeFilesToDelete(filter, isilUBL.getIsil(), Vertx.vertx().getOrCreateContext())
        .onComplete(ar -> {
          if (ar.succeeded()) {
            PostgresClient.getInstance(vertx, Constants.MODULE_TENANT)
                .get(
                    "files",
                    File.class,
                    true,
                    true,
                    arDB -> {
                      if (arDB.succeeded()) {
                        if (arDB.result() != null) {
                          context.assertEquals(1, arDB.result().getResults().size());
                        } else {
                          context.fail("Cannot get files.");
                        }
                        async.complete();
                      } else {
                        context.fail(ar.cause().toString());
                      }
                      cleanUp(id1, filter.getId());
                    });
          } else {
            context.fail("Failed deleting files.");
          }
        });
  }

  private void cleanUp(String fileId, String filterId) {
    // DELETE File 1
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .delete(FINC_SELECT_FILES_ENDPOINT + "/" + fileId)
        .then()
        .statusCode(204);

    // DELETE Filter
    given()
        .header("X-Okapi-Tenant", TENANT_UBL)
        .delete(FINC_SELECT_FILTERS_ENDPOINT + "/" + filterId)
        .then()
        .statusCode(204);
  }
}
