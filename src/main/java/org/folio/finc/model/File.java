package org.folio.finc.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import javax.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"id", "isil", "data"})
public class File {

  /** (Required) */
  @JsonProperty("id")
  @NotNull
  private String id;

  /** Isil of library using this filter */
  @JsonProperty("isil")
  @JsonPropertyDescription("Isil of library using this filter")
  private String isil;

  /** Isil of library using this filter */
  @JsonProperty("data")
  @JsonPropertyDescription("The stored data")
  private String data;

  /** (Required) */
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  /** (Required) */
  @JsonProperty("id")
  public void setId(String id) {
    this.id = id;
  }

  public File withId(String id) {
    this.id = id;
    return this;
  }

  /** Isil of library using this filter */
  @JsonProperty("isil")
  public String getIsil() {
    return isil;
  }

  /** Isil of library using this filter */
  @JsonProperty("isil")
  public void setIsil(String isil) {
    this.isil = isil;
  }

  public File withIsil(String isil) {
    this.isil = isil;
    return this;
  }

  /** The data */
  @JsonProperty("data")
  public String getData() {
    return data;
  }

  /** Isil of library using this filter */
  @JsonProperty("data")
  public void setData(String data) {
    this.data = data;
  }

  public File withData(String data) {
    this.data = data;
    return this;
  }
}
