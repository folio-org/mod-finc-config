package org.folio.finc.select;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.folio.finc.select.query.MetadataCollectionsQueryTranslator;
import org.folio.finc.select.query.QueryTranslator;
import org.junit.BeforeClass;
import org.junit.Test;

public class MetadataCollectionsQueryTranslatorTest {

  private static final String isil = "ISIL-01";

  private static QueryTranslator cut;

  @BeforeClass
  public static void setUp() {
    cut = new MetadataCollectionsQueryTranslator();
  }

  @Test
  public void translatePermittedFalse() {
    String query = "mdSource.id=\"uuid-1234\" AND permitted=no";
    String expected =
        "(mdSource.id=\"uuid-1234\") AND (cql.allRecords=1 NOT permittedFor == \"*\\\"" + isil
            + "\\\"*\" AND usageRestricted=\"yes\")";
    String result = cut.translateQuery(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translatePermittedTrue() {
    String query = "mdSource.id=\"uuid-1234\" AND permitted=yes";
    String expected =
        "(mdSource.id=\"uuid-1234\") AND (permittedFor == \"*\\\"" + isil
            + "\\\"*\" OR usageRestricted=\"no\")";
    String result = cut.translateQuery(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translatePermittedFalseWithoutSource() {
    String query = "permitted=no";
    String expected =
        "(cql.allRecords=1 NOT permittedFor == \"*\\\"" + isil
            + "\\\"*\" AND usageRestricted=\"yes\")";
    String result = cut.translateQuery(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translatePermittedTrueWithoutSource() {
    String query = "permitted=yes";
    String expected =
        "(permittedFor == \"*\\\"" + isil + "\\\"*\" OR usageRestricted=\"no\")";
    String result = cut.translateQuery(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedFalse() {
    String query = "mdSource.id=\"uuid-1234\" AND selected=\"no\"";
    String expected =
        "(mdSource.id=\"uuid-1234\") AND (cql.allRecords=1 NOT selectedBy == \"*\\\"" + isil
            + "\\\"*\")";
    String result = cut.translateQuery(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedTrue() {
    String query = "mdSource.id=\"uuid-1234\" AND selected=\"yes\"";
    String expected =
        "(mdSource.id=\"uuid-1234\") AND (selectedBy == \"*\\\"" + isil + "\\\"*\")";
    String result = cut.translateQuery(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedFalseWithoutSource() {
    String query = "selected=no";
    String expected =
        "(cql.allRecords=1 NOT selectedBy == \"*\\\"" + isil + "\\\"*\")";
    String result = cut.translateQuery(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedTrueWithoutSource() {
    String query = "selected=yes";
    String expected = "(selectedBy == \"*\\\"" + isil + "\\\"*\")";
    String result = cut.translateQuery(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedPermittedFalse() {
    String query = "mdSource.id=\"uuid-1234\" AND selected=no AND permitted=no";
    String expected =
        String.format(
            "(mdSource.id=\"uuid-1234\") AND (cql.allRecords=1 NOT selectedBy == \"*\\\"%s\\\"*\") AND (cql.allRecords=1 NOT permittedFor == \"*\\\"%s\\\"*\" AND usageRestricted=\"yes\")",
            isil, isil);
    String result = cut.translateQuery(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedPermittedTrue() {
    String query = "mdSource.id=\"uuid-1234\" AND selected=yes AND permitted=yes";
    String expected =
        String.format(
            "(mdSource.id=\"uuid-1234\") AND (selectedBy == \"*\\\"%s\\\"*\") AND (permittedFor == \"*\\\"%s\\\"*\" OR usageRestricted=\"no\")",
            isil, isil);
    String result = cut.translateQuery(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedPermittedFalseWithoutSource() {
    String query = "selected=no AND permitted=no";
    String expected =
        String.format(
            "(cql.allRecords=1 NOT selectedBy == \"*\\\"%s\\\"*\") AND (cql.allRecords=1 NOT permittedFor == \"*\\\"%s\\\"*\" AND usageRestricted=\"yes\")",
            isil, isil);
    String result = cut.translateQuery(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedPermittedTrueWithoutSource() {
    String query = "selected=yes AND permitted=yes";
    String expected =
        String.format(
            "(selectedBy == \"*\\\"%s\\\"*\") AND (permittedFor == \"*\\\"%s\\\"*\" OR usageRestricted=\"no\")",
            isil, isil);
    String result = cut.translateQuery(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedFalsePermittedTrue() {
    String query = "mdSource.id=\"uuid-1234\" AND selected=no AND permitted=yes";
    String expected =
        String.format(
            "(mdSource.id=\"uuid-1234\") AND (cql.allRecords=1 NOT selectedBy == \"*\\\"%s\\\"*\") AND (permittedFor == \"*\\\"%s\\\"*\" OR usageRestricted=\"no\")",
            isil, isil);
    String result = cut.translateQuery(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedFalsePermittedTrueWithoutSource() {
    String query = "selected=no AND permitted=yes";
    String expected =
        String.format(
            "(cql.allRecords=1 NOT selectedBy == \"*\\\"%s\\\"*\") AND (permittedFor == \"*\\\"%s\\\"*\" OR usageRestricted=\"no\")",
            isil, isil);
    String result = cut.translateQuery(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedTrueAndFalseWithoutSource() {
    String query = "selected=(\"yes\" or \"no\")";
    String expected =
        "((selectedBy == \"*\\\"" + isil + "\\\"*\") OR (cql.allRecords=1 NOT selectedBy == \"*\\\""
            + isil + "\\\"*\"))";
    String result = cut.translateQuery(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translatePermittedTrueAndFalseWithoutSource() {
    String query = "permitted=(\"yes\" or \"no\")";
    String expected =
        "((permittedFor == \"*\\\""
            + isil
            + "\\\"*\" OR usageRestricted=\"no\") OR (cql.allRecords=1 NOT permittedFor == \"*\\\""
            + isil
            + "\\\"*\" AND usageRestricted=\"yes\"))";
    String result = cut.translateQuery(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translatePermittedFalseAndTrueWithoutSource() {
    String query = "permitted=(\"no\" or \"yes\")";
    String expected =
        "((cql.allRecords=1 NOT permittedFor == \"*\\\"" + isil
            + "\\\"*\" AND usageRestricted=\"yes\") OR (permittedFor == \"*\\\"" + isil
            + "\\\"*\" OR usageRestricted=\"no\"))";
    String result = cut.translateQuery(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translatePermittedFalseAndTrueWithSelectedTrueAndFalseWithoutSource() {
    String query = "permitted=(\"no\" or \"yes\") AND selected=(\"yes\" or \"no\")";
    String expected =
        "((selectedBy == \"*\\\"" + isil + "\\\"*\") OR (cql.allRecords=1 NOT selectedBy == \"*\\\""
            + isil + "\\\"*\")) AND "
            + "((cql.allRecords=1 NOT permittedFor == \"*\\\"" + isil
            + "\\\"*\" AND usageRestricted=\"yes\") OR (permittedFor == \"*\\\"" + isil
            + "\\\"*\" OR usageRestricted=\"no\"))";

    String result = cut.translateQuery(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translatePermittedFalseAndTrueWithSelectedTrueAndFalseWithSource() {
    String query =
        "mdSource.id=\"uuid-1234\" AND permitted=(\"no\" or \"yes\") AND selected=(\"yes\" or \"no\")";
    String expected =
        "(mdSource.id=\"uuid-1234\") AND "
            + "((selectedBy == \"*\\\"" + isil
            + "\\\"*\") OR (cql.allRecords=1 NOT selectedBy == \"*\\\"" + isil + "\\\"*\")) AND "
            + "((cql.allRecords=1 NOT permittedFor == \"*\\\"" + isil
            + "\\\"*\" AND usageRestricted=\"yes\") OR (permittedFor == \"*\\\"" + isil
            + "\\\"*\" OR usageRestricted=\"no\"))";
    String result = cut.translateQuery(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translatePermittedFalseAndTrueWithSelectedTrueAndFalseWithSourceAndUsageRestricted() {
    String query =
        "mdSource.id=\"uuid-1234\" AND permitted=(\"no\" or \"yes\") AND selected=(\"yes\" or \"no\") AND usageRestricted=\"no\"";
    String expected =
        "(mdSource.id=\"uuid-1234\") AND (usageRestricted=\"no\") AND "
            + "((selectedBy == \"*\\\"" + isil
            + "\\\"*\") OR (cql.allRecords=1 NOT selectedBy == \"*\\\"" + isil + "\\\"*\")) AND "
            + "((cql.allRecords=1 NOT permittedFor == \"*\\\"" + isil
            + "\\\"*\" AND usageRestricted=\"yes\") OR (permittedFor == \"*\\\"" + isil
            + "\\\"*\" OR usageRestricted=\"no\"))";
    String result = cut.translateQuery(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translatePermittedFalseAndTrueWithSelected() {
    String query = "permitted=\"yes\" or \"no\" AND selected=\"yes\"";
    String expected =
        "(selectedBy == \"*\\\"" + isil + "\\\"*\") AND "
            + "((permittedFor == \"*\\\"" + isil
            + "\\\"*\" OR usageRestricted=\"no\") OR (cql.allRecords=1 NOT permittedFor == \"*\\\""
            + isil + "\\\"*\" AND usageRestricted=\"yes\"))";

    String result = cut.translateQuery(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translatePermittedFalseWithoutSourceAndSortBy() {
    String query = "permitted=no sortby label/sort.descending";
    String expected =
        "(cql.allRecords=1 NOT permittedFor == \"*\\\"" + isil
            + "\\\"*\" AND usageRestricted=\"yes\") sortby label/sort.descending";
    String result = cut.translateQuery(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateBalancing1() {
    String query =
        "(selected=\"no\" and permitted=\"no\" and freeContent=(\"yes\" or \"undetermined\"))";
    String expected =
        "(freeContent=(\"yes\" or \"undetermined\")) AND (cql.allRecords=1 NOT selectedBy == \"*\\\"ISIL-01\\\"*\") AND (cql.allRecords=1 NOT permittedFor == \"*\\\"ISIL-01\\\"*\" AND usageRestricted=\"yes\")";
    String result = cut.translateQuery(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateBalancing2() {
    String query =
        "(freeContent=(\"yes\" or \"undetermined\") and selected=\"no\" and permitted=\"no\")";
    String expected =
        "(freeContent=(\"yes\" or \"undetermined\")) AND (cql.allRecords=1 NOT selectedBy == \"*\\\"ISIL-01\\\"*\") AND (cql.allRecords=1 NOT permittedFor == \"*\\\"ISIL-01\\\"*\" AND usageRestricted=\"yes\")";
    String result = cut.translateQuery(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateBalancing3() {
    String query =
        "(selected=\"no\" and freeContent=(\"yes\" or \"undetermined\") and permitted=\"no\")";
    String expected =
        "(freeContent=(\"yes\" or \"undetermined\")) AND (cql.allRecords=1 NOT selectedBy == \"*\\\"ISIL-01\\\"*\") AND (cql.allRecords=1 NOT permittedFor == \"*\\\"ISIL-01\\\"*\" AND usageRestricted=\"yes\")";
    String result = cut.translateQuery(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateNullQuery() {
    String query = null;
    String result = cut.translateQuery(query, isil);
    assertNull(result);
  }
}
