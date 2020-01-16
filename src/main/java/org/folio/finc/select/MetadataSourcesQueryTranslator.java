package org.folio.finc.select;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.folio.finc.select.exception.FincSelectInvalidQueryException;

public class MetadataSourcesQueryTranslator {

  private static final String SELECTED = "selected";
  private static final String AND = "AND";

  public static String translate(String query, String isil) {

    if (query == null || "".equals(query)) {
      return query;
    }

    String[] queryAndSortBy = splitSortBy(query);
    query = queryAndSortBy[0];
    String sortBy = queryAndSortBy[1];

    String selected = "";
    String q = "";
    String[] ands = query.split("[aA][nN][dD]");
    for (String s : ands) {
      if (s.contains(SELECTED)) {
        selected = processSelectedQuery(s, isil);
      } else {
        String tmp = processRemainingQuery(s);
        q += calculateAppendable(q, tmp);
      }
    }

    String result = balanceBrackets(q);
    result += calculateAppendable(result, selected);
    result += sortBy;
    return result;
  }

  private static String processSelectedQuery(String query, String isil) {
    return translate(
      query, SELECTED, isil, MetadataSourcesQueryTranslator::selectedBy, Function.identity());
  }

  private static String processRemainingQuery(String query) {
    if ("".equals(query)) {
      return "";
    } else {
      return "(" + query.trim() + ")";
    }
  }

  private static String selectedBy(String isil) {
    return String.format("selectedBy any \"%s\"", isil);
  }

  private static String translate(
    String query,
    String key,
    String isil,
    UnaryOperator<String> replaceQueryFunc,
    Function<String, String> postProcessQueryFunc) {

    query = prepareQuery(query);

    if (!query.contains(key)) {
      return query;
    }

    Pattern pattern =
        Pattern.compile(
            key
                + "=\\(?(\")?(?<first>all|some|none)(\")?(\\s?(?<second>[Aa][Nn][Dd]|[Oo][Rr])\\s?(\")?(?<third>all|some|none)(\")?)?(\\s?(?<fourth>[Aa][Nn][Dd]|[Oo][Rr])\\s?(\")?(?<fifth>all|some|none)(\")?)?\\)?",
            Pattern.CASE_INSENSITIVE);
//    Pattern pattern =
//      Pattern.compile(
//        key
//          + "=\\(?(\")?(?<first>all|some|none)(\")?(\\s?(?<second>[Aa][Nn][Dd]|[Oo][Rr])\\s?(\")?(?<third>all|some|none)(\")?)?\\)?",
//        Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(query);
    if (matcher.find()) {
      String firstAllSomeNone = matcher.group("first");
      String firstMultiValAndOr = matcher.group("second");
      String secondAllSomeNone = matcher.group("third");
      String secondMultiValAndOr = matcher.group("fourth");
      String thirdAllSomeNone = matcher.group("fifth");

      String group = matcher.group();

      if (firstAllSomeNone != null) {
        String q = String.format("selectedBy == \"*\\\"isil\\\": \\\"%s\\\", \\\"selected\\\": \\\"%s\\\"*\"", isil, firstAllSomeNone);
        query = query.replace(group, q);
      }
      if (firstMultiValAndOr != null) {
        query = "(" + query + ")" + " " + firstMultiValAndOr.toUpperCase();
        if (secondAllSomeNone != null) {
          String q = String.format("selectedBy == \"*\\\"isil\\\": \\\"%s\\\", \\\"selected\\\": \\\"%s\\\"*\"", isil, secondAllSomeNone);
          query = query + " (" + q + ")";
        }
      }
      if (secondMultiValAndOr != null) {
        query = query + " " + secondMultiValAndOr.toUpperCase();
        if (thirdAllSomeNone != null) {
          String q = String.format("selectedBy == \"*\\\"isil\\\": \\\"%s\\\", \\\"selected\\\": \\\"%s\\\"*\"", isil, thirdAllSomeNone);
          query = query + " (" + q + ")";
        }
      }
    }
    return "(" + query + ")";
  }

  private static String prepareQuery(String query) {
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

  private static String calculateAppendable(String query, String toAppend) {
    if ("".equals(query) || "".equals(toAppend)) {
      return toAppend;
    } else {
      return " " + AND + " " + toAppend;
    }
  }

  public static String[] splitSortBy(String query) {
    if (query == null) {
      return new String[] {"", ""};
    }
    int sortbyIndex = query.toLowerCase().indexOf("sortby");
    String sortBy = "";
    if (sortbyIndex != -1) {
      sortBy = " " + query.substring(sortbyIndex);
      query = query.substring(0, sortbyIndex);
    }
    return new String[] {query, sortBy};
  }

  private static String balanceBrackets(String query) {
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
}
