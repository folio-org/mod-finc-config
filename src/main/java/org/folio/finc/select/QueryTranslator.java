package org.folio.finc.select;

import java.util.Arrays;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.folio.cql2pgjson.CQL2PgJSON;
import org.folio.cql2pgjson.exception.FieldException;
import org.folio.rest.persist.cql.CQLWrapper;

// TODO: NEEDS REFACTORING! (Tests are passing)

public class QueryTranslator {

  private QueryTranslator() {
    throw new IllegalStateException("Utility class");
  }

  public static String translate(String query, String isil) {

    if (query == null || "".equals(query)) {
      return query;
    }
    /*String result = processSelectedQuery(query, isil);
    return processPermittedQuery(result, isil);*/

    String permitted = "";
    String selected = "";
    String q = "";
    String[] ands = query.split("AND");
    for (String s : ands) {
      if (s.contains("permitted")) {
        permitted = processPermittedQuery(s, isil);
      } else if (s.contains("selected")) {
        selected = processSelectedQuery(s, isil);
      }
      else {
        q += s;
      }
    }

    /*String selected = processSelectedQuery(query, isil);
    String permitted = processPermittedQuery(query, isil);*/

    String result = q.equals("") ? "" : "(" + q.trim() + ")";

    /*if (selected != "" && !selected.contains("NOT")) {
      selected = "AND " + selected;
    }
    if (permitted != "" && !permitted.contains("NOT")) {
      permitted = "AND " + permitted;
    }*/

 /*   if (result.equals("")) {
      result += selected;
    } else {
      if (selected.contains("NOT")) {
        result += selected;
      } else {
        result += " AND " + selected;
      }
    }*/

    result += result.equals("") || selected.equals("") ? selected : " AND " + selected;
    result += result.equals("") || permitted.equals("") ? permitted : " AND " + permitted;
    return result;
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
    query = query.trim();
    if (!query.contains(key)) {
      return query;
    }

    Pattern pattern =
        Pattern.compile(
            key
                + "=\\(?(\\\")?(?<first>[Yy][Ee][Ss]|[Nn][Oo])(\\\")?(\\s?(?<second>[Aa][Nn][Dd]|[Oo][Rr])\\s?(\\\")?(?<third>[Yy][Ee][Ss]|[Nn][Oo])(\\\")?)?\\)?",
            Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(query);
    if (matcher.find()) {
      String firstValue = matcher.group("first");
      String multiValAndOr = matcher.group("second");
      String secondValue = matcher.group("third");
      String group = matcher.group();
      String replacedQuery = replaceQueryFunc.apply(isil);

/*      if ("yes".equals(firstValue)) {
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
        }*/


      if ("yes".equals(firstValue)) {
        query = query.replace(group, replacedQuery);
      } else { // selectedValue is false
        /*if (multiValAndOr != null) {
          query = query.replace(group, "NOT " + replacedQuery);
        } else {*/
          query = query.replace(group, "cql.allRecords=1 NOT " + replacedQuery);
//        }
//          query = group.replace(group, "cql.allRecords=1 NOT " + replacedQuery);
      }

      if (multiValAndOr != null) {
        query = "(" + query + ")" + " " + multiValAndOr.toUpperCase();
        if ("yes".equals(secondValue)) {
          query = query + " (" + replacedQuery + ")";
        } else {
          query = query + " (" + "cql.allRecords=1 NOT " + replacedQuery + ")";
        }
      }
      query = "(" + query + ")";
      System.out.println(query);
    }

    /*Pattern pattern =
    Pattern.compile(
        "(AND )?"
            + key
            + "=\\(?:(\\\")?first([Yy][Ee][Ss]|[Nn][Oo])(\\\")?(\\s?([Oo][Rr]|[Aa][Nn][Dd])\\s?(\\\")?([Yy][Ee][Ss]|[Nn][Oo])(\\\")?)?\\)?",
        Pattern.CASE_INSENSITIVE);*/
    /*Pattern pattern =
        Pattern.compile(
            "(?<firstAnd>AND )?"
                + key
                + "=\\(?(\\\")?(?<second>[Yy][Ee][Ss]|[Nn][Oo])(\\\")?(\\s?(?<third>[Oo][Rr]|[Aa][Nn][Dd])\\s?(\\\")?(?<fourth>[Yy][Ee][Ss]|[Nn][Oo])(\\\")?)?\\)?"
                + "(?<secondAnd>\\s?AND\\s?)?",
            Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(query);
    if (matcher.find()) {

      String firstAnd = matcher.group("firstAnd");
      String firstValue = matcher.group("second");
      String multiValAndOr = matcher.group("third");
      String secondValue = matcher.group("fourth");
      String secondAnd = matcher.group("secondAnd");
      String andValue = firstAnd != null ? firstAnd : secondAnd;
      String group = matcher.group();
      String replacedQuery = replaceQueryFunc.apply(isil);

      if (andValue != null) {
        String[] split = query.split(andValue);
        for (String s : split) {
          if (s.contains(key)) {
            return translate(s, key, isil, replaceQueryFunc);
          }
        }
      }

      if ("yes".equals(firstValue)) {
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

      if (multiValAndOr != null) {
        query = "(" + query + ")" + " " + multiValAndOr.toUpperCase();
        if ("yes".equals(secondValue)) {
          query = query + " (" + replacedQuery + ")";
        } else {
          query = query + " (" + "cql.allRecords=1 NOT " + replacedQuery + ")";
        }
      }*/

      /*  String andValue = matcher.group(1);
      String value = matcher.group(3);
      String group = matcher.group();
      String replacedQuery = replaceQueryFunc.apply(isil);
      if ("yes".equals(value)) {
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
      }*/
//    }
    return query;
  }

  private static CQLWrapper getCQL(String query, String isil) throws FieldException {
    CQL2PgJSON cql2PgJSON = new CQL2PgJSON(Arrays.asList("metadata_collections" + ".jsonb"));

    return new CQLWrapper(cql2PgJSON, query);
  }
}
