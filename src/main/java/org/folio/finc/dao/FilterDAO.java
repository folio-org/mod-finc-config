package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.FincSelectFilter;
import org.folio.rest.jaxrs.model.FincSelectFilters;

public interface FilterDAO {

  Future<FincSelectFilters> getAll(
      String query, int offset, int limit, String isil, Context vertxContext);

  Future<FincSelectFilter> getById(String id, String isil, Context vertxContext);

  Future<FincSelectFilter> insert(FincSelectFilter entity, Context vertxContext);

  Future<FincSelectFilter> update(FincSelectFilter entity, String id, Context vertxContext);

  Future<Integer> deleteById(String id, String isil, Context vertxContext);
}
