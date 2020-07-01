package org.folio.finc;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertTrue;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.finc.rules.EmbeddedPostgresRule;
import org.folio.rest.RestVerticle;
import org.folio.rest.tools.utils.NetworkUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.listeners.SchedulerListenerSupport;

@RunWith(VertxUnitRunner.class)
public class PostDeployImplITest {

  private static final String TENANT = "finc";
  private static Vertx vertx;

  @ClassRule
  public static WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

  @ClassRule
  public static EmbeddedPostgresRule pgRule = new EmbeddedPostgresRule(TENANT);

  private static final DeploymentOptions options = new DeploymentOptions();

  @BeforeClass
  public static void beforeClass() {
    vertx = Vertx.vertx();

    int port = NetworkUtils.nextFreePort();
    options.setConfig(
        new JsonObject()
            .put("http.port", port)
            .put("okapiUrl", "http://localhost:" + wireMockRule.port())
            .put("tenantsPath", "/_/proxy/tenants")
            .put("testing", true));

    JsonArray tenantsResponseBody =
        new JsonArray()
            .add(new JsonObject().put("id", TENANT));
    stubFor(
        get(urlEqualTo("/_/proxy/tenants"))
            .willReturn(aResponse().withBody(tenantsResponseBody.encodePrettily())));
  }

  @Test
  public void testThatPostDeployImplIsSchedulingJobs(TestContext context)
      throws SchedulerException {
    Async async = context.async(1);

    Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
    scheduler.getListenerManager().addSchedulerListener(new SchedulerListenerAsyncCountdown(async));
    options.getConfig().put("testing", false);
    vertx.deployVerticle(
        RestVerticle.class.getName(),
        options,
        ar -> {
          if (ar.succeeded()) {
            async.countDown();
          } else {
            context.fail(ar.cause());
          }
        });

    async.awaitSuccess(5000);
    assertTrue(scheduler.checkExists(new JobKey("harvest-ezb-files-job")));
  }

  private static class SchedulerListenerAsyncCountdown extends SchedulerListenerSupport {

    private final Async async;

    @Override
    public void jobAdded(JobDetail jobDetail) {
      async.countDown();
    }

    public SchedulerListenerAsyncCountdown(Async async) {
      this.async = async;
    }
  }
}
