package org.folio.finc.dao;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;

import io.vertx.core.Context;
import io.vertx.core.Future;

public interface IsilDAO {

  Future<Optional<String>> getIsilForTenant(String tenantId, Context context);

  default Future<String> withIsilForTenant(String tenantId, Context context) {
    return getIsilForTenant(tenantId, context)
        .map(
            optional ->
                optional.orElseThrow(
                    () -> new NoSuchElementException("ISIL not found for tenant: " + tenantId)));
  }
}
