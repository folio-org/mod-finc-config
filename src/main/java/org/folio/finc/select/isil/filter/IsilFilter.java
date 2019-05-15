package org.folio.finc.select.isil.filter;

import java.util.List;
import java.util.stream.Collectors;

public abstract class IsilFilter<T, S> {

  public List<T> filterForIsil(List<S> collection, String isil) {
    return collection.stream()
        .map(entry -> this.filterForIsil(entry, isil))
        .collect(Collectors.toList());
  }

  public abstract T filterForIsil(S entry, String isil);
}
