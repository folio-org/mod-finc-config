package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.FincSelectFilterToCollections;

public interface FilterToCollectionsDAO {

  Future<FincSelectFilterToCollections> getById(String filterId, String isil, Context vertxContext);

  Future<FincSelectFilterToCollections> insert(
      FincSelectFilterToCollections entity, Context vertxContext);

  Future<Integer> deleteById(String id, Context vertxContext);
}
