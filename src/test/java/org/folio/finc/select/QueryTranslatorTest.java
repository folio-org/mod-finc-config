package org.folio.finc.select;

import static org.junit.Assert.*;

import org.junit.Test;

public class QueryTranslatorTest {

  private static final String isil = "ISIL-01";

  @Test
  public void translatePermittedFalse() {
    String query = "mdSource.id=\"uuid-1234\" AND permitted=false";
    String expected = "mdSource.id=\"uuid-1234\" NOT permittedFor any \"" + isil + "\"";
    String result = QueryTranslator.translate(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translatePermittedTrue() {
    String query = "mdSource.id=\"uuid-1234\" AND permitted=true";
    String expected = "mdSource.id=\"uuid-1234\" AND permittedFor any \"" + isil + "\"";
    String result = QueryTranslator.translate(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translatePermittedFalseWithoutSource() {
    String query = "permitted=false";
    String expected = "cql.allRecords=1 NOT permittedFor any \"" + isil + "\"";
    String result = QueryTranslator.translate(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translatePermittedTrueWithoutSource() {
    String query = "permitted=true";
    String expected = "permittedFor any \"" + isil + "\"";
    String result = QueryTranslator.translate(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedFalse() {
    String query = "mdSource.id=\"uuid-1234\" AND selected=false";
    String expected = "mdSource.id=\"uuid-1234\" NOT selectedBy any \"" + isil + "\"";
    String result = QueryTranslator.translate(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedTrue() {
    String query = "mdSource.id=\"uuid-1234\" AND selected=true";
    String expected = "mdSource.id=\"uuid-1234\" AND selectedBy any \"" + isil + "\"";
    String result = QueryTranslator.translate(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedFalseWithoutSource() {
    String query = "selected=false";
    String expected = "cql.allRecords=1 NOT selectedBy any \"" + isil + "\"";
    String result = QueryTranslator.translate(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedTrueWithoutSource() {
    String query = "selected=true";
    String expected = "selectedBy any \"" + isil + "\"";
    String result = QueryTranslator.translate(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedPermittedFalse() {
    String query = "mdSource.id=\"uuid-1234\" AND selected=false AND permitted=false";
    String expected = String.format("mdSource.id=\"uuid-1234\" NOT selectedBy any \"%s\" NOT permittedFor any \"%s\"", isil, isil);
    String result = QueryTranslator.translate(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedPermittedTrue() {
    String query = "mdSource.id=\"uuid-1234\" AND selected=true AND permitted=true";
    String expected = String.format("mdSource.id=\"uuid-1234\" AND selectedBy any \"%s\" AND permittedFor any \"%s\"", isil, isil);
    String result = QueryTranslator.translate(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedPermittedFalseWithoutSource() {
    String query = "selected=false AND permitted=false";
    String expected = String.format("cql.allRecords=1 NOT selectedBy any \"%s\" NOT permittedFor any \"%s\"", isil, isil);
    String result = QueryTranslator.translate(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedPermittedTrueWithoutSource() {
    String query = "selected=true AND permitted=true";
    String expected = String.format("selectedBy any \"%s\" AND permittedFor any \"%s\"", isil, isil);
    String result = QueryTranslator.translate(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedFalsePermittedTrue() {
    String query = "mdSource.id=\"uuid-1234\" AND selected=false AND permitted=true";
    String expected = String.format("mdSource.id=\"uuid-1234\" NOT selectedBy any \"%s\" AND permittedFor any \"%s\"", isil, isil);
    String result = QueryTranslator.translate(query, isil);
    assertNotNull(result);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedFalePermittedTrueWithoutSource() {
    String query = "selected=false AND permitted=true";
    String expected = String.format("cql.allRecords=1 NOT selectedBy any \"%s\" AND permittedFor any \"%s\"", isil, isil);
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
