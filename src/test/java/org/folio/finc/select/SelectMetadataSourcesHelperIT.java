package org.folio.finc.select;

import static org.folio.okapi.common.XOkapiHeaders.TENANT;

import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.HashMap;
import java.util.Map;
import org.folio.rest.jaxrs.model.Select;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class SelectMetadataSourcesHelperIT extends MetadataSourceVerticleTestBase {

  private static final SelectMetadataSourcesHelper cut =
      new SelectMetadataSourcesHelper(vertx, TENANT_UBL);
  @Rule public Timeout timeout = Timeout.seconds(10);

  @Test
  public void testSuccessfulSelect(TestContext context) {

    Select select = new Select().withSelect(true);

    Map<String, String> header = new HashMap<>();
    header.put(TENANT, TENANT_UBL);
    header.put(CONTENT_TYPE, APPLICATION_JSON);

    // TODO: Fix test, missing assertion
    cut.selectAllCollectionsOfMetadataSource(
        metadataSource2.getId(),
        select,
        header,
        context.asyncAssertSuccess(),
        vertx.getOrCreateContext());
  }
}
