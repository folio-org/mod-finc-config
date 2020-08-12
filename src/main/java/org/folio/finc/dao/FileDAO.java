package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Future;
import org.folio.finc.model.File;
import org.folio.rest.persist.Criteria.Criterion;

public interface FileDAO {

  Future<File> getById(String id, Context vertxContext);

  Future<File> getByCriterion(Criterion criterion, Context vertxContext);

  Future<File> upsert(File entity, String id, Context vertxContext);

  Future<Integer> deleteById(String id, Context vertxContext);
}
