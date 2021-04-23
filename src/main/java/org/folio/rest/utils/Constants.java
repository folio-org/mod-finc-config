package org.folio.rest.utils;

public class Constants {

  public static final String MODULE_TENANT = "finc";
  public static final String STREAM_ID = "streamed_id";
  public static final String STREAM_COMPLETE = "complete";
  public static final String STREAM_ABORT = "streamed_abort";

  private Constants() {
    throw new IllegalStateException("Utility class");
  }
}
