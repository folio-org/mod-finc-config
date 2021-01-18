package org.folio.finc.select;

import org.folio.finc.select.query.MetadataCollectionsQueryTranslator;
import org.folio.finc.select.query.QueryTranslator;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(value = Parameterized.class)
public class MetadataCollectionsQueryTranslatorTest {

  private static final String isil = "ISIL-01";

  private static QueryTranslator cut;

  @BeforeClass
  public static void setUp() {
    cut = new MetadataCollectionsQueryTranslator();
  }

  private String query;
  private String expected;

  public MetadataCollectionsQueryTranslatorTest(String query, String expected) {
    this.query = query;
    this.expected = expected;
  }

  @Parameterized.Parameters(name = "{index}: translateQuery({0}) = {1}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {
            "mdSource.id=\"uuid-1234\" AND permitted=no",
            "(mdSource.id=\"uuid-1234\") AND (cql.allRecords=1 NOT permittedFor == \"*\\\""
                + isil
                + "\\\"*\" AND usageRestricted=\"yes\")"
          },
          {
            "mdSource.id=\"uuid-1234\" AND permitted=yes",
            "(mdSource.id=\"uuid-1234\") AND (permittedFor == \"*\\\""
                + isil
                + "\\\"*\" OR usageRestricted=\"no\")"
          },
          {
            "permitted=no",
            "(cql.allRecords=1 NOT permittedFor == \"*\\\""
                + isil
                + "\\\"*\" AND usageRestricted=\"yes\")"
          },
          {
            "permitted=yes",
            "(permittedFor == \"*\\\"" + isil + "\\\"*\" OR usageRestricted=\"no\")"
          },
          {
            "mdSource.id=\"uuid-1234\" AND selected=\"no\"",
            "(mdSource.id=\"uuid-1234\") AND (cql.allRecords=1 NOT selectedBy == \"*\\\""
                + isil
                + "\\\"*\")"
          },
          {
            "mdSource.id=\"uuid-1234\" AND selected=\"yes\"",
            "(mdSource.id=\"uuid-1234\") AND (selectedBy == \"*\\\"" + isil + "\\\"*\")"
          },
          {"selected=no", "(cql.allRecords=1 NOT selectedBy == \"*\\\"" + isil + "\\\"*\")"},
          {"selected=yes", "(selectedBy == \"*\\\"" + isil + "\\\"*\")"},
          {
            "mdSource.id=\"uuid-1234\" AND selected=no AND permitted=no",
            String.format(
                "(mdSource.id=\"uuid-1234\") AND (cql.allRecords=1 NOT selectedBy == \"*\\\"%s\\\"*\") AND (cql.allRecords=1 NOT permittedFor == \"*\\\"%s\\\"*\" AND usageRestricted=\"yes\")",
                isil, isil)
          },
          {
            "mdSource.id=\"uuid-1234\" AND selected=yes AND permitted=yes",
            String.format(
                "(mdSource.id=\"uuid-1234\") AND (selectedBy == \"*\\\"%s\\\"*\") AND (permittedFor == \"*\\\"%s\\\"*\" OR usageRestricted=\"no\")",
                isil, isil)
          },
          {
            "selected=no AND permitted=no",
            String.format(
                "(cql.allRecords=1 NOT selectedBy == \"*\\\"%s\\\"*\") AND (cql.allRecords=1 NOT permittedFor == \"*\\\"%s\\\"*\" AND usageRestricted=\"yes\")",
                isil, isil)
          },
          {
            "selected=yes AND permitted=yes",
            String.format(
                "(selectedBy == \"*\\\"%s\\\"*\") AND (permittedFor == \"*\\\"%s\\\"*\" OR usageRestricted=\"no\")",
                isil, isil)
          },
          {
            "mdSource.id=\"uuid-1234\" AND selected=no AND permitted=yes",
            String.format(
                "(mdSource.id=\"uuid-1234\") AND (cql.allRecords=1 NOT selectedBy == \"*\\\"%s\\\"*\") AND (permittedFor == \"*\\\"%s\\\"*\" OR usageRestricted=\"no\")",
                isil, isil)
          },
          {
            "selected=no AND permitted=yes",
            String.format(
                "(cql.allRecords=1 NOT selectedBy == \"*\\\"%s\\\"*\") AND (permittedFor == \"*\\\"%s\\\"*\" OR usageRestricted=\"no\")",
                isil, isil)
          },
          {
            "selected=(\"yes\" or \"no\")",
            "((selectedBy == \"*\\\""
                + isil
                + "\\\"*\") OR (cql.allRecords=1 NOT selectedBy == \"*\\\""
                + isil
                + "\\\"*\"))"
          },
          {
            "permitted=(\"yes\" or \"no\")",
            "((permittedFor == \"*\\\""
                + isil
                + "\\\"*\" OR usageRestricted=\"no\") OR (cql.allRecords=1 NOT permittedFor == \"*\\\""
                + isil
                + "\\\"*\" AND usageRestricted=\"yes\"))"
          },
          {
            "permitted=(\"no\" or \"yes\")",
            "((cql.allRecords=1 NOT permittedFor == \"*\\\""
                + isil
                + "\\\"*\" AND usageRestricted=\"yes\") OR (permittedFor == \"*\\\""
                + isil
                + "\\\"*\" OR usageRestricted=\"no\"))"
          },
          {
            "permitted=(\"no\" or \"yes\") AND selected=(\"yes\" or \"no\")",
            "((selectedBy == \"*\\\""
                + isil
                + "\\\"*\") OR (cql.allRecords=1 NOT selectedBy == \"*\\\""
                + isil
                + "\\\"*\")) AND "
                + "((cql.allRecords=1 NOT permittedFor == \"*\\\""
                + isil
                + "\\\"*\" AND usageRestricted=\"yes\") OR (permittedFor == \"*\\\""
                + isil
                + "\\\"*\" OR usageRestricted=\"no\"))"
          },
          {
            "mdSource.id=\"uuid-1234\" AND permitted=(\"no\" or \"yes\") AND selected=(\"yes\" or \"no\")",
            "(mdSource.id=\"uuid-1234\") AND "
                + "((selectedBy == \"*\\\""
                + isil
                + "\\\"*\") OR (cql.allRecords=1 NOT selectedBy == \"*\\\""
                + isil
                + "\\\"*\")) AND "
                + "((cql.allRecords=1 NOT permittedFor == \"*\\\""
                + isil
                + "\\\"*\" AND usageRestricted=\"yes\") OR (permittedFor == \"*\\\""
                + isil
                + "\\\"*\" OR usageRestricted=\"no\"))"
          },
          {
            "mdSource.id=\"uuid-1234\" AND permitted=(\"no\" or \"yes\") AND selected=(\"yes\" or \"no\") AND usageRestricted=\"no\"",
            "(mdSource.id=\"uuid-1234\") AND (usageRestricted=\"no\") AND "
                + "((selectedBy == \"*\\\""
                + isil
                + "\\\"*\") OR (cql.allRecords=1 NOT selectedBy == \"*\\\""
                + isil
                + "\\\"*\")) AND "
                + "((cql.allRecords=1 NOT permittedFor == \"*\\\""
                + isil
                + "\\\"*\" AND usageRestricted=\"yes\") OR (permittedFor == \"*\\\""
                + isil
                + "\\\"*\" OR usageRestricted=\"no\"))"
          },
          {
            "permitted=\"yes\" or \"no\" AND selected=\"yes\"",
            "(selectedBy == \"*\\\""
                + isil
                + "\\\"*\") AND "
                + "((permittedFor == \"*\\\""
                + isil
                + "\\\"*\" OR usageRestricted=\"no\") OR (cql.allRecords=1 NOT permittedFor == \"*\\\""
                + isil
                + "\\\"*\" AND usageRestricted=\"yes\"))"
          },
          {
            "permitted=no sortby label/sort.descending",
            "(cql.allRecords=1 NOT permittedFor == \"*\\\""
                + isil
                + "\\\"*\" AND usageRestricted=\"yes\") sortby label/sort.descending"
          },
          {
            "(selected=\"no\" and permitted=\"no\" and freeContent=(\"yes\" or \"undetermined\"))",
            "(freeContent=(\"yes\" or \"undetermined\")) AND (cql.allRecords=1 NOT selectedBy == \"*\\\"ISIL-01\\\"*\") AND (cql.allRecords=1 NOT permittedFor == \"*\\\"ISIL-01\\\"*\" AND usageRestricted=\"yes\")"
          },
          {
            "(freeContent=(\"yes\" or \"undetermined\") and selected=\"no\" and permitted=\"no\")",
            "(freeContent=(\"yes\" or \"undetermined\")) AND (cql.allRecords=1 NOT selectedBy == \"*\\\"ISIL-01\\\"*\") AND (cql.allRecords=1 NOT permittedFor == \"*\\\"ISIL-01\\\"*\" AND usageRestricted=\"yes\")"
          },
          {
            "(selected=\"no\" and freeContent=(\"yes\" or \"undetermined\") and permitted=\"no\")",
            "(freeContent=(\"yes\" or \"undetermined\")) AND (cql.allRecords=1 NOT selectedBy == \"*\\\"ISIL-01\\\"*\") AND (cql.allRecords=1 NOT permittedFor == \"*\\\"ISIL-01\\\"*\" AND usageRestricted=\"yes\")"
          },
          {null, null}
        });
  }

  @Test
  public void testTranslate() {
    String result = cut.translateQuery(query, isil);
    assertEquals(expected, result);
  }
}
