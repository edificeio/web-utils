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

import com.fasterxml.jackson.core.type.TypeReference;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.security.XSSUtils;
import fr.wseduc.webutils.validation.JsonSchemaValidator;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestUtils {

	private static final Logger log = LoggerFactory.getLogger(RequestUtils.class);
	private static final JsonSchemaValidator validator = JsonSchemaValidator.getInstance();
	private static final Pattern versionPatter = Pattern.compile("version=([0-9]+\\.[0-9]+)");
	public static final Pattern REGEXP_AUTHORIZATION = Pattern.compile("^\\s*(OAuth|Bearer)\\s+([^\\s\\,]*)");

	private static void resumeQuietly(final HttpServerRequest request){
		try{
			//resume if body has been paused elsewhere
			request.resume();
		}catch(Exception e){}
	}

	public static void bodyToJson(final HttpServerRequest request, final Handler<JsonObject> handler) {
		request.bodyHandler(new Handler<Buffer>() {
			@Override
			public void handle(Buffer event) {
				try {
					JsonObject json = new fr.wseduc.webutils.collections.JsonObject(XSSUtils.stripXSS(event.toString("UTF-8")));
					handler.handle(json);
				} catch (RuntimeException e) {
					log.warn(e.getMessage(), e);
					Renders.badRequest(request, e.getMessage());
				}
			}
		});
		resumeQuietly(request);
	}

	public static void bodyToJsonArray(final HttpServerRequest request, final Handler<JsonArray> handler) {
		request.bodyHandler(new Handler<Buffer>() {
			@Override
			public void handle(Buffer event) {
				try {
					String obj = XSSUtils.stripXSS(event.toString("UTF-8"));
					JsonArray json = new fr.wseduc.webutils.collections.JsonArray(obj);
					handler.handle(json);
				} catch (RuntimeException e) {
					log.warn(e.getMessage(), e);
					Renders.badRequest(request, e.getMessage());
				}
			}
		});
		resumeQuietly(request);
	}

	public static <T> Future<T> bodyToClass(final HttpServerRequest request, final Class<T> clazz) {
		final Promise<T> promise = Promise.promise();
		request.bodyHandler(new Handler<Buffer>() {
			@Override
			public void handle(Buffer event) {
				try {

					String obj = XSSUtils.stripXSS(event.toString("UTF-8"));
					final T body = Json.decodeValue(obj, clazz);
					promise.complete(body);
				} catch (RuntimeException e) {
					log.warn(e.getMessage(), e);
					promise.fail(e);
					Renders.badRequest(request, e.getMessage());
				}
			}
		});
		return promise.future();
	}

	public static <T> Future<T> bodyToClass(final HttpServerRequest request, final TypeReference<T> typeReference) {
		final Promise<T> promise = Promise.promise();
		request.bodyHandler(new Handler<Buffer>() {
			@Override
			public void handle(Buffer event) {
				try {

					String obj = XSSUtils.stripXSS(event.toString("UTF-8"));
					final T body = Json.decodeValue(obj, typeReference);
					promise.complete(body);
				} catch (RuntimeException e) {
					log.warn(e.getMessage(), e);
					promise.fail(e);
					Renders.badRequest(request, e.getMessage());
				}
			}
		});
		return promise.future();
	}

	public static void bodyToJson(final HttpServerRequest request, final String schema,
			final Handler<JsonObject> handler) {
		request.bodyHandler(new Handler<Buffer>() {
			@Override
			public void handle(Buffer event) {
				try {
					final JsonObject json = new fr.wseduc.webutils.collections.JsonObject(XSSUtils.stripXSS(event.toString("UTF-8")));
					validator.validate(schema, json, event1 -> {
						if (event1.succeeded()) {
							if ("ok".equals(event1.result().body().getString("status"))) {
								handler.handle(json);
							} else {
								final String message = event1.result().body().getString("message");
								log.debug(message);
								log.debug(event1.result().body()
										.getJsonArray("report", new JsonArray()).encodePrettily());
								Renders.badRequest(request, event1.result().body().getString("error", message));
							}
						} else {
							log.error("Validate async error.", event1.cause());
							Renders.badRequest(request, event1.cause().getMessage());
						}
					});
				} catch (RuntimeException e) {
					log.warn(e.getMessage(), e);
					Renders.badRequest(request, e.getMessage());
				}
			}
		});
		resumeQuietly(request);
	}

	public static String acceptVersion(HttpServerRequest request) {
		final String accept = request.headers().get("Accept");
		return getAcceptVersion(accept);
	}

	public static String getAcceptVersion(String accept) {
		Matcher m;
		if (accept != null && (m = versionPatter.matcher(accept)).find()) {
			return m.group(1);
		}
		return "";
	}

	public static Integer getIntParam(final String name, final HttpServerRequest request) {
		final String paramValue = request.getParam(name);
		if(paramValue == null) {
			return null;
		}
		return Integer.parseInt(paramValue);
	}

	public static List<String> getListOfStringsParam(final String name, final HttpServerRequest request) {
		final String paramValue = request.getParam(name);
		if(paramValue == null) {
			return null;
		}
		return Arrays.asList(paramValue.split(","));
	}

	/**
	 * @param request Incoming user request
	 * @return The content of user-agent header of the request or {@code null} if
	 * there are no headers or if the header is not set
	 */
	public static String getUserAgent(final HttpServerRequest request) {
		final String ua;
		if(request == null || request.headers() == null) {
			ua = null;
		} else {
			final MultiMap headers = request.headers();
			ua = headers.get("User-Agent");
		}
		return ua;
	}

	/**
	 * @param request Incoming user request
	 * @return The value of the token in the authorization header
	 */
	public static Optional<String> getTokenHeader(final HttpServerRequest request) {
		//get from header
		final String header = request.getHeader("Authorization");
		if (header != null && Pattern.matches("^\\s*(OAuth|Bearer)(.*)$", header)) {
			final Matcher matcher = REGEXP_AUTHORIZATION.matcher(header);
			if (!matcher.find()) {
				return Optional.empty();
			} else {
				final String token = matcher.group(2);
				return Optional.ofNullable(token);
			}
		} else {
			return Optional.empty();
		}
	}

}
