package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Future;
import org.folio.finc.model.File;

public interface SelectFileDAO {

  Future<File> getById(String id, String isil, Context vertxContext);

  Future<File> upsert(File entity, String id, Context vertxContext);

  Future<Integer> deleteById(String id, String isil, Context vertxContext);
}
