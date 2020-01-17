package fr.wseduc.webutils.request;

import io.vertx.core.buffer.Buffer;

import java.util.Optional;

public interface HttpServerRequestWithBuffering {
    Optional<Buffer> getBodyResponseBuffered();
    HttpServerRequestWithBuffering enableResponseBuffering();
    HttpServerRequestWithBuffering disableResponseBuffering();
}
