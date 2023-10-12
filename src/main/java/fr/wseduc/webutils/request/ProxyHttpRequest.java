package fr.wseduc.webutils.request;

import java.util.Map;
import java.util.Set;

import io.netty.handler.codec.DecoderResult;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.net.HostAndPort;
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
    public @Nullable HostAndPort authority() {
        return original.authority();
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
    public HttpServerRequest setParamsCharset(String charset) {
        return original.setParamsCharset(charset);
    }

    @Override
    public String getParamsCharset() {
        return original.getParamsCharset();
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
    public Future<Buffer> body() {
        return original.body();
    }

    @Override
    public Future<Void> end() {
        return original.end();
    }

    @Override
    public Future<NetSocket> toNetSocket() {
        return original.toNetSocket();
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
    public Future<ServerWebSocket> toWebSocket() {
        return original.toWebSocket();
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

	@Override
	public HttpServerRequest streamPriorityHandler(Handler<StreamPriority> handler)
	{
		return original.streamPriorityHandler(handler);
	}

    @Override
    public DecoderResult decoderResult() {
        return original.decoderResult();
    }

    @Override
	public long bytesRead()
	{
		return original.bytesRead();
	}

	@Override
	public HttpServerRequest fetch(long bytes)
	{
		return original.fetch(bytes);
	}

	@Override
	public Map<String, Cookie> cookieMap()
	{
		return original.cookieMap();
	}

    @Override
    public Set<Cookie> cookies(String name) {
        return original.cookies(name);
    }

    @Override
    public Set<Cookie> cookies() {
        return cookies();
    }

    @Override
	public int cookieCount()
	{
		return original.cookieCount();
	}

	@Override
	public Cookie getCookie(String str)
	{
		return original.getCookie(str);
	}

    @Override
    public @Nullable Cookie getCookie(String name, String domain, String path) {
        return original.getCookie(name, domain, path);
    }
}
