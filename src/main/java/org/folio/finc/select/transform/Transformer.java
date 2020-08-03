package org.folio.finc.select.transform;

import java.util.List;
import java.util.stream.Collectors;

public interface Transformer<T, S> {

  default List<T> transformCollection(List<S> collection, String isil) {
    return collection.stream()
        .map(entry -> this.transformEntry(entry, isil))
        .collect(Collectors.toList());
  }

  T transformEntry(S entry, String isil);
}
