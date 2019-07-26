package org.folio.finc.select;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class QueryTranslatorTest {

  private static final String isil = "ISIL-01";

  @Test
  public void translatePermittedFalse() {
    String query = "mdSource.id=\"uuid-1234\" AND permitted=no";
    String expected =
        "(mdSource.id=\"uuid-1234\") AND (cql.allRecords=1 NOT permittedFor any \"" + isil + "\")";
    String result = QueryTranslator.translate(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translatePermittedTrue() {
    String query = "mdSource.id=\"uuid-1234\" AND permitted=yes";
    String expected = "(mdSource.id=\"uuid-1234\") AND (permittedFor any \"" + isil + "\")";
    String result = QueryTranslator.translate(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translatePermittedFalseWithoutSource() {
    String query = "permitted=no";
    String expected = "(cql.allRecords=1 NOT permittedFor any \"" + isil + "\")";
    String result = QueryTranslator.translate(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translatePermittedTrueWithoutSource() {
    String query = "permitted=yes";
    String expected = "(permittedFor any \"" + isil + "\")";
    String result = QueryTranslator.translate(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedFalse() {
    String query = "mdSource.id=\"uuid-1234\" AND selected=\"no\"";
    String expected =
        "(mdSource.id=\"uuid-1234\") AND (cql.allRecords=1 NOT selectedBy any \"" + isil + "\")";
    String result = QueryTranslator.translate(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedTrue() {
    String query = "mdSource.id=\"uuid-1234\" AND selected=\"yes\"";
    String expected = "(mdSource.id=\"uuid-1234\") AND (selectedBy any \"" + isil + "\")";
    String result = QueryTranslator.translate(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedFalseWithoutSource() {
    String query = "selected=no";
    String expected = "(cql.allRecords=1 NOT selectedBy any \"" + isil + "\")";
    String result = QueryTranslator.translate(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedTrueWithoutSource() {
    String query = "selected=yes";
    String expected = "(selectedBy any \"" + isil + "\")";
    String result = QueryTranslator.translate(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedPermittedFalse() {
    String query = "mdSource.id=\"uuid-1234\" AND selected=no AND permitted=no";
    String expected =
        String.format(
            "(mdSource.id=\"uuid-1234\") AND (cql.allRecords=1 NOT selectedBy any \"%s\") AND (cql.allRecords=1 NOT permittedFor any \"%s\")",
            isil, isil);
    String result = QueryTranslator.translate(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedPermittedTrue() {
    String query = "mdSource.id=\"uuid-1234\" AND selected=yes AND permitted=yes";
    String expected =
        String.format(
            "(mdSource.id=\"uuid-1234\") AND (selectedBy any \"%s\") AND (permittedFor any \"%s\")",
            isil, isil);
    String result = QueryTranslator.translate(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedPermittedFalseWithoutSource() {
    String query = "selected=no AND permitted=no";
    String expected =
        String.format(
            "(cql.allRecords=1 NOT selectedBy any \"%s\") AND (cql.allRecords=1 NOT permittedFor any \"%s\")",
            isil, isil);
    String result = QueryTranslator.translate(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedPermittedTrueWithoutSource() {
    String query = "selected=yes AND permitted=yes";
    String expected =
        String.format("(selectedBy any \"%s\") AND (permittedFor any \"%s\")", isil, isil);
    String result = QueryTranslator.translate(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedFalsePermittedTrue() {
    String query = "mdSource.id=\"uuid-1234\" AND selected=no AND permitted=yes";
    String expected =
        String.format(
            "(mdSource.id=\"uuid-1234\") AND (cql.allRecords=1 NOT selectedBy any \"%s\") AND (permittedFor any \"%s\")",
            isil, isil);
    String result = QueryTranslator.translate(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedFalsePermittedTrueWithoutSource() {
    String query = "selected=no AND permitted=yes";
    String expected =
        String.format(
            "(cql.allRecords=1 NOT selectedBy any \"%s\") AND (permittedFor any \"%s\")",
            isil, isil);
    String result = QueryTranslator.translate(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedTrueAndFalseWithoutSource() {
    String query = "selected=(\"yes\" or \"no\")";
    String expected =
        "((selectedBy any \""
            + isil
            + "\") OR (cql.allRecords=1 NOT selectedBy any \""
            + isil
            + "\"))";
    String result = QueryTranslator.translate(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translatePermittedTrueAndFalseWithoutSource() {
    String query = "permitted=(\"yes\" or \"no\")";
    String expected =
        "((permittedFor any \""
            + isil
            + "\") OR (cql.allRecords=1 NOT permittedFor any \""
            + isil
            + "\"))";
    String result = QueryTranslator.translate(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translatePermittedFalseAndTrueWithoutSource() {
    String query = "permitted=(\"no\" or \"yes\")";
    String expected =
        "((cql.allRecords=1 NOT permittedFor any \""
            + isil
            + "\") OR (permittedFor any \""
            + isil
            + "\"))";
    String result = QueryTranslator.translate(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translatePermittedFalseAndTrueWithSelectedTrueAndFalseWithoutSource() {
    String query = "permitted=(\"no\" or \"yes\") AND selected=(\"yes\" or \"no\")";
    String expected =
        "((selectedBy any \""
            + isil
            + "\") OR (cql.allRecords=1 NOT selectedBy any \""
            + isil
            + "\")) AND "
            + "((cql.allRecords=1 NOT permittedFor any \""
            + isil
            + "\") OR (permittedFor any \""
            + isil
            + "\"))";

    String result = QueryTranslator.translate(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateNullQuery() {
    String query = null;
    String result = QueryTranslator.translate(query, isil);
    assertNull(result);
  }
}
