package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Promise;
import org.folio.finc.model.File;

public interface FileDAO {

  Promise<File> getById(String id, String isil, Context vertxContext);

  Promise<File> upsert(File entity, String id, Context vertxContext);

  Promise<Integer> deleteById(String id, String isil, Context vertxContext);
}
