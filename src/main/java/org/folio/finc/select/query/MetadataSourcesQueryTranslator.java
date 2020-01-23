package org.folio.finc.select.query;

import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetadataSourcesQueryTranslator extends QueryTranslator {

  @Override
  String doTranslate(
      String query,
      String key,
      String isil,
      UnaryOperator<String> replaceQueryFunc,
      UnaryOperator<String> postProcessQueryFunc) {

    query = prepareQuery(query);

    if (!query.contains(key)) {
      return query;
    }

    Pattern pattern =
        Pattern.compile(
            key
                + "=\\(?(\")?(?<first>all|some|none)(\")?(\\s?(?<second>[Aa][Nn][Dd]|[Oo][Rr])\\s?(\")?(?<third>all|some|none)(\")?)?(\\s?(?<fourth>[Aa][Nn][Dd]|[Oo][Rr])\\s?(\")?(?<fifth>all|some|none)(\")?)?\\)?",
            Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(query);
    if (matcher.find()) {
      String firstAllSomeNone = matcher.group("first");
      String firstMultiValAndOr = matcher.group("second");
      String secondAllSomeNone = matcher.group("third");
      String secondMultiValAndOr = matcher.group("fourth");
      String thirdAllSomeNone = matcher.group("fifth");

      String group = matcher.group();

      if (firstAllSomeNone != null) {
        String q = formatQuery(isil, firstAllSomeNone);
        query = query.replace(group, q);
      }
      if (firstMultiValAndOr != null) {
        query = "(" + query + ")" + " " + firstMultiValAndOr.toUpperCase();
        if (secondAllSomeNone != null) {
          String q = formatQuery(isil, secondAllSomeNone);
          query = query + " (" + q + ")";
        }
      }
      if (secondMultiValAndOr != null) {
        query = query + " " + secondMultiValAndOr.toUpperCase();
        if (thirdAllSomeNone != null) {
          String q = formatQuery(isil, thirdAllSomeNone);
          query = query + " (" + q + ")";
        }
      }
    }
    return "(" + query + ")";
  }

  private String formatQuery(String isil, String selected) {
    return String.format(
        "selectedBy == \"*\\\"isil\\\": \\\"%s\\\", \\\"selected\\\": \\\"%s\\\"*\"",
        isil, selected);
  }
}
