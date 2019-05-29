package org.folio.finc.select;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryTranslator {

  public static String translate(String query, String isil) {
    String result = processSelectedQuery(query, isil);
    return processPermittedQuery(result, isil);
  }

  private static String processSelectedQuery(String query, String isil) {
    return translate(query, "selected", isil, QueryTranslator::selectedBy);
  }

  private static String processPermittedQuery(String query, String isil) {
    return translate(query, "permitted", isil, QueryTranslator::permittedFor);
  }

  private static String selectedBy(String isil) {
    return String.format("selectedBy any \"%s\"", isil);
  }

  private static String permittedFor(String isil) {
    return String.format("permittedFor any \"%s\"", isil);
  }

  private static String translate(
      String query, String key, String isil, Function<String, String> replaceQueryFunc) {
    if (query.contains(key)) {
      Pattern pattern =
          Pattern.compile(
              "(AND )?" + key + "=([Tt][Rr][Uu][Ee]|[Ff][Aa][Ll][Ss][Ee])",
              Pattern.CASE_INSENSITIVE);
      Matcher matcher = pattern.matcher(query);
      if (matcher.find()) {
        String andValue = matcher.group(1);
        String value = matcher.group(2);
        String group = matcher.group();
        String replacedQuery = replaceQueryFunc.apply(isil);
        if ("true".equals(value)) {
          if (andValue != null) {
            query = query.replace(group, "AND " + replacedQuery);
          } else {
            query = query.replace(group, replacedQuery);
          }
        } else { // selectedValue is false
          if (andValue == null) {
            query = query.replace(group, "cql.allRecords=1 NOT " + replacedQuery);
          } else {
            query = query.replace(group, "NOT " + replacedQuery);
          }
        }
      }
    }
    return query;
  }
}
