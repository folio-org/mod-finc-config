package org.folio.finc.select;

import static org.junit.Assert.assertEquals;

import org.folio.finc.select.query.MetadataSourcesQueryTranslator;
import org.folio.finc.select.query.QueryTranslator;
import org.junit.BeforeClass;
import org.junit.Test;

public class MetadataSourceQueryTranslatorTest {

  private static final String ISIL = "ISIL-01";

  private static QueryTranslator cut;

  @BeforeClass
  public static void setUp() {
    cut = new MetadataSourcesQueryTranslator();
  }

  @Test
  public void translateSelectedAll() {
    String query = "sourceId=\"1*\" AND selected=\"all\"";
    String expected = String.format("(sourceId=\"1*\") AND (selectedBy =/@selected=all %s)", ISIL);
    String result = cut.translateQuery(query, ISIL);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedSome() {
    String query = "sourceId=\"1*\" AND selected=\"some\"";
    String expected = String.format("(sourceId=\"1*\") AND (selectedBy =/@selected=some %s)", ISIL);
    String result = cut.translateQuery(query, ISIL);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedNone() {
    String query = "sourceId=\"1*\" AND selected=\"none\"";
    String expected = String.format("(sourceId=\"1*\") AND (selectedBy =/@selected=none %s)", ISIL);
    String result = cut.translateQuery(query, ISIL);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedAllWithSortBy() {
    String query = "sourceId=\"1*\" AND selected=\"all\" sortby label";
    String expected =
        String.format("(sourceId=\"1*\") AND (selectedBy =/@selected=all %s) sortby label", ISIL);
    String result = cut.translateQuery(query, ISIL);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedAllWithSortBy2() {
    String query = "(sourceId=\"1*\" AND selected=\"all\") sortby label";
    String expected =
        String.format("(sourceId=\"1*\") AND (selectedBy =/@selected=all %s) sortby label", ISIL);
    String result = cut.translateQuery(query, ISIL);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedAllWithStatus() {
    String query = "sourceId=\"1*\" AND selected=\"all\" AND status=(\"active\")";
    String expected =
        String.format(
            "(sourceId=\"1*\") AND (status=(\"active\")) AND (selectedBy =/@selected=all %s)",
            ISIL);
    String result = cut.translateQuery(query, ISIL);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedAllWithStatusAndSortBy() {
    String query = "sourceId=\"1*\" AND selected=\"all\" AND status=(\"active\") sortby label";
    String expected =
        String.format(
            "(sourceId=\"1*\") AND (status=(\"active\")) AND (selectedBy =/@selected=all %s) sortby label",
            ISIL);
    String result = cut.translateQuery(query, ISIL);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedSomeAndNone() {
    String query = "sourceId=\"1*\" AND selected=(\"none\" OR \"some\")";
    String expected =
        String.format(
            "(sourceId=\"1*\") AND ((selectedBy =/@selected=none %s) OR (selectedBy =/@selected=some %s))",
            ISIL, ISIL);
    String result = cut.translateQuery(query, ISIL);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedSomeAndNoneWithSortBy() {
    String query = "sourceId=\"1*\" AND selected=(\"none\" OR \"some\") sortby label";
    String expected =
        String.format(
            "(sourceId=\"1*\") AND ((selectedBy =/@selected=none %s) OR (selectedBy =/@selected=some %s)) sortby label",
            ISIL, ISIL);
    String result = cut.translateQuery(query, ISIL);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedAllAndSomeAndNone() {
    String query = "sourceId=\"1*\" AND selected=(\"none\" OR \"some\" OR \"all\")";
    String expected =
        String.format(
            "(sourceId=\"1*\") AND ((selectedBy =/@selected=none %s) OR (selectedBy =/@selected=some %s) OR (selectedBy =/@selected=all %s))",
            ISIL, ISIL, ISIL);
    String result = cut.translateQuery(query, ISIL);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedAllAndSomeAndNoneWithSortBy() {
    String query = "sourceId=\"1*\" AND selected=(\"none\" OR \"some\" OR \"all\") sortby label";
    String expected =
        String.format(
            "(sourceId=\"1*\") AND ((selectedBy =/@selected=none %s) OR (selectedBy =/@selected=some %s) OR (selectedBy =/@selected=all %s)) sortby label",
            ISIL, ISIL, ISIL);
    String result = cut.translateQuery(query, ISIL);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedAllAndSomeAndNoneWithStatusAndSortBy() {
    String query =
        "sourceId=\"1*\" AND selected=(\"none\" OR \"some\" OR \"all\") AND status=\"active\" sortby label";
    String expected =
        String.format(
            "(sourceId=\"1*\") AND (status=\"active\") AND ((selectedBy =/@selected=none %s) OR (selectedBy =/@selected=some %s) OR (selectedBy =/@selected=all %s)) sortby label",
            ISIL, ISIL, ISIL);
    String result = cut.translateQuery(query, ISIL);
    assertEquals(expected, result);
  }
}
