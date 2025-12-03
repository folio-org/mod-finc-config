package org.folio.rest.utils;

public class Constants {

  public static final String ENV_EZB_DOWNLOAD_URL = "EZB_DOWNLOAD_URL";
  public static final String MODULE_TENANT = "finc";
  public static final String QUARTZ_EZB_JOB_KEY = "harvest-ezb-files-job";
  public static final String QUARTZ_EZB_TRIGGER_KEY = "harvest-ezb-files-trigger";
  public static final String STREAM_ABORT = "streamed_abort";
  public static final String STREAM_COMPLETE = "complete";
  public static final String STREAM_ID = "streamed_id";

  /** Bytes per megabyte */
  public static final long BYTES_PER_MB = 1024L * 1024L;

  /** Maximum file upload size in bytes (50 MB) */
  public static final long MAX_UPLOAD_FILE_SIZE = 50L * BYTES_PER_MB;

  /** Maximum file upload size in MB */
  public static final long MAX_UPLOAD_FILE_SIZE_MB = 50L;

  private Constants() {
    throw new IllegalStateException("Utility class");
  }
}
