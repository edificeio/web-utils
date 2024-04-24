package fr.wseduc.webutils.security.oauth;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;

import org.junit.Test;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.cert.X509Certificate;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DefaultOAuthResourceProviderTest {
  final DefaultOAuthResourceProvider provider = new DefaultOAuthResourceProvider(null);
  @Test
  public void testHasBearerHeader() {
    assertFalse(
      "A request without Authorization header should not be considered as a request with a bearer",
      provider.hasBearerHeader(new DummyRequest()));
    assertFalse(
      "A request with an unknown Authorization header should not be considered as a request with a bearer",
      provider.hasBearerHeader(new DummyRequest("Basic yoyo")));
    assertTrue(
      "A request with an Authorization header with a bearer should be OK",
      provider.hasBearerHeader(new DummyRequest("Bearer my-token")));
    assertTrue(
      "A request with an Authorization header with multiple values with a Bearer inside should be OK",
      provider.hasBearerHeader(new DummyRequest("Basic tata, Bearer my-token")));
    assertTrue(
      "A request with an Authorization header with multiple values with a Bearer inside should be OK",
      provider.hasBearerHeader(new DummyRequest("Bearer my-token, Basic tata")));
  }



  private static class DummyRequest implements HttpServerRequest {
    private final MultiMap headers;

    private DummyRequest(String auth) {
      this.headers = MultiMap.caseInsensitiveMultiMap();
      this.headers.add("Authorization", auth);
    }
    private DummyRequest() {
      this.headers = MultiMap.caseInsensitiveMultiMap();
    }
    @Override
    public String absoluteURI() {
            throw new UnsupportedOperationException("Unimplemented method 'absoluteURI'");
    }
    @Override
    public long bytesRead() {
            throw new UnsupportedOperationException("Unimplemented method 'bytesRead'");
    }
    @Override
    public HttpConnection connection() {
            throw new UnsupportedOperationException("Unimplemented method 'connection'");
    }
    @Override
    public int cookieCount() {
            throw new UnsupportedOperationException("Unimplemented method 'cookieCount'");
    }
    @Override
    public Map<String, Cookie> cookieMap() {
            throw new UnsupportedOperationException("Unimplemented method 'cookieMap'");
    }
    @Override
    public HttpServerRequest customFrameHandler(Handler<HttpFrame> arg0) {
            throw new UnsupportedOperationException("Unimplemented method 'customFrameHandler'");
    }
    @Override
    public HttpServerRequest endHandler(Handler<Void> arg0) {
            throw new UnsupportedOperationException("Unimplemented method 'endHandler'");
    }
    @Override
    public HttpServerRequest exceptionHandler(Handler<Throwable> arg0) {
            throw new UnsupportedOperationException("Unimplemented method 'exceptionHandler'");
    }
    @Override
    public HttpServerRequest fetch(long arg0) {
            throw new UnsupportedOperationException("Unimplemented method 'fetch'");
    }
    @Override
    public MultiMap formAttributes() {
            throw new UnsupportedOperationException("Unimplemented method 'formAttributes'");
    }
    @Override
    public Cookie getCookie(String arg0) {
            throw new UnsupportedOperationException("Unimplemented method 'getCookie'");
    }
    @Override
    public String getFormAttribute(String arg0) {
            throw new UnsupportedOperationException("Unimplemented method 'getFormAttribute'");
    }
    @Override
    public String getHeader(String arg0) {
            throw new UnsupportedOperationException("Unimplemented method 'getHeader'");
    }
    @Override
    public String getHeader(CharSequence arg0) {
            throw new UnsupportedOperationException("Unimplemented method 'getHeader'");
    }
    @Override
    public String getParam(String arg0) {
            throw new UnsupportedOperationException("Unimplemented method 'getParam'");
    }
    @Override
    public HttpServerRequest handler(Handler<Buffer> arg0) {
            throw new UnsupportedOperationException("Unimplemented method 'handler'");
    }
    @Override
    public MultiMap headers() {
            return this.headers;
    }
    @Override
    public String host() {
            throw new UnsupportedOperationException("Unimplemented method 'host'");
    }
    @Override
    public boolean isEnded() {
            throw new UnsupportedOperationException("Unimplemented method 'isEnded'");
    }
    @Override
    public boolean isExpectMultipart() {
            throw new UnsupportedOperationException("Unimplemented method 'isExpectMultipart'");
    }
    @Override
    public boolean isSSL() {
            throw new UnsupportedOperationException("Unimplemented method 'isSSL'");
    }
    @Override
    public SocketAddress localAddress() {
            throw new UnsupportedOperationException("Unimplemented method 'localAddress'");
    }
    @Override
    public HttpMethod method() {
            throw new UnsupportedOperationException("Unimplemented method 'method'");
    }
    @Override
    public NetSocket netSocket() {
            throw new UnsupportedOperationException("Unimplemented method 'netSocket'");
    }
    @Override
    public MultiMap params() {
            throw new UnsupportedOperationException("Unimplemented method 'params'");
    }
    @Override
    public String path() {
            throw new UnsupportedOperationException("Unimplemented method 'path'");
    }
    @Override
    public HttpServerRequest pause() {
            throw new UnsupportedOperationException("Unimplemented method 'pause'");
    }
    @Override
    public X509Certificate[] peerCertificateChain() throws SSLPeerUnverifiedException {
            throw new UnsupportedOperationException("Unimplemented method 'peerCertificateChain'");
    }
    @Override
    public String query() {
            throw new UnsupportedOperationException("Unimplemented method 'query'");
    }
    @Override
    public String rawMethod() {
            throw new UnsupportedOperationException("Unimplemented method 'rawMethod'");
    }
    @Override
    public SocketAddress remoteAddress() {
            throw new UnsupportedOperationException("Unimplemented method 'remoteAddress'");
    }
    @Override
    public HttpServerResponse response() {
            throw new UnsupportedOperationException("Unimplemented method 'response'");
    }
    @Override
    public HttpServerRequest resume() {
            throw new UnsupportedOperationException("Unimplemented method 'resume'");
    }
    @Override
    public String scheme() {
            throw new UnsupportedOperationException("Unimplemented method 'scheme'");
    }
    @Override
    public HttpServerRequest setExpectMultipart(boolean arg0) {
            throw new UnsupportedOperationException("Unimplemented method 'setExpectMultipart'");
    }
    @Override
    public SSLSession sslSession() {
            throw new UnsupportedOperationException("Unimplemented method 'sslSession'");
    }
    @Override
    public HttpServerRequest streamPriorityHandler(Handler<StreamPriority> arg0) {
            throw new UnsupportedOperationException("Unimplemented method 'streamPriorityHandler'");
    }
    @Override
    public ServerWebSocket upgrade() {
            throw new UnsupportedOperationException("Unimplemented method 'upgrade'");
    }
    @Override
    public HttpServerRequest uploadHandler(Handler<HttpServerFileUpload> arg0) {
            throw new UnsupportedOperationException("Unimplemented method 'uploadHandler'");
    }
    @Override
    public String uri() {
            throw new UnsupportedOperationException("Unimplemented method 'uri'");
    }
    @Override
    public HttpVersion version() {
            throw new UnsupportedOperationException("Unimplemented method 'version'");
    }


  }
}