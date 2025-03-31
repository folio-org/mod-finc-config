package org.folio.finc.dao;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.Future;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class IsilDAOTest {

  static final String TENANT = "test-tenant";

  @Test
  void testWithTenantIsilForTenantSucceedsWithNonEmptyOptional() {
    IsilDAO isilDAO = (tenantId, context) -> Future.succeededFuture(Optional.of("abc-123"));
    assertThat(isilDAO.withIsilForTenant(TENANT, null).result()).isEqualTo("abc-123");
  }

  @Test
  void testWithTenantIsilForTenantSucceedsWithEmptyOptional() {
    IsilDAO isilDAO = (tenantId, context) -> Future.succeededFuture(Optional.empty());
    assertThat(isilDAO.withIsilForTenant(TENANT, null).cause())
        .hasMessage("ISIL not found for tenant: " + TENANT);
  }

  @Test
  void testWithTenantIsilForTenantFails() {
    IsilDAO isilDAO = (tenantId, context) -> Future.failedFuture("error");
    assertThat(isilDAO.withIsilForTenant(TENANT, null).cause()).hasMessage("error");
  }
}
