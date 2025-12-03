package org.folio.rest.impl;

import java.io.IOException;

/** Exception thrown when uploaded file exceeds maximum allowed size */
public class FileSizeExceededException extends IOException {

  public FileSizeExceededException(String message) {
    super(message);
  }
}
