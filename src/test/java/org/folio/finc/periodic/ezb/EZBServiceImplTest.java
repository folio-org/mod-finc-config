package org.folio.finc.periodic.ezb;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class EZBServiceImplTest {

  private static final String USER = "user";
  private static final String PASS = "pass";
  private static final String CONTENT = "content";
  private static final String ISIL = "isil1";
  private static EZBServiceImpl ezbService;
  private static final ProxySelector defaultProxySelector = ProxySelector.getDefault();

  @Rule public WireMockRule wmRule = new WireMockRule(new WireMockConfiguration().dynamicPort());
  @Rule public WireMockRule proxyRule = new WireMockRule(new WireMockConfiguration().dynamicPort());

  @Before
  public void setUp() {
    ProxySelector.setDefault(defaultProxySelector);
    ezbService = new EZBServiceImpl(wmRule.url("%s"));
  }

  @Test
  public void testFetchEZBFileWith200(TestContext context) {
    wmRule.stubFor(
        get(urlEqualTo("/" + ISIL)).willReturn(aResponse().withStatus(200).withBody(CONTENT)));
    ezbService
        .fetchEZBFile(USER, PASS, ISIL, Vertx.vertx())
        .onComplete(
            context.asyncAssertSuccess(
                s -> {
                  wmRule.verify(
                      1,
                      getRequestedFor(urlEqualTo("/" + ISIL))
                          .withBasicAuth(new BasicCredentials(USER, PASS)));
                  assertThat(s).isEqualTo(CONTENT);
                }));
  }

  @Test
  public void testFetchEZBFileWith404(TestContext context) {
    wmRule.stubFor(get(urlEqualTo("/" + ISIL)).willReturn(aResponse().withStatus(404)));
    ezbService
        .fetchEZBFile(USER, PASS, ISIL, Vertx.vertx())
        .onComplete(
            context.asyncAssertFailure(
                t -> {
                  wmRule.verify(
                      1,
                      getRequestedFor(urlEqualTo("/" + ISIL))
                          .withBasicAuth(new BasicCredentials(USER, PASS)));
                  assertThat(t).hasMessageContaining("Failed to fetch ezb file. Status code: 404");
                }));
  }

  @Test
  public void testFetchEZBFileWithError(TestContext context) {
    wmRule.stop();
    ezbService
        .fetchEZBFile(USER, PASS, ISIL, Vertx.vertx())
        .onComplete(
            context.asyncAssertFailure(
                t -> assertThat(t).hasMessageContaining("Failed to fetch ezb file.")));
  }

  @Test
  public void testFetchEZBFileWithProxy(TestContext context) {
    ProxySelector.setDefault(
        new ProxySelector() {
          @Override
          public List<Proxy> select(URI uri) {
            return Collections.singletonList(
                new Proxy(Type.HTTP, new InetSocketAddress("localhost", proxyRule.port())));
          }

          @Override
          public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {}
        });

    wmRule.stubFor(
        get(urlEqualTo("/" + ISIL)).willReturn(aResponse().withStatus(200).withBody(CONTENT)));
    proxyRule.stubFor(get(anyUrl()).willReturn(aResponse().proxiedFrom(wmRule.baseUrl())));
    ezbService
        .fetchEZBFile(USER, PASS, ISIL, Vertx.vertx())
        .onComplete(
            context.asyncAssertSuccess(
                s -> {
                  proxyRule.verify(
                      1,
                      getRequestedFor(urlEqualTo("/" + ISIL))
                          .withBasicAuth(new BasicCredentials(USER, PASS)));
                  wmRule.verify(
                      1,
                      getRequestedFor(urlEqualTo("/" + ISIL))
                          .withBasicAuth(new BasicCredentials(USER, PASS)));
                  assertThat(s).isEqualTo(CONTENT);
                }));
  }
}
