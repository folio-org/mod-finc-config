package org.folio.rest.impl;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static com.google.common.net.MediaType.PLAIN_TEXT_UTF_8;

import javax.ws.rs.core.Response;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollection;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollectionWithFilters;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollectionWithFiltersCollection;
import org.folio.rest.jaxrs.model.FincConfigMetadataCollections;
import org.folio.rest.jaxrs.resource.support.ResponseDelegate;

public class GetFincConfigMetadataCollectionsResponse extends ResponseDelegate {

  static final String JSON = JSON_UTF_8.withoutParameters().toString();
  static final String PLAIN_TEXT = PLAIN_TEXT_UTF_8.withoutParameters().toString();

  protected GetFincConfigMetadataCollectionsResponse(Response delegate, Object entity) {
    super(delegate, entity);
  }

  protected GetFincConfigMetadataCollectionsResponse(Response delegate) {
    super(delegate);
  }

  public static GetFincConfigMetadataCollectionsResponse respond200WithApplicationJson(
      FincConfigMetadataCollectionWithFiltersCollection entity) {
    Response.ResponseBuilder responseBuilder = Response.status(200).header(CONTENT_TYPE, JSON);
    responseBuilder.entity(entity);
    return new GetFincConfigMetadataCollectionsResponse(responseBuilder.build(), entity);
  }

  public static GetFincConfigMetadataCollectionsResponse respond200WithApplicationJson(
      FincConfigMetadataCollections entity) {
    Response.ResponseBuilder responseBuilder = Response.status(200).header(CONTENT_TYPE, JSON);
    responseBuilder.entity(entity);
    return new GetFincConfigMetadataCollectionsResponse(responseBuilder.build(), entity);
  }

  public static GetFincConfigMetadataCollectionsResponse respond200WithApplicationJson(
      FincConfigMetadataCollection entity) {
    Response.ResponseBuilder responseBuilder = Response.status(200).header(CONTENT_TYPE, JSON);
    responseBuilder.entity(entity);
    return new GetFincConfigMetadataCollectionsResponse(responseBuilder.build(), entity);
  }

  public static GetFincConfigMetadataCollectionsResponse respond200WithApplicationJson(
      FincConfigMetadataCollectionWithFilters entity) {
    Response.ResponseBuilder responseBuilder = Response.status(200).header(CONTENT_TYPE, JSON);
    responseBuilder.entity(entity);
    return new GetFincConfigMetadataCollectionsResponse(responseBuilder.build(), entity);
  }

  public static GetFincConfigMetadataCollectionsResponse respond400WithTextPlain(Object entity) {
    Response.ResponseBuilder responseBuilder =
        Response.status(400).header(CONTENT_TYPE, PLAIN_TEXT);
    responseBuilder.entity(entity);
    return new GetFincConfigMetadataCollectionsResponse(responseBuilder.build(), entity);
  }

  public static GetFincConfigMetadataCollectionsResponse respond401WithTextPlain(Object entity) {
    Response.ResponseBuilder responseBuilder =
        Response.status(401).header(CONTENT_TYPE, PLAIN_TEXT);
    responseBuilder.entity(entity);
    return new GetFincConfigMetadataCollectionsResponse(responseBuilder.build(), entity);
  }

  public static GetFincConfigMetadataCollectionsResponse respond404WithTextPlain(Object entity) {
    Response.ResponseBuilder responseBuilder =
        Response.status(404).header(CONTENT_TYPE, PLAIN_TEXT);
    responseBuilder.entity(entity);
    return new GetFincConfigMetadataCollectionsResponse(responseBuilder.build(), entity);
  }

  public static GetFincConfigMetadataCollectionsResponse respond500WithTextPlain(Object entity) {
    Response.ResponseBuilder responseBuilder =
        Response.status(500).header(CONTENT_TYPE, PLAIN_TEXT);
    responseBuilder.entity(entity);
    return new GetFincConfigMetadataCollectionsResponse(responseBuilder.build(), entity);
  }
}
