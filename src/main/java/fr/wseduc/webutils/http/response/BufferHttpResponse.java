package fr.wseduc.webutils.http.response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpFrame;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.Cookie;
import io.vertx.core.net.HostAndPort;

import java.util.Set;

public class BufferHttpResponse implements HttpServerResponse {
    final private Buffer buffer = Buffer.buffer();
    final private HttpServerResponse original;

    public BufferHttpResponse(HttpServerResponse original) {
        this.original = original;
    }

    public Buffer getBuffer() {
        return buffer;
    }

    @Override
    public Future<Void> write(Buffer data) {
        buffer.appendBuffer(data);
        return original.write(data);
    }

    @Override
    public Future<Void> end(Buffer buffer) {
        buffer.appendBuffer(buffer);
        return original.end(buffer);
    }

    @Override
    public HttpServerResponse exceptionHandler(Handler<Throwable> handler) {
        return original.exceptionHandler(handler);
    }

    @Override
    public HttpServerResponse setWriteQueueMaxSize(int maxSize) {
        return original.setWriteQueueMaxSize(maxSize);
    }

    @Override
    public HttpServerResponse drainHandler(Handler<Void> handler) {
        return original.drainHandler(handler);
    }

    @Override
    public int getStatusCode() {
        return original.getStatusCode();
    }

    @Override
    public HttpServerResponse setStatusCode(int statusCode) {
        return original.setStatusCode(statusCode);
    }

    @Override
    public String getStatusMessage() {
        return original.getStatusMessage();
    }

    @Override
    public HttpServerResponse setStatusMessage(String statusMessage) {
        return original.setStatusMessage(statusMessage);
    }

    @Override
    public HttpServerResponse setChunked(boolean chunked) {
        return original.setChunked(chunked);
    }

    @Override
    public boolean isChunked() {
        return original.isChunked();
    }

    @Override
    public MultiMap headers() {
        return original.headers();
    }

    @Override
    public HttpServerResponse putHeader(String name, String value) {
        return original.putHeader(name, value);
    }

    @Override
    public HttpServerResponse putHeader(CharSequence name, CharSequence value) {
        return original.putHeader(name, value);
    }

    @Override
    public HttpServerResponse putHeader(String name, Iterable<String> values) {
        return original.putHeader(name, values);
    }

    @Override
    public HttpServerResponse putHeader(CharSequence name, Iterable<CharSequence> values) {
        return original.putHeader(name, values);
    }

    @Override
    public MultiMap trailers() {
        return original.trailers();
    }

    @Override
    public HttpServerResponse putTrailer(String name, String value) {
        return original.putTrailer(name, value);
    }

    @Override
    public HttpServerResponse putTrailer(CharSequence name, CharSequence value) {
        return original.putTrailer(name, value);
    }

    @Override
    public HttpServerResponse putTrailer(String name, Iterable<String> values) {
        return original.putTrailer(name, values);
    }

    @Override
    public HttpServerResponse putTrailer(CharSequence name, Iterable<CharSequence> value) {
        return original.putTrailer(name, value);
    }

    @Override
    public HttpServerResponse closeHandler(Handler<Void> handler) {
        return original.closeHandler(handler);
    }

    @Override
    
    public HttpServerResponse endHandler(Handler<Void> handler) {
        return original.endHandler(handler);
    }

    @Override
    
    public Future<Void> write(String chunk, String enc) {
        buffer.appendBuffer(Buffer.buffer(chunk, enc));
        return original.write(chunk, enc);
    }

    @Override
    
    public Future<Void> write(String chunk) {
        buffer.appendBuffer(Buffer.buffer(chunk));
        return original.write(chunk);
    }

    @Override
    
    public HttpServerResponse writeContinue() {
        return original.writeContinue();
    }

    @Override
    public Future<Void> writeEarlyHints(MultiMap headers) {
        return original.writeEarlyHints(headers);
    }

    @Override
    public void writeEarlyHints(MultiMap headers, Handler<AsyncResult<Void>> handler) {
        original.writeEarlyHints(headers, handler);
    }

    @Override
    public Future<Void> end(String chunk) {
        buffer.appendBuffer(Buffer.buffer(chunk));
        return original.end(chunk);
    }

    @Override
    public Future<Void> end(String chunk, String enc) {
        buffer.appendBuffer(Buffer.buffer(chunk, enc));
        return original.end(chunk, enc);
    }

    @Override
    public Future<Void> end() {
        return original.end();
    }

    @Override
    
    public Future<Void> sendFile(String filename) {
        return original.sendFile(filename);
    }

    @Override
    
    public Future<Void> sendFile(String filename, long offset) {
        return original.sendFile(filename, offset);
    }

