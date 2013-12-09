package edu.one.core.infra.request;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

public class RequestUtils {

	public static void bodyToJson(final HttpServerRequest request, final Handler<JsonObject> handler) {
		request.bodyHandler(new Handler<Buffer>() {
			@Override
			public void handle(Buffer event) {
				try {
					JsonObject json = new JsonObject(event.toString("UTF-8"));
					handler.handle(json);
				} catch (RuntimeException e) {
					request.response().setStatusCode(400).end(e.getMessage());
				}
			}
		});
	}

}
