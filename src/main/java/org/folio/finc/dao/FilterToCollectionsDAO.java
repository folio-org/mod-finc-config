package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.FincSelectFilterToCollections;
import org.folio.rest.persist.Criteria.Criterion;

public interface FilterToCollectionsDAO {

  Future<FincSelectFilterToCollections> getByIdAndIsil(String filterId, String isil, Context vertxContext);

  Future<FincSelectFilterToCollections> getById(String filterId, Context vertxContext);

  Future<FincSelectFilterToCollections> getByCriterion(Criterion criterion, Context vertxContext);

  Future<FincSelectFilterToCollections> insert(
      FincSelectFilterToCollections entity, Context vertxContext);

  Future<Integer> deleteById(String id, Context vertxContext);
}
