/*
 * Copyright © WebServices pour l'Éducation, 2014
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
