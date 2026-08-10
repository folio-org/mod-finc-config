package org.folio.finc.periodic;

import static io.vertx.core.Future.succeededFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.finc.mocks.EZBServiceMock.EZB_FILE_CONTENT;

import io.vertx.core.Future;
import io.vertx.ext.unit.TestContext;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import org.folio.finc.dao.EZBCredentialsDAOImpl;
import org.folio.finc.mocks.EZBServiceMock;
import org.folio.rest.jaxrs.model.FincSelectFilter;
import org.folio.rest.persist.PostgresClient;
import org.junit.Test;

public class EZBHarvestServiceIT extends AbstractEZBHarvestTest {

  private static final EZBHarvestService EZB_HARVEST_SERVICE =
      new EZBHarvestService(new EZBServiceMock());
  private static final Date DATE =
      Date.from(LocalDate.of(1977, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
  private static final String FILE_ID = "f81e18f1-4427-4eb7-a936-2039ca1c5023";
  private static final String FILE_ID2 = "2dbc4564-d311-4288-b88a-0827d6f6c8f1";
  private static final String EXISTING_CONTENT = "existing ezb file content";
  private static final String OTHER_TENANT = "ubl";
  private static final String OTHER_ISIL = "DE-14";

  @Test
  public void checkThatEZBFileIsAdded(TestContext context) {
    EZB_HARVEST_SERVICE
        .harvest(tenant, vertxContext)
        .flatMap(v -> getUpdatedEZBFile())
        .onComplete(context.asyncAssertSuccess(s -> assertThat(s).isEqualTo(EZB_FILE_CONTENT)));
  }

  @Test
  public void checkThatEZBFileIsUpdated(TestContext context) {
    insertEZBFile("foobar", FILE_ID, DATE)
        .flatMap(v -> EZB_HARVEST_SERVICE.harvest(tenant, vertxContext))
        .flatMap(v -> getUpdatedEZBFile())
        .onComplete(context.asyncAssertSuccess(s -> assertThat(s).isEqualTo(EZB_FILE_CONTENT)));
  }

  @Test
  public void checkThatEZBFileIsUpdatedForMultiFiles(TestContext context) {
    Future<FincSelectFilter> firstInsert = insertEZBFile("foo", FILE_ID, DATE);
    Future<FincSelectFilter> secondInsert = insertEZBFile("bar", FILE_ID2, DATE);
    Future.all(Arrays.asList(firstInsert, secondInsert))
        .flatMap(cf -> EZB_HARVEST_SERVICE.harvest(tenant, vertxContext))
        .flatMap(v -> getUpdatedEZBFile())
        .onComplete(context.asyncAssertSuccess(s -> assertThat(s).isEqualTo(EZB_FILE_CONTENT)));
  }

  @Test
  public void checkThatEZBFileIsNotUpdated(TestContext context) {
    insertEZBFile(EZB_FILE_CONTENT, FILE_ID, DATE)
        .flatMap(v -> EZB_HARVEST_SERVICE.harvest(tenant, vertxContext))
        .flatMap(v -> getUpdatedEZBFile())
        .flatMap(
            s -> {
              assertThat(s).isEqualTo(EZB_FILE_CONTENT);
              return succeededFuture();
            })
        .flatMap(v -> getMetadataOfFilter())
        .onComplete(
            context.asyncAssertSuccess(
                metadata -> assertThat(metadata.getUpdatedDate()).isEqualTo(DATE)));
  }

  @Test
  public void checkThatNothingIsHarvestedWithoutCredentials(TestContext context) {
    insertEZBFile(EXISTING_CONTENT, FILE_ID, DATE)
        .flatMap(v -> new EZBCredentialsDAOImpl().deleteByIsil(ISIL, vertxContext))
        .flatMap(n -> EZB_HARVEST_SERVICE.harvest(tenant, vertxContext))
        .flatMap(v -> getUpdatedEZBFile())
        .onComplete(context.asyncAssertSuccess(s -> assertThat(s).isEqualTo(EXISTING_CONTENT)));
  }

  @Test
  public void checkThatOnlyTheHarvestedTenantsFilterIsUpdated(TestContext context) {
    insertIsil(OTHER_ISIL, OTHER_TENANT)
        .flatMap(id -> insertFilter(OTHER_ISIL))
        .flatMap(id -> insertCredential(OTHER_ISIL))
        .flatMap(id -> EZB_HARVEST_SERVICE.harvest(tenant, vertxContext))
        .flatMap(v -> getUpdatedEZBFile())
        .flatMap(
            s -> {
              assertThat(s).isEqualTo(EZB_FILE_CONTENT);
              return getEZBFilter(OTHER_ISIL);
            })
        .onComplete(
            context.asyncAssertSuccess(
                filters ->
                    assertThat(filters.getFincSelectFilters().get(0).getFilterFiles()).isEmpty()));
  }

  @Test
  public void checkThatEZBFileIsNotAddedWithoutFilter(TestContext context) {
    PostgresClient.getInstance(vertx, tenant)
        .delete("filters", filterId)
        .flatMap(rs -> EZB_HARVEST_SERVICE.harvest(tenant, vertxContext))
        .flatMap(v -> getEZBFilter())
        .onComplete(context.asyncAssertSuccess(fsf -> assertThat(fsf.getTotalRecords()).isZero()));
  }
}
