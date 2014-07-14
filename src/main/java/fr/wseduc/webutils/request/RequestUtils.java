package fr.wseduc.webutils.request;

import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.validation.JsonSchemaValidator;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

public class RequestUtils {

	private static final Logger log = LoggerFactory.getLogger(RequestUtils.class);
	private static final JsonSchemaValidator validator = JsonSchemaValidator.getInstance();

	public static void bodyToJson(final HttpServerRequest request, final Handler<JsonObject> handler) {
		request.bodyHandler(new Handler<Buffer>() {
			@Override
			public void handle(Buffer event) {
				try {
					JsonObject json = new JsonObject(event.toString("UTF-8"));
					handler.handle(json);
				} catch (RuntimeException e) {
					log.warn(e.getMessage(), e);
					Renders.badRequest(request, e.getMessage());
				}
			}
		});
	}

	public static void bodyToJson(final HttpServerRequest request, final String schema,
			final Handler<JsonObject> handler) {
		request.bodyHandler(new Handler<Buffer>() {
			@Override
			public void handle(Buffer event) {
				try {
					final JsonObject json = new JsonObject(event.toString("UTF-8"));
					validator.validate(schema, json, new AsyncResultHandler<Message<JsonObject>>() {
						@Override
						public void handle(AsyncResult<Message<JsonObject>> event) {
							if (event.succeeded()) {
								if ("ok".equals(event.result().body().getString("status"))) {
									handler.handle(json);
								} else {
									log.debug(event.result().body().getString("message"));
									log.debug(event.result().body()
											.getArray("report", new JsonArray()).encodePrettily());
									Renders.badRequest(request, event.result().body().getString("error"));
								}
							} else {
								log.error("Validate async error.", event.cause());
								Renders.badRequest(request, event.cause().getMessage());
							}
						}
					});
				} catch (RuntimeException e) {
					log.warn(e.getMessage(), e);
					Renders.badRequest(request, e.getMessage());
				}
			}
		});
	}

}
