package org.folio.finc.select.query;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.UnaryOperator;
import org.folio.finc.select.exception.FincSelectInvalidQueryException;

public abstract class QueryTranslator {

  private static final String PERMITTED = "permitted";
  private static final String SELECTED = "selected";
  private static final String AND = "AND";

  public String translateQuery(String query, String isil) {

    if (query == null || "".equals(query)) {
      return query;
    }

    String[] queryAndSortBy = splitSortBy(query);
    query = queryAndSortBy[0];
    String sortBy = queryAndSortBy[1];

    String permitted = "";
    String selected = "";
    StringBuilder sb = new StringBuilder();
    String[] ands = query.split("[aA][nN][dD]");
    for (String s : ands) {
      if (s.contains(PERMITTED)) {
        permitted = processPermittedQuery(s, isil);
      } else if (s.contains(SELECTED)) {
        selected = processSelectedQuery(s, isil);
      } else {
        String tmp = processRemainingQuery(s);
        sb.append(calculateAppendable(sb.toString(), tmp));
      }
    }

    String result = balanceBrackets(sb.toString());
    result += calculateAppendable(result, selected);
    result += calculateAppendable(result, permitted);
    result += sortBy;
    return result;
  }

  private String processSelectedQuery(String query, String isil) {
    return doTranslate(query, SELECTED, isil, this::selectedBy);
  }

  private String processPermittedQuery(String query, String isil) {
    return doTranslate(
        query, PERMITTED, isil, this::permittedFor);
  }

  private String processRemainingQuery(String query) {
    if ("".equals(query)) {
      return "";
    } else {
      return "(" + query.trim() + ")";
    }
  }

  private String selectedBy(String isil) {
    return String.format("selectedBy = \"%s\"", isil);
  }

  private String permittedFor(String isil) {
    return String.format("permittedFor = \"%s\"", isil);
  }

  protected String addUsageRestrictedNoIfUsagePermitted(String query) {
    if (query.contains("permittedFor")) {
      return query + " OR usageRestricted=\"no\"";
    }
    return query;
  }

  protected String addUsageRestrictedYesIfUsagePermitted(String query) {
    if (query.contains("permittedFor")) {
      return query + " AND usageRestricted=\"yes\"";
    }
    return query;
  }

  String prepareQuery(String query) {
    query = query.trim();
    int queryLength = query.length();
    int leadingParenthesesIndex = query.indexOf('(');
    int trialingParenthesesIndex = query.lastIndexOf((')'));
    if (leadingParenthesesIndex == 0) {
      query = query.substring(1);
    }
    if (trialingParenthesesIndex == queryLength - 1) {
      query = query.substring(0, query.length() - 1);
    }
    return query;
  }

  private String calculateAppendable(String query, String toAppend) {
    if ("".equals(query) || "".equals(toAppend)) {
      return toAppend;
    } else {
      return " " + AND + " " + toAppend;
    }
  }

  public String[] splitSortBy(String query) {
    if (query == null) {
      return new String[]{"", ""};
    }
    int sortbyIndex = query.toLowerCase().indexOf("sortby");
    String sortBy = "";
    if (sortbyIndex != -1) {
      sortBy = " " + query.substring(sortbyIndex);
      query = query.substring(0, sortbyIndex);
    }
    return new String[]{query, sortBy};
  }

  private String balanceBrackets(String query) {
    StringBuilder sb = new StringBuilder();
    sb.append(query);

    Deque<SimpleEntry<Character, Integer>> stack = new ArrayDeque<>();
    char[] chars = query.toCharArray();
    for (int i = 0; i < chars.length; i++) {
      char current = chars[i];
      if (current == '(') {
        SimpleEntry<Character, Integer> entry = new SimpleEntry<>(current, i);
        stack.addFirst(entry);
      }

      if (current == ')') {
        if (stack.isEmpty()) {
          sb.deleteCharAt(i);
        }
        if (!stack.isEmpty()) {
          char last = stack.peekFirst().getKey();
          if (last == '(') {
            stack.removeFirst();
          } else {
            throw new FincSelectInvalidQueryException("Invalid query");
          }
        }
      }
    }
    stack.forEach(entry -> sb.deleteCharAt(entry.getValue()));
    return sb.toString();
  }

  abstract String doTranslate(
      String query,
      String key,
      String isil,
      UnaryOperator<String> replaceQueryFunc);
}
