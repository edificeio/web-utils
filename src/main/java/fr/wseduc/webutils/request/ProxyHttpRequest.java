package fr.wseduc.webutils.request;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.cert.X509Certificate;

public class ProxyHttpRequest implements HttpServerRequest {
    private  final HttpServerRequest original;
    private final HttpServerResponse response;
    public ProxyHttpRequest(HttpServerRequest original, HttpServerResponse response){
        this.original = original;
        this.response = response;
    }

    public HttpServerRequest getOriginal() {
        return original;
    }

    @Override
    public HttpServerRequest exceptionHandler(Handler<Throwable> handler) {
        return original.exceptionHandler(handler);
    }

    @Override
    public HttpServerRequest handler(Handler<Buffer> handler) {
        return original.handler(handler);
    }

    @Override
    public HttpServerRequest pause() {
        return original.pause();
    }

    @Override
    public HttpServerRequest resume() {
        return original.resume();
    }

    @Override
    public HttpServerRequest endHandler(Handler<Void> endHandler) {
        return original.endHandler(endHandler);
    }

    @Override
    public HttpVersion version() {
        return original.version();
    }

    @Override
    public HttpMethod method() {
        return original.method();
    }

    @Override
    public String rawMethod() {
        return original.rawMethod();
    }

    @Override
    public boolean isSSL() {
        return original.isSSL();
    }

    @Override
    
    public String scheme() {
        return original.scheme();
    }

    @Override
    public String uri() {
        return original.uri();
    }

    @Override
    
    public String path() {
        return original.path();
    }

    @Override
    
    public String query() {
        return original.query();
    }

    @Override
    
    public String host() {
        return original.host();
    }

    @Override
    
    public HttpServerResponse response() {
        return response;
    }

    @Override
    
    public MultiMap headers() {
        return original.headers();
    }

    @Override
    
    public String getHeader(String headerName) {
        return original.getHeader(headerName);
    }

    @Override
    
    public String getHeader(CharSequence headerName) {
        return original.getHeader(headerName);
    }

    @Override
    
    public MultiMap params() {
        return original.params();
    }

    @Override
    
    public String getParam(String paramName) {
        return original.getParam(paramName);
    }

    @Override
    
    public SocketAddress remoteAddress() {
        return original.remoteAddress();
    }

    @Override
    
    public SocketAddress localAddress() {
        return original.localAddress();
    }

    @Override
    
    public SSLSession sslSession() {
        return original.sslSession();
    }

    @Override
    
    public X509Certificate[] peerCertificateChain() throws SSLPeerUnverifiedException {
        return original.peerCertificateChain();
    }

    @Override
    public String absoluteURI() {
        return original.absoluteURI();
    }

    @Override
    
    public HttpServerRequest bodyHandler( Handler<Buffer> bodyHandler) {
        return original.bodyHandler(bodyHandler);
    }

    @Override
    
    public NetSocket netSocket() {
        return original.netSocket();
    }

    @Override
    
    public HttpServerRequest setExpectMultipart(boolean expect) {
        return original.setExpectMultipart(expect);
    }

    @Override
    public boolean isExpectMultipart() {
        return original.isExpectMultipart();
    }

    @Override
    
    public HttpServerRequest uploadHandler( Handler<HttpServerFileUpload> uploadHandler) {
        return original.uploadHandler(uploadHandler);
    }

    @Override
    
    public MultiMap formAttributes() {
        return original.formAttributes();
    }

    @Override
    
    public String getFormAttribute(String attributeName) {
        return original.getFormAttribute(attributeName);
    }

    @Override
    public ServerWebSocket upgrade() {
        return original.upgrade();
    }

    @Override
    public boolean isEnded() {
        return original.isEnded();
    }

    @Override
    
    public HttpServerRequest customFrameHandler(Handler<HttpFrame> handler) {
        return original.customFrameHandler(handler);
    }

    @Override
    
    public HttpConnection connection() {
        return original.connection();
    }
}
