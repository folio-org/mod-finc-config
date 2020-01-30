package org.folio.finc.select.isil.filter;

import java.util.List;
import java.util.stream.Collectors;

public interface IsilFilter<T, S> {

  default List<T> filterForIsil(List<S> collection, String isil) {
    return collection.stream()
        .map(entry -> this.filterForIsil(entry, isil))
        .collect(Collectors.toList());
  }

  T filterForIsil(S entry, String isil);
}
