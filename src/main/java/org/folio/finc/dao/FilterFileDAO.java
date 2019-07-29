package org.folio.finc.dao;

import io.vertx.core.Context;
import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.FincSelectFilterFile;
import org.folio.rest.jaxrs.model.FincSelectFilterFiles;

public interface FilterFileDAO {

  Future<FincSelectFilterFiles> getAll(
      String query, int offset, int limit, String isil, Context vertxContext);

  Future<FincSelectFilterFile> getById(String id, String isil, Context vertxContext);

  Future<FincSelectFilterFile> insert(FincSelectFilterFile entity, Context vertxContext);

  Future<Integer> deleteById(String id, String isil, Context vertxContext);
}
