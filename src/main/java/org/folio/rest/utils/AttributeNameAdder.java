package org.folio.rest.utils;

import io.vertx.core.Context;
import io.vertx.core.Future;
import java.util.Map;
import java.util.Objects;
import org.folio.rest.jaxrs.model.FincConfigMetadataSource;
import org.folio.rest.utils.nameresolver.OrgaizationNameResolver;

public class AttributeNameAdder {

  private AttributeNameAdder() {
    throw new IllegalStateException("Utility class.");
  }

  public static Future<FincConfigMetadataSource> resolveAndAddAttributeNames(
      FincConfigMetadataSource metadataSource,
      Map<String, String> okapiHeaders,
      Context vertxContext) {

    String organizationId = getOrganizationId(metadataSource);
    if (Objects.isNull(organizationId)) return Future.succeededFuture(metadataSource);

    Future<String> orgaNameFuture =
        OrgaizationNameResolver.resolveName(organizationId, okapiHeaders, vertxContext);
    return orgaNameFuture.map(
        name -> {
          if (Objects.nonNull(name)) {
            metadataSource.getOrganization().setName(name);
          }
          return metadataSource;
        });
  }

  private static String getOrganizationId(FincConfigMetadataSource metadataSource) {
    if (metadataSource.getOrganization() != null
        && metadataSource.getOrganization().getId() != null) {
      return metadataSource.getOrganization().getId();
    }
    return null;
  }
}
