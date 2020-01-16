package org.folio.finc.select;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MetadataSourceQueryTranslatorTest {

  private static final String ISIL = "ISIL-01";

  @Test
  public void translateSelectedAll() {
    String query = "sourceId=\"1*\" AND selected=\"all\"";
    String expected =
        "(sourceId=\"1*\") AND (selectedBy == \"*\\\"isil\\\": \\\""
            + ISIL
            + "\\\", \\\"selected\\\": \\\"all\\\"*\")";
    String result = MetadataSourcesQueryTranslator.translate(query, ISIL);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedSome() {
    String query = "sourceId=\"1*\" AND selected=\"some\"";
    String expected =
        "(sourceId=\"1*\") AND (selectedBy == \"*\\\"isil\\\": \\\""
            + ISIL
            + "\\\", \\\"selected\\\": \\\"some\\\"*\")";
    String result = MetadataSourcesQueryTranslator.translate(query, ISIL);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedNone() {
    String query = "sourceId=\"1*\" AND selected=\"none\"";
    String expected =
        "(sourceId=\"1*\") AND (selectedBy == \"*\\\"isil\\\": \\\""
            + ISIL
            + "\\\", \\\"selected\\\": \\\"none\\\"*\")";
    String result = MetadataSourcesQueryTranslator.translate(query, ISIL);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedAllWithSortBy() {
    String query = "sourceId=\"1*\" AND selected=\"all\" sortby label";
    String expected =
        "(sourceId=\"1*\") AND (selectedBy == \"*\\\"isil\\\": \\\""
            + ISIL
            + "\\\", \\\"selected\\\": \\\"all\\\"*\") sortby label";
    String result = MetadataSourcesQueryTranslator.translate(query, ISIL);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedAllWithSortBy2() {
    String query = "(sourceId=\"1*\" AND selected=\"all\") sortby label";
    String expected =
        "(sourceId=\"1*\") AND (selectedBy == \"*\\\"isil\\\": \\\""
            + ISIL
            + "\\\", \\\"selected\\\": \\\"all\\\"*\") sortby label";
    String result = MetadataSourcesQueryTranslator.translate(query, ISIL);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedAllWithStatus() {
    String query = "sourceId=\"1*\" AND selected=\"all\" AND status=(\"active\")";
    String expected =
        "(sourceId=\"1*\") AND (status=(\"active\")) AND (selectedBy == \"*\\\"isil\\\": \\\""
            + ISIL
            + "\\\", \\\"selected\\\": \\\"all\\\"*\")";
    String result = MetadataSourcesQueryTranslator.translate(query, ISIL);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedAllWithStatusAndSortBy() {
    String query = "sourceId=\"1*\" AND selected=\"all\" AND status=(\"active\") sortby label";
    String expected =
        "(sourceId=\"1*\") AND (status=(\"active\")) AND (selectedBy == \"*\\\"isil\\\": \\\""
            + ISIL
            + "\\\", \\\"selected\\\": \\\"all\\\"*\") sortby label";
    String result = MetadataSourcesQueryTranslator.translate(query, ISIL);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedSomeAndNone() {
    String query = "sourceId=\"1*\" AND selected=(\"none\" OR \"some\")";
    String expected =
        "(sourceId=\"1*\") AND ((selectedBy == \"*\\\"isil\\\": \\\""
            + ISIL
            + "\\\", \\\"selected\\\": \\\"none\\\"*\") OR (selectedBy == \"*\\\"isil\\\": \\\""
            + ISIL
            + "\\\", \\\"selected\\\": \\\"some\\\"*\"))";
    String result = MetadataSourcesQueryTranslator.translate(query, ISIL);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedSomeAndNoneWithSortBy() {
    String query = "sourceId=\"1*\" AND selected=(\"none\" OR \"some\") sortby label";
    String expected =
        "(sourceId=\"1*\") AND ((selectedBy == \"*\\\"isil\\\": \\\""
            + ISIL
            + "\\\", \\\"selected\\\": \\\"none\\\"*\") OR (selectedBy == \"*\\\"isil\\\": \\\""
            + ISIL
            + "\\\", \\\"selected\\\": \\\"some\\\"*\")) sortby label";
    String result = MetadataSourcesQueryTranslator.translate(query, ISIL);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedAllAndSomeAndNone() {
    String query = "sourceId=\"1*\" AND selected=(\"none\" OR \"some\" OR \"all\")";
    String expected =
        "(sourceId=\"1*\") AND ((selectedBy == \"*\\\"isil\\\": \\\""
            + ISIL
            + "\\\", \\\"selected\\\": \\\"none\\\"*\") OR (selectedBy == \"*\\\"isil\\\": \\\""
            + ISIL
            + "\\\", \\\"selected\\\": \\\"some\\\"*\") OR (selectedBy == \"*\\\"isil\\\": \\\""
            + ISIL
            + "\\\", \\\"selected\\\": \\\"all\\\"*\"))";
    String result = MetadataSourcesQueryTranslator.translate(query, ISIL);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedAllAndSomeAndNoneWithSortBy() {
    String query = "sourceId=\"1*\" AND selected=(\"none\" OR \"some\" OR \"all\") sortby label";
    String expected =
        "(sourceId=\"1*\") AND ((selectedBy == \"*\\\"isil\\\": \\\""
            + ISIL
            + "\\\", \\\"selected\\\": \\\"none\\\"*\") OR (selectedBy == \"*\\\"isil\\\": \\\""
            + ISIL
            + "\\\", \\\"selected\\\": \\\"some\\\"*\") OR (selectedBy == \"*\\\"isil\\\": \\\""
            + ISIL
            + "\\\", \\\"selected\\\": \\\"all\\\"*\")) sortby label";
    String result = MetadataSourcesQueryTranslator.translate(query, ISIL);
    assertEquals(expected, result);
  }

  @Test
  public void translateSelectedAllAndSomeAndNoneWithStatusAndSortBy() {
    String query =
        "sourceId=\"1*\" AND selected=(\"none\" OR \"some\" OR \"all\") AND status=\"active\" sortby label";
    String expected =
        "(sourceId=\"1*\") AND (status=\"active\") AND ((selectedBy == \"*\\\"isil\\\": \\\""
            + ISIL
            + "\\\", \\\"selected\\\": \\\"none\\\"*\") OR (selectedBy == \"*\\\"isil\\\": \\\""
            + ISIL
            + "\\\", \\\"selected\\\": \\\"some\\\"*\") OR (selectedBy == \"*\\\"isil\\\": \\\""
            + ISIL
            + "\\\", \\\"selected\\\": \\\"all\\\"*\")) sortby label";
    String result = MetadataSourcesQueryTranslator.translate(query, ISIL);
    assertEquals(expected, result);
  }
}
