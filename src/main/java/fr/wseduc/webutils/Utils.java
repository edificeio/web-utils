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

package fr.wseduc.webutils;

import fr.wseduc.webutils.eventbus.ResultMessage;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Consumer;

public class Utils {

	public static <T> T getOrElse(T value, T defaultValue) {
		return getOrElse(value, defaultValue, true);
	}

	public static <T> T getOrElse(T value, T defaultValue, boolean allowEmpty) {
		if (value != null && (allowEmpty || !value.toString().trim().isEmpty())) {
			return value;
		}
		return defaultValue;
	}

	public static String inputStreamToString(InputStream in) {
		Scanner scanner = new Scanner(in, "UTF-8");
		String content = scanner.useDelimiter("\\A").next();
		scanner.close();
		return content;
	}

	public static JsonObject loadFromResource(String resource) {
		String src = new Scanner(Utils.class.getClassLoader()
				.getResourceAsStream(resource), "UTF-8")
				.useDelimiter("\\A").next();
		return new JsonObject(src);
	}

	public static JsonObject validAndGet(JsonObject json, List<String> fields,
				List<String> requiredFields) {
		if (json != null) {
			JsonObject e = json.copy();
			for (String attr: json.fieldNames()) {
				if (!fields.contains(attr) || e.getValue(attr) == null) {
					e.remove(attr);
				}
			}
			if (e.getMap().keySet().containsAll(requiredFields)) {
				return e;
			}
		}
		return null;
	}

	public static Either<String, JsonObject> validResult(Message<JsonObject> res) {
		if ("ok".equals(res.body().getString("status"))) {
			JsonObject r = res.body().getJsonObject("result");
			if (r == null) {
				r = res.body();
				r.remove("status");
			}
			return new Either.Right<>(r);
		} else {
			return new Either.Left<>(res.body().getString("message", ""));
		}
	}

	public static Either<String, JsonArray> validResults(Message<JsonObject> res) {
		if ("ok".equals(res.body().getString("status"))) {
			return new Either.Right<>(res.body().getJsonArray("results", new JsonArray()));
		} else {
			return new Either.Left<>(res.body().getString("message", ""));
		}
	}

	// TODO improve with type and validation
	public static JsonObject jsonFromMultimap(MultiMap attributes) {
		JsonObject json = new JsonObject();
		if (attributes != null) {
			for (Map.Entry<String, String> e: attributes.entries()) {
				json.put(e.getKey(), e.getValue());
			}
		}
		return json;
	}

	public static <T> boolean defaultValidationError(JsonObject c,
			Handler<Either<String, T>> result, String ... params) {
		return validationError(c, result, "invalid.field", params);
	}

	public static <T> boolean validationError(JsonObject c,
			Handler<Either<String, T>> result, String errorMessage, String ... params) {
		if (c == null) {
			result.handle(new Either.Left<String, T>(errorMessage));
			return true;
		}
		return validationParamsError(result, errorMessage, params);
	}

	public static <T> boolean defaultValidationParamsError(Handler<Either<String, T>> result, String ... params) {
		return validationParamsError(result, "invalid.parameter", params);
	}

	public static <T> boolean validationParamsError(Handler<Either<String, T>> result,
			String errorMessage, String ... params) {
		if (params.length > 0) {
			for (String s : params) {
				if (s == null) {
					result.handle(new Either.Left<String, T>(errorMessage));
					return true;
				}
			}
		}
		return false;
	}

	public static <T> boolean defaultValidationParamsNull(
			Handler<Either<String, T>> result, Object ... params) {
		return validationParamsNull(result, "null.parameter", params);
	}

	public static <T> boolean validationParamsNull(Handler<Either<String, T>> result,
			String errorMessage, Object ... params) {
		if (params.length > 0) {
			for (Object s : params) {
				if (s == null) {
					result.handle(new Either.Left<String, T>(errorMessage));
					return true;
				}
			}
		}
		return false;
	}

	public static <T extends Enum<T>> T stringToEnum(String name, T defaultValue, Class<T> type) {
		if (name != null) {
			try {
				return Enum.valueOf(type, name);
			} catch (IllegalArgumentException | NullPointerException e) {
				return defaultValue;
			}
		}
		return defaultValue;
	}

	public static JsonArray flatten(JsonArray array) {
		JsonArray a = new JsonArray();
		if (array != null) {
			for (Object o : array) {
				if (o instanceof JsonArray) {
					JsonArray r = flatten((JsonArray) o);
					for (Object object : r) {
						a.add(object);
					}
				} else {
					a.add(o);
				}
			}
		}
		return a;
	}

	public static boolean isEmpty(String string) {
		return string == null || string.isEmpty();
	}

	public static boolean isNotEmpty(String string) {
		return !isEmpty(string);
	}

	public static Handler<AsyncResult<Message<JsonObject>>> handlerToAsyncHandler(Handler<Message<JsonObject>> handler) {
		return event -> {
			if (event.succeeded()) {
				handler.handle(event.result());
			} else {
				handler.handle(new ResultMessage().error(event.cause().getMessage()));
			}
		};
	}

	/**
	 * Bridges a legacy callback-based service method (using {@link Either}) into a Vert.x {@link Future}.
	 * <p>
	 * Many service methods in this codebase accept a {@code Handler<Either<String, T>>} as their last
	 * argument, where a {@link Either.Left} signals failure (with an error message) and a
	 * {@link Either.Right} signals success (with the result value). This utility wraps such a call
	 * into a {@link Future} so it can be composed with other async operations.
	 *
	 * <pre>{@code
	 * // Given a service method:
	 * //   void findUser(String id, Handler<Either<String, JsonObject>> handler)
	 *
	 * Utils.eitherToFuture(handler -> userService.findUser(userId, handler))
	 *     .compose(user -> Utils.eitherToFuture(handler -> roleService.getRoles(user.getString("id"), handler)))
	 *     .onSuccess(roles -> renderJson(request, roles))
	 *     .onFailure(err -> renderError(request, err));
	 * }</pre>
	 *
	 * @param call a consumer that invokes the legacy service method, passing along the provided handler
	 * @param <T>  the type of the success value
	 * @return a {@link Future} that completes with the right value on success, or fails with the left message on error
	 */
	public static <T> Future<T> eitherToFuture(Consumer<Handler<Either<String, T>>> call) {
		Promise<T> promise = Promise.promise();
		call.accept(res -> {
			if (res.isRight()) promise.complete(res.right().getValue());
			else promise.fail(res.left().getValue());
		});
		return promise.future();
	}

}
