package org.folio.finc.config;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.okapi.common.XOkapiHeaders.TENANT;
import static org.folio.rest.jaxrs.model.FincSelectFilter.Type.BLACKLIST;
import static org.folio.rest.jaxrs.model.FincSelectFilter.Type.WHITELIST;
import static org.folio.rest.utils.Constants.MODULE_TENANT;

import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.ObjectUtils;
import org.assertj.core.api.ThrowingConsumer;
import org.folio.finc.ApiTestBase;
import org.folio.rest.jaxrs.model.FilterFile;
import org.folio.rest.jaxrs.model.FilteredBy;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollectionWithFilters;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollectionWithFiltersCollection;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollections;
import org.folio.rest.jaxrs.model.FincSelectFilter;
import org.folio.rest.jaxrs.model.FincSelectFilterToCollections;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class ConfigMetadataCollectionsWithFiltersIT extends ApiTestBase {

  static final String FILTERS_COLLECTIONS_ENDPOINT =
      FINC_CONFIG_FILTERS_ENDPOINT + "/%s/collections";
  static final String INCLUDE_FILTERED_BY_FALSE = "?includeFilteredBy=false";
  private static final String INCLUDE_FILTERED_BY_TRUE = "?includeFilteredBy=true";
  private static final String COLLECTION1_ID = "cae1d25d-c92e-43a3-90b9-28fb0149b907";
  private static final String COLLECTION2_ID = "68aebb7f-6325-44e8-bcdd-f528fff6dca5";

  @BeforeAll
  static void beforeAll() {
    loadTestSamples();
  }

  private static void loadTestSamples() {
    // Setup ISILs
    loadIsilUbl();
    loadIsilDiku();

    // Create a Metadata Collection
    postMetadataCollection(COLLECTION1_ID, "123");
    postMetadataCollection(COLLECTION2_ID, "234");

    // Create Filters for tenant UBL
    postFilterWithFilesAndCollections(
        "Filter 1 UBL",
        WHITELIST,
        List.of(
            createFilterFile("ubl_file_content1", "ubl_file1.txt", TENANT_UBL),
            createFilterFile("ubl_file_content2", "ubl_file2.txt", TENANT_UBL)),
        List.of(COLLECTION1_ID),
        TENANT_UBL);

    postFilterWithFilesAndCollections(
        "Filter 2 UBL", WHITELIST, null, List.of(COLLECTION1_ID), TENANT_UBL);

    // Create Filters for tenant Diku
    postFilterWithFilesAndCollections(
        "Filter Diku",
        BLACKLIST,
        List.of(createFilterFile("diku_file_content1", "diku_file1.txt", TENANT_DIKU)),
        List.of(COLLECTION1_ID),
        TENANT_DIKU);
  }

  private static void postFilterWithFilesAndCollections(
      String filterName,
      FincSelectFilter.Type filterType,
      List<FilterFile> filterFiles,
      List<String> collections,
      String tenantId) {
    // Create Filter
    FincSelectFilter filter =
        new FincSelectFilter()
            .withLabel(filterName)
            .withId(UUID.randomUUID().toString())
            .withType(filterType)
            .withFilterFiles(filterFiles);
    String filterId = post(FINC_SELECT_FILTERS_ENDPOINT, filter, tenantId);

    // Connect Filter to Collections
    FincSelectFilterToCollections filter2collections =
        new FincSelectFilterToCollections().withId(filterId).withCollectionIds(collections);
    put(FILTERS_COLLECTIONS_ENDPOINT.formatted(filterId), filter2collections, tenantId);
  }

  private static void postMetadataCollection(String id, String nameSuffix) {
    FincConfigMetadataCollection metadataCollection =
        new FincConfigMetadataCollection()
            .withId(id)
            .withCollectionId("collection-" + nameSuffix)
            .withLabel("Metadata Collection Test " + nameSuffix)
            .withUsageRestricted(FincConfigMetadataCollection.UsageRestricted.NO)
            .withSolrMegaCollections(List.of("Solr Mega Collection Test"));
    post(FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT, metadataCollection, MODULE_TENANT);
  }

  private static FilterFile createFilterFile(String content, String filename, String tenantId) {
    String ublFile1Id = postFile(content, tenantId);
    return new FilterFile()
        .withId(UUID.randomUUID().toString())
        .withLabel(filename)
        .withFilename(filename)
        .withFileId(ublFile1Id)
        .withCriteria(filename + "-criteria");
  }

  private void assertFilteredBy(List<? extends FilteredBy> filteredBy) {
    assertThat(filteredBy).hasSize(3);
    assertThat(filteredBy)
        .extracting(FilteredBy::getIsil)
        .containsExactlyInAnyOrder("DE-15", "DE-15", "DIKU-01");
    assertThat(filteredBy)
        .flatExtracting(FilteredBy::getFilterFiles)
        .extracting(FilterFile::getId)
        .hasSize(3)
        .allMatch(ObjectUtils::isNotEmpty);
  }

  @Test
  void testGetCollectionsWithFilters() {
    assertThat(
            given()
                .header(TENANT, MODULE_TENANT)
                .get(FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT + INCLUDE_FILTERED_BY_TRUE)
                .then()
                .statusCode(200)
                .extract()
                .as(FincConfigMetadataCollectionWithFiltersCollection.class)
                .getFincConfigMetadataCollections()
                .stream()
                .map(FincConfigMetadataCollectionWithFilters::getFilteredBy)
                .toList())
        .hasSize(2)
        .anyMatch(List::isEmpty)
        .anySatisfy(this::assertFilteredBy);
  }

  @ParameterizedTest
  @CsvSource({COLLECTION1_ID + ", true", COLLECTION2_ID + ", false"})
  void testGetCollectionByIdWithFilters(String collectionId, boolean shouldContainElements) {
    ThrowingConsumer<List<? extends FilteredBy>> assertFilteredBy =
        shouldContainElements ? this::assertFilteredBy : List::isEmpty;
    assertThat(
            given()
                .header(TENANT, MODULE_TENANT)
                .get(
                    FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT
                        + "/"
                        + collectionId
                        + INCLUDE_FILTERED_BY_TRUE)
                .then()
                .statusCode(200)
                .extract()
                .as(FincConfigMetadataCollectionWithFilters.class)
                .getFilteredBy())
        .satisfies(assertFilteredBy);
  }

  @ParameterizedTest
  @ValueSource(strings = {INCLUDE_FILTERED_BY_FALSE, ""})
  void testGetCollectionsWithoutFilters(String queryStr) {
    // fails if the JSON response contains a "filteredBy" attribute due to
    // additionalAttributes=false
    assertThat(
            given()
                .header(TENANT, MODULE_TENANT)
                .get(FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT + queryStr)
                .then()
                .statusCode(200)
                .extract()
                .as(FincConfigMetadataCollections.class)
                .getFincConfigMetadataCollections())
        .hasSize(2);
  }

  @ParameterizedTest
  @ValueSource(strings = {INCLUDE_FILTERED_BY_FALSE, ""})
  void testGetCollectionByIdWithoutFilters(String queryStr) {
    // fails if the JSON response contains a "filteredBy" attribute due to
    // additionalAttributes=false
    assertThat(
            given()
                .header(TENANT, MODULE_TENANT)
                .get(FINC_CONFIG_METADATA_COLLECTIONS_ENDPOINT + "/" + COLLECTION1_ID + queryStr)
                .then()
                .statusCode(200)
                .extract()
                .as(FincConfigMetadataCollection.class)
                .getId())
        .isEqualTo(COLLECTION1_ID);
  }
}