    @Override
    
    public Future<Void> sendFile(String filename, long offset, long length) {
        return original.sendFile(filename, offset, length);
    }

    @Override
    
    public HttpServerResponse sendFile(String filename, Handler<AsyncResult<Void>> resultHandler) {
        return original.sendFile(filename, resultHandler);
    }

    @Override
    
    public HttpServerResponse sendFile(String filename, long offset, Handler<AsyncResult<Void>> resultHandler) {
        return original.sendFile(filename, offset, resultHandler);
    }

    @Override
    
    public HttpServerResponse sendFile(String filename, long offset, long length, Handler<AsyncResult<Void>> resultHandler) {
        return original.sendFile(filename, offset, length, resultHandler);
    }

    @Override
    public void close() {
        original.close();
    }

    @Override
    public boolean ended() {
        return original.ended();
    }

    @Override
    public boolean closed() {
        return original.closed();
    }

    @Override
    public boolean headWritten() {
        return original.headWritten();
    }

    @Override
    
    public HttpServerResponse headersEndHandler(Handler<Void> handler) {
        return original.headersEndHandler(handler);
    }

    @Override
    
    public HttpServerResponse bodyEndHandler(Handler<Void> handler) {
        return original.bodyEndHandler(handler);
    }

    @Override
    public long bytesWritten() {
        return original.bytesWritten();
    }

    @Override
    public int streamId() {
        return original.streamId();
    }

    @Override
    public HttpServerResponse push(HttpMethod method, String host, String path, Handler<AsyncResult<HttpServerResponse>> handler) {
        return original.push(method, host, path, handler);
    }

    @Override
    public HttpServerResponse push(HttpMethod method, String path, MultiMap headers, Handler<AsyncResult<HttpServerResponse>> handler) {
        return original.push(method, path, headers, handler);
    }

    @Override
    
    public HttpServerResponse push(HttpMethod method, String path, Handler<AsyncResult<HttpServerResponse>> handler) {
        return original.push(method, path, handler);
    }

    @Override
    
    public HttpServerResponse push(HttpMethod method, String host, String path, MultiMap headers, Handler<AsyncResult<HttpServerResponse>> handler) {
        return original.push(method, host, path, headers, handler);
    }

    @Override
    public Future<HttpServerResponse> push(HttpMethod method, HostAndPort authority, String path, MultiMap headers) {
        return original.push(method, authority, path, headers);
    }

    @Override
    public Future<HttpServerResponse> push(HttpMethod method, String host, String path, MultiMap headers) {
        return original.push(method, host, path, headers);
    }

    @Override
    public boolean reset() {
        return original.reset();
    }

    @Override
    public boolean reset(long code) {
        return original.reset(code);
    }

    @Override
    public HttpServerResponse writeCustomFrame(int type, int flags, Buffer payload) {
        return original.writeCustomFrame(type, flags, payload);
    }

    @Override
    public HttpServerResponse writeCustomFrame(HttpFrame frame) {
        return original.writeCustomFrame(frame);
    }

    @Override
    public boolean writeQueueFull() {
        return original.writeQueueFull();
    }

	@Override
	public void end(Handler<AsyncResult<Void>> handler)
	{
		original.end(handler);
	}

	@Override
	public void end(Buffer buff, Handler<AsyncResult<Void>> handler)
	{
		original.end(buff, handler);
	}

	@Override
	public void end(String str, Handler<AsyncResult<Void>> handler)
	{
		original.end(str, handler);
	}

	@Override
	public void end(String str, String str2, Handler<AsyncResult<Void>> handler)
	{
		original.end(str, str2, handler);
	}

	@Override
	public void write(Buffer buff, Handler<AsyncResult<Void>> handler) {
        original.write(buff, handler);
	}

	@Override
	public void write(String str, Handler<AsyncResult<Void>> handler) {
        original.write(str, handler);
	}

	@Override
	public void write(String str, String str2, Handler<AsyncResult<Void>> handler) {
        original.write(str, str2, handler);
	}

	@Override
	public HttpServerResponse addCookie(Cookie cookie)
	{
		return original.addCookie(cookie);
	}

	@Override
	public Cookie removeCookie(String str, boolean bool)
	{
		return original.removeCookie(str, bool);
	}

    @Override
    public Set<Cookie> removeCookies(String name, boolean invalidate) {
        return original.removeCookies(name, invalidate);
    }

    @Override
    public @io.vertx.codegen.annotations.Nullable Cookie removeCookie(String name, String domain, String path, boolean invalidate) {
        return original.removeCookie(name, domain, path, invalidate);
    }
}
