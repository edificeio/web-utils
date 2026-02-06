package fr.wseduc.webutils.request;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;

public interface IAccessLogger {

    String UNAUTHENTICATED_USER_ID = "unauthenticated";
    String NO_SESSION_COOKIE = "nocookie";
    String NO_TOKEN_ID = "notoken";
    String LOG_FORMAT_CONF_KEY = "ACCESS_LOG_FORMAT";

    void log(HttpServerRequest request, Handler<Void> handler);
}
