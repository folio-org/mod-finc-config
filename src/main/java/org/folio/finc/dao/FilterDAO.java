package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Promise;
import org.folio.rest.jaxrs.model.FincSelectFilter;
import org.folio.rest.jaxrs.model.FincSelectFilters;

public interface FilterDAO {

  Promise<FincSelectFilters> getAll(
      String query, int offset, int limit, String isil, Context vertxContext);

  Promise<FincSelectFilter> getById(String id, String isil, Context vertxContext);

  Promise<FincSelectFilter> insert(FincSelectFilter entity, Context vertxContext);

  Promise<FincSelectFilter> update(FincSelectFilter entity, String id, Context vertxContext);

  Promise<Integer> deleteById(String id, String isil, Context vertxContext);
}
