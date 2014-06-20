package fr.wseduc.webutils;

import java.io.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.http.HttpMethod;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.filter.SecurityHandler;
import fr.wseduc.webutils.security.ActionType;
import fr.wseduc.webutils.security.SecuredAction;

public abstract class Controller extends Renders {

	private static final MethodHandles.Lookup lookup = MethodHandles.publicLookup();
	protected RouteMatcher rm;
	private final Map<String, Set<Binding>> uriBinding;
	protected Map<String, SecuredAction> securedActions;
	protected EventBus eb;

	public Controller(Vertx vertx, Container container, RouteMatcher rm,
			Map<String, SecuredAction> securedActions) {
		super(vertx, container);
		this.rm = rm;
		this.uriBinding = new HashMap<>();
		this.securedActions = securedActions;
		if (vertx != null) {
			this.eb = Server.getEventBus(vertx);
		}
		if (rm != null) {
			loadRoutes();
		}
	}

	protected void loadRoutes() {
		InputStream is = Controller.class.getClassLoader().getResourceAsStream(
				this.getClass().getName() + ".json");
		if (is != null) {
			BufferedReader r = null;
			try {
				r = new BufferedReader(new InputStreamReader(is, "UTF-8"));
				String line;
				while((line = r.readLine()) != null) {
					JsonObject route = new JsonObject(line);
					String httpMethod = route.getString("httpMethod");
					String method = route.getString("method");
					String path = route.getString("path");
					if (httpMethod == null || path == null || method == null ||
							httpMethod.trim().isEmpty() || method.trim().isEmpty()) {
						continue;
					}
					boolean regex = route.getBoolean("regex", false);
					switch (httpMethod) {
						case "POST":
							if (regex) {
								postWithRegEx(path, method);
							} else {
								post(path, method);
							}
							break;
						case "GET":
							if (regex) {
								getWithRegEx(path, method);
							} else {
								get(path, method);
							}
							break;
						case "DELETE":
							if (regex) {
								deleteWithRegEx(path, method);
							} else {
								delete(path, method);
							}
							break;
						case "PUT":
							if (regex) {
								putWithRegEx(path, method);
							} else {
								put(path, method);
							}
							break;
						case "BUS":
							registerMethod(path, method);
							break;
					}
				}
			} catch (IOException | NoSuchMethodException | IllegalAccessException e) {
				log.error("Unable to load routes in controller " + this.getClass().getName(), e);
			} finally {
				if (r != null) {
					try {
						r.close();
					} catch (IOException e) {
						log.error("Close inputstream error", e);
					}
				}
			}
		} else {
			log.warn("Not found routes file to controller " + this.getClass().getName());
		}
	}

	private Handler<HttpServerRequest> execute(final String method) {
		try {
			final MethodHandle mh = lookup.bind(this, method,
					MethodType.methodType(void.class, HttpServerRequest.class));
			return new Handler<HttpServerRequest>() {

				@Override
				public void handle(HttpServerRequest request) {
					try {
						mh.invokeExact(request);
					} catch (Throwable e) {
						log.error("Error invoking secured method : " + method, e);
						request.response().setStatusCode(500).end();
					}
				}
			};
		} catch (NoSuchMethodException | IllegalAccessException e) {

			return new Handler<HttpServerRequest>() {

				@Override
				public void handle(HttpServerRequest request) {
					log.error("Error mapping method : " + method, e);
					request.response().setStatusCode(404).end();
				}
			};
		}
	}

	private Handler<HttpServerRequest> executeSecure(final String method) {
		try {
			final MethodHandle mh = lookup.bind(this, method,
					MethodType.methodType(void.class, HttpServerRequest.class));
			return new SecurityHandler() {

				@Override
				public void filter(HttpServerRequest request) {
					try {
						mh.invokeExact(request);
					} catch (Throwable e) {
						log.error("Error invoking secured method : " + method, e);
						request.response().setStatusCode(500).end();
					}
				}
			};
		} catch (NoSuchMethodException | IllegalAccessException e) {

			return new SecurityHandler() {

				@Override
				public void filter(HttpServerRequest request) {
					log.error("Error mapping secured method : " + method, e);
					request.response().setStatusCode(404).end();
				}
			};
		}
	}

