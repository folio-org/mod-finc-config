package org.folio.finc.select.query;

import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Processes query made to finc-select metadata sources. Translates selected=(yes|no) and
 * permitted=(yes|no) to selectedBy resp. permittedBy queries with corresponding isil. E.g., if
 * library with isil 'ISIL-01' looks for selected metadata collections the query 'selected="yes"'
 * will be translated to 'selectedBy="ISIL-01"'.
 */
public class MetadataCollectionsQueryTranslator extends QueryTranslator {

  private static final String YES = "yes";
  private static final String CQL_ALL_RECORDS_1_NOT = "cql.allRecords=1 NOT";

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
                + "=\\(?(\")?(?<first>[Yy][Ee][Ss]|[Nn][Oo])(\")?(\\s?(?<second>[Aa][Nn][Dd]|[Oo][Rr])\\s?(\")?(?<third>[Yy][Ee][Ss]|[Nn][Oo])(\")?)?\\)?",
            Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(query);
    if (matcher.find()) {
      String firstYesNo = matcher.group("first");
      String multiValAndOr = matcher.group("second");
      String secondYesNo = matcher.group("third");
      String group = matcher.group();
      String replacedQuery = replaceQueryFunc.apply(isil);

      if (YES.equals(firstYesNo)) {
        String q = postProcessQueryFunc.apply(replacedQuery);
        query = query.replace(group, q);
      } else { // selectedValue is false
        query = query.replace(group, CQL_ALL_RECORDS_1_NOT + " " + replacedQuery);
      }

      if (multiValAndOr != null) {
        query = "(" + query + ")" + " " + multiValAndOr.toUpperCase();
        if (YES.equals(secondYesNo)) {
          String q = postProcessQueryFunc.apply(replacedQuery);
          query = query + " (" + q + ")";
        } else {
          query = query + " (" + CQL_ALL_RECORDS_1_NOT + " " + replacedQuery + ")";
        }
      }
    }
    return "(" + query + ")";
  }
}
