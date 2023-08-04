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
import static java.util.Collections.emptySet;
import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RequestUtils {

	private static final Logger log = LoggerFactory.getLogger(RequestUtils.class);
	private static final JsonSchemaValidator validator = JsonSchemaValidator.getInstance();
	private static final Pattern versionPatter = Pattern.compile("version=([0-9]+\\.[0-9]+)");
	public static final Pattern REGEXP_AUTHORIZATION = Pattern.compile("^\\s*(OAuth|Bearer)\\s+([^\\s\\,]*)");
	/** Default date and time format for incoming requests.*/
	private static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("HHmm-ddMMyyyy");

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
		return bodyToClass(request, clazz, null);
	}

	/**
	 *
	 * @param request Http request
	 * @param clazz Desired class of the body
	 * @param onEmptyBody Supplier of a default value when the content of the body is empty
	 * @return {@code null} if the body is null, value supplied by {@code onEmptyBody} if the body is empty, otherwise
	 * the deserialized value of the body
	 * @param <T> Desired class
	 */
	public static <T> Future<T> bodyToClass(final HttpServerRequest request, final Class<T> clazz, final Supplier<T> onEmptyBody) {
		final Promise<T> promise = Promise.promise();
		request.bodyHandler(new Handler<Buffer>() {
			@Override
			public void handle(Buffer event) {
				try {
					final T body;
					if(event == null) {
						body = null;
					} else {
						final String content = event.toString("UTF-8");
						if(content == null) {
							body = null;
						} else if(isEmpty(content) && onEmptyBody != null) {
							body = onEmptyBody.get();
						} else {
							String obj = XSSUtils.stripXSS(content);
							body = Json.decodeValue(obj, clazz);
						}
					}
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

	/**
	 * Split the value of {@code paramName} GET param by {@code ,} and returns the unique values
	 * @param paramName Name of the GET parameter
	 * @param request Http request whose GET parameters we want to extract
	 * @return The set of comma-separated values specified in {@code paramName} GET param
	 */
	public static Set<String> getParamAsSet(final String paramName, final HttpServerRequest request) {
		return getParamAsSet(paramName, ",", request);
	}

	/**
	 * Split the value of {@code paramName} GET param by {@code separator} and returns the unique values
	 * @param paramName Name of the GET parameter
	 * @param separator Separator to use between the values
	 * @param request Http request whose GET parameters we want to extract
	 * @return The set of values specified in {@code paramName} GET param
	 */
	public static Set<String> getParamAsSet(final String paramName, final String separator, final HttpServerRequest request) {
		final Set<String> values;
		final String paramValue = request.getParam(paramName);
		if(isEmpty(paramValue)) {
			values = emptySet();
		} else {
			values = Arrays.stream(paramValue.split(","))
					.filter(StringUtils::isNotEmpty)
					.collect(Collectors.toSet());
		}
		return values;
	}


	/**
	 * Parses the date contained in a GET param with {@link RequestUtils#DEFAULT_DATE_FORMAT}.
	 * @param paramName Name of the GET param containing the date
	 * @param format Expected format of the date
	 * @param request Http request
	 * @return the parsed date or {@code empty} if the request parameter was not present in the request or if its value
	 * was empty
	 * @throws IllegalArgumentException if the {@code paramName} contained a value that did not respect {@code format}
	 */
	public static Optional<Date> getDateParam(final String paramName, final HttpServerRequest request) {
		return getDateParam(paramName, DEFAULT_DATE_FORMAT, request);
	}

	/**
	 * @param paramName Name of the GET param containing the date
	 * @param format Expected format of the date
	 * @param request Http request
	 * @return the parsed date or {@code empty} if the request parameter was not present in the request or if its value
	 * was empty
	 * @throws IllegalArgumentException if the {@code paramName} contained a value that did not respect {@code format}
	 */
	public static Optional<Date> getDateParam(final String paramName, final String format, final HttpServerRequest request) {
		return getDateParam(paramName, isEmpty(format) ? DEFAULT_DATE_FORMAT : new SimpleDateFormat(format), request);
	}

	/**
	 * @param paramName Name of the GET param containing the date
	 * @param format Expected format of the date
	 * @param request Http request
	 * @return the parsed date or {@code empty} if the request parameter was not present in the request or if its value
	 * was empty
	 * @throws IllegalArgumentException if the {@code paramName} contained a value that did not respect {@code format}
	 */
	public static Optional<Date> getDateParam(final String paramName, final SimpleDateFormat format, final HttpServerRequest request) {
		final Optional<Date> resultDate;
		final String paramValue = request.getParam(paramName);
		if(isEmpty(paramValue)) {
			resultDate = Optional.empty();
		} else {
			try {
				resultDate = Optional.ofNullable(format.parse(paramValue));
			} catch (ParseException e) {
				throw new IllegalArgumentException(paramName + " could not be parsed with format " + format.toPattern(), e);
			}
		}
		return resultDate;
	}

}
