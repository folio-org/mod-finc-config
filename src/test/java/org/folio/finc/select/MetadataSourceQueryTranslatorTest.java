package org.folio.finc.select;

import org.folio.finc.select.query.MetadataSourcesQueryTranslator;
import org.folio.finc.select.query.QueryTranslator;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(value = Parameterized.class)
public class MetadataSourceQueryTranslatorTest {

  private static final String ISIL = "ISIL-01";

  private static QueryTranslator cut;

  @BeforeClass
  public static void setUp() {
    cut = new MetadataSourcesQueryTranslator();
  }

  private String query;
  private String expected;

  public MetadataSourceQueryTranslatorTest(String query, String expected) {
    this.query = query;
    this.expected = expected;
  }

  @Parameterized.Parameters(name = "{index}: translateQuery({0}) = {1}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {
            "sourceId=\"1*\" AND selected=\"all\"",
            String.format("(sourceId=\"1*\") AND (selectedBy =/@selected=all %s)", ISIL)
          },
          {
            "sourceId=\"1*\" AND selected=\"some\"",
            String.format("(sourceId=\"1*\") AND (selectedBy =/@selected=some %s)", ISIL)
          },
          {
            "sourceId=\"1*\" AND selected=\"none\"",
            String.format("(sourceId=\"1*\") AND (selectedBy =/@selected=none %s)", ISIL)
          },
          {
            "sourceId=\"1*\" AND selected=\"all\" sortby label",
            String.format(
                "(sourceId=\"1*\") AND (selectedBy =/@selected=all %s) sortby label", ISIL)
          },
          {
            "(sourceId=\"1*\" AND selected=\"all\") sortby label",
            String.format(
                "(sourceId=\"1*\") AND (selectedBy =/@selected=all %s) sortby label", ISIL)
          },
          {
            "sourceId=\"1*\" AND selected=\"all\" AND status=(\"active\")",
            String.format(
                "(sourceId=\"1*\") AND (status=(\"active\")) AND (selectedBy =/@selected=all %s)",
                ISIL)
          },
          {
            "sourceId=\"1*\" AND selected=\"all\" AND status=(\"active\") sortby label",
            String.format(
                "(sourceId=\"1*\") AND (status=(\"active\")) AND (selectedBy =/@selected=all %s) sortby label",
                ISIL)
          },
          {
            "sourceId=\"1*\" AND selected=(\"none\" OR \"some\")",
            String.format(
                "(sourceId=\"1*\") AND ((selectedBy =/@selected=none %s) OR (selectedBy =/@selected=some %s))",
                ISIL, ISIL)
          },
          {
            "sourceId=\"1*\" AND selected=(\"none\" OR \"some\") sortby label",
            String.format(
                "(sourceId=\"1*\") AND ((selectedBy =/@selected=none %s) OR (selectedBy =/@selected=some %s)) sortby label",
                ISIL, ISIL)
          },
          {
            "sourceId=\"1*\" AND selected=(\"none\" OR \"some\" OR \"all\")",
            String.format(
                "(sourceId=\"1*\") AND ((selectedBy =/@selected=none %s) OR (selectedBy =/@selected=some %s) OR (selectedBy =/@selected=all %s))",
                ISIL, ISIL, ISIL)
          },
          {
            "sourceId=\"1*\" AND selected=(\"none\" OR \"some\" OR \"all\") sortby label",
            String.format(
                "(sourceId=\"1*\") AND ((selectedBy =/@selected=none %s) OR (selectedBy =/@selected=some %s) OR (selectedBy =/@selected=all %s)) sortby label",
                ISIL, ISIL, ISIL)
          },
          {
            "sourceId=\"1*\" AND selected=(\"none\" OR \"some\" OR \"all\") AND status=\"active\" sortby label",
            String.format(
                "(sourceId=\"1*\") AND (status=\"active\") AND ((selectedBy =/@selected=none %s) OR (selectedBy =/@selected=some %s) OR (selectedBy =/@selected=all %s)) sortby label",
                ISIL, ISIL, ISIL)
          },
        });
  }

  @Test
  public void testTranslate() {
    String result = cut.translateQuery(query, ISIL);
    assertEquals(expected, result);
  }
}