	public void registerMethod(String address, String method)
			throws NoSuchMethodException, IllegalAccessException {
		final MethodHandle mh = lookup.bind(this, method,
				MethodType.methodType(void.class, Message.class));
		Server.getEventBus(vertx).registerHandler(address, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> message) {
				try {
					mh.invokeExact(message);
				} catch (Throwable e) {
					container.logger().error(e.getMessage(), e);
					JsonObject json = new JsonObject().putString("status", "error")
							.putString("message", e.getMessage());
					message.reply(json);
				}
			}
		});
	}

	private Handler<HttpServerRequest> bindHandler(String method) {
		if (method == null || method.trim().isEmpty()) {
			throw new NullPointerException();
		}
		if (securedActions.containsKey(this.getClass().getName() + "|" + method)) {
			return executeSecure(method);
		}
		return execute(method);
	}

	public Map<String, Set<Binding>> getUriBinding() {
		return this.uriBinding;
	}

	public Map<String, Set<Binding>> getSecuredUriBinding() {
		Map<String, Set<Binding>> bindings = new HashMap<>();
		for (Entry<String, Set<Binding>> e : this.uriBinding.entrySet()) {
			if (securedActions.containsKey(e.getKey())) {
				bindings.put(e.getKey(), e.getValue());
			}
		}
		return bindings;
	}

	public Set<Binding> securedUriBinding() {
		Set<Binding> bindings = new HashSet<>();
		for (Entry<String, Set<Binding>> e : this.uriBinding.entrySet()) {
			if (securedActions.containsKey(e.getKey())) {
				bindings.addAll(e.getValue());
			}
		}
		return bindings;
	}

	public Controller get(String pattern, String method) {
		pattern = addPathPrefix(pattern);
		addPattern(pattern, HttpMethod.GET, method);
		rm.get(pattern, bindHandler(method));
		return this;
	}

	public Controller put(String pattern, String method) {
		pattern = addPathPrefix(pattern);
		addPattern(pattern, HttpMethod.PUT, method);
		rm.put(pattern, bindHandler(method));
		return this;
	}

	public Controller post(String pattern, String method) {
		pattern = addPathPrefix(pattern);
		addPattern(pattern, HttpMethod.POST, method);
		rm.post(pattern, bindHandler(method));
		return this;
	}

	public Controller delete(String pattern, String method) {
		pattern = addPathPrefix(pattern);
		addPattern(pattern, HttpMethod.DELETE, method);
		rm.delete(pattern, bindHandler(method));
		return this;
	}

	public Controller getWithRegEx(String regex, String method) {
		regex = addPathPrefix(regex).replaceAll("\\/", "\\/");
		addRegEx(regex, HttpMethod.GET, method);
		rm.getWithRegEx(regex, bindHandler(method));
		return this;
	}

	public Controller putWithRegEx(String regex, String method) {
		regex = addPathPrefix(regex).replaceAll("\\/", "\\/");
		addRegEx(regex, HttpMethod.PUT, method);
		rm.putWithRegEx(regex, bindHandler(method));
		return this;
	}

	public Controller postWithRegEx(String regex, String method) {
		regex = addPathPrefix(regex).replaceAll("\\/", "\\/");
		addRegEx(regex, HttpMethod.POST, method);
		rm.postWithRegEx(regex, bindHandler(method));
		return this;
	}

	public Controller deleteWithRegEx(String regex, String method) {
		regex = addPathPrefix(regex).replaceAll("\\/", "\\/");
		addRegEx(regex, HttpMethod.DELETE, method);
		rm.deleteWithRegEx(regex, bindHandler(method));
		return this;
	}

	private void addPattern(String input, HttpMethod httpMethod, String method) {
		String serviceMethod = this.getClass().getName() + "|" + method;
		Matcher m = Pattern.compile(":([A-Za-z][A-Za-z0-9_]*)").matcher(input);
		StringBuffer sb = new StringBuffer();
		Set<String> groups = new HashSet<>();
		while (m.find()) {
			String group = m.group().substring(1);
			if (groups.contains(group)) {
				throw new IllegalArgumentException("Cannot use identifier " + group + " more than once in pattern string");
			}
			m.appendReplacement(sb, "(?<$1>[^\\/]+)");
			groups.add(group);
		}
		m.appendTail(sb);
		String regex = sb.toString();
		Set<Binding> bindings = uriBinding.get(serviceMethod);
		if (bindings == null) {
			bindings = new HashSet<>();
			uriBinding.put(serviceMethod, bindings);
		}
		bindings.add(new Binding(httpMethod, Pattern.compile(regex), serviceMethod, actionType(serviceMethod)));
	}

	private void addRegEx(String input, HttpMethod httpMethod, String method) {
		String serviceMethod = this.getClass().getName() + "|" + method;
		Set<Binding> bindings = uriBinding.get(serviceMethod);
		if (bindings == null) {
			bindings = new HashSet<>();
			uriBinding.put(serviceMethod, bindings);
		}
		bindings.add(new Binding(httpMethod, Pattern.compile(input), serviceMethod, actionType(serviceMethod)));
	}

	private ActionType actionType(String serviceMethod) {
		try {
			return ActionType.valueOf(securedActions.get(serviceMethod).getType());
		} catch (IllegalArgumentException | NullPointerException e) {
			return ActionType.UNSECURED;
		}
	}

	private String addPathPrefix(String pattern) {
		if (pattern == null || pattern.trim().isEmpty()) {
			return pathPrefix;
		}
		if (!pattern.trim().startsWith("/")) {
			return pathPrefix + "/" + pattern.trim();
		}
		return pathPrefix + pattern.trim();
	}

}
