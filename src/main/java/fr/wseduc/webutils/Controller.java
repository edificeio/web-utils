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

import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.http.HttpMethod;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.http.TraceIdContextHandler;
import fr.wseduc.webutils.request.AccessLogger;
import fr.wseduc.webutils.request.filter.SecurityHandler;
import fr.wseduc.webutils.request.filter.XSSHandler;
import fr.wseduc.webutils.security.ActionType;
import fr.wseduc.webutils.security.SecuredAction;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.context.storage.ContextLocal;
import org.apache.commons.lang3.time.StopWatch;
import org.vertx.java.core.http.RouteMatcher;

import java.io.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Controller extends Renders {

	private static final MethodHandles.Lookup lookup = MethodHandles.publicLookup();
	protected RouteMatcher rm;
	private final Map<String, Set<Binding>> uriBinding;
	protected Map<String, SecuredAction> securedActions;
	/** Fully qualified method names, annotated by @MfaProtected, which require MFA before use. */
	protected Set<String> mfaProtectedMethods = new HashSet<String>();
	protected EventBus eb;
	protected String busPrefix = "";
	private AccessLogger accessLogger;
	public static final String TRACE_ID = "X-Cloud-Trace-Context";

	public Controller(Vertx vertx, JsonObject config, RouteMatcher rm,
			Map<String, SecuredAction> securedActions) {
		super(vertx, config);
		this.rm = rm;
		this.uriBinding = new HashMap<>();
		this.securedActions = securedActions;
		if (vertx != null) {
			this.eb = Server.getEventBus(vertx);
		}
		if (rm != null) {
			loadRoutes();
		}
		this.accessLogger = new AccessLogger();
	}

	protected void init(Vertx vertx, JsonObject config, RouteMatcher rm,
	Map<String, SecuredAction> securedActions)
	{
		super.init(vertx, config);
		this.rm = rm;
		this.securedActions = securedActions;
		this.eb = Server.getEventBus(vertx);
		if (rm != null) {
			loadRoutes();
		} else {
			log.error("RouteMatcher is null.");
		}
	}

	protected void loadRoutes() {
		InputStream is = this.getClass().getClassLoader().getResourceAsStream("routes" + File.separator +
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
							registerMethod(path, method, route.getBoolean("local", true));
							break;
					}
					if( route.getBoolean("mfaProtected", false) ) {
						String serviceMethod = this.getClass().getName() + "|" + method;
						mfaProtectedMethods.add(serviceMethod);
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
			final String qualifiedName = this.getClass().getName() + "|" + method;
			return new XSSHandler() {

				@Override
				public void filter(final HttpServerRequest request) {
					accessLogger.log(request, v -> {
						try {
							doExecuteInvoke(mh, request, qualifiedName, false);
						} catch (Throwable e) {
							if (!(e instanceof IllegalStateException) ||
									!"Response is closed".equals(e.getMessage())) {
								log.error("Error invoking secured method : " + method, e);
								request.response().setStatusCode(500).end();
							}
						}
					});
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
			final String qualifiedName = this.getClass().getName() + "|" + method;
			return new SecurityHandler() {

				@Override
				public void filter(HttpServerRequest request) {
					try {
						doExecuteInvoke(mh, request, qualifiedName, true);
					} catch (Throwable e) {
						if (!(e instanceof IllegalStateException) ||
								!"Response is closed".equals(e.getMessage())) {
							log.error("Error invoking secured method : " + method, e);
							request.response().setStatusCode(500).end();
						}
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

	private void doExecuteInvoke(MethodHandle mh, HttpServerRequest request,
								 String qualifiedName, boolean secured) throws Throwable {
		final Context ctx = Vertx.currentContext();
		String traceId = TraceIdContextHandler.getTraceId(ctx, request);
		String shortQualifiedName = qualifiedName.replaceAll("\\B\\w+(\\.[a-z])","$1");
		if(secured) {
			log.info(" Begin secured method : " + shortQualifiedName);
		} else {
			log.debug(" Begin method : " + shortQualifiedName);
		}
		request.response().putHeader(TRACE_ID, traceId);

		// if call later and the http response implementation doesn't manage multiple end handler it will be
		//erased, not a big deal
		request.response().endHandler(h -> {
			StopWatch watch = TraceIdContextHandler.getTraceTime(ctx);
			if(secured && watch != null) {
				watch.stop();
				log.info(" End of secured method : " + shortQualifiedName + " in [" + watch.getTime(TimeUnit.MILLISECONDS) + " ms]");
			} else if(!secured && watch != null) {
				watch.stop();
				log.debug(" End of method : " + shortQualifiedName + " in [" + watch.getTime(TimeUnit.MILLISECONDS) + " ms]");
			}
		});
		//invoke the target method on the controller
		mh.invokeExact(request);
	}

	public void registerMethod(String address, String method, boolean local)
			throws NoSuchMethodException, IllegalAccessException {
		final MethodHandle mh = lookup.bind(this, method,
				MethodType.methodType(void.class, Message.class));
		Handler<Message<JsonObject>> handler = message -> {
            try {
                mh.invokeExact(message);
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
                JsonObject json = new JsonObject().put("status", "error")
                        .put("message", e.getMessage());
                message.reply(json);
            }
        };
		if (local) {
			Server.getEventBus(vertx).localConsumer(busPrefix + address, handler);
		} else {
			Server.getEventBus(vertx).consumer(busPrefix + address, handler);
		}
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

	public Set<Binding> getMfaProtectedBindings() {
		Set<Binding> ret = new HashSet<Binding>( mfaProtectedMethods.size() );
		for( String method : mfaProtectedMethods ) {
			Set<Binding> bindings = uriBinding.get(method);
			if( bindings!=null ) {
				ret.addAll(bindings);
			}
		}
		return ret;
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
		bindings.add(new Binding(httpMethod, Pattern.compile(regex), serviceMethod, actionType(serviceMethod), findOverride(serviceMethod)));
	}

	private void addRegEx(String input, HttpMethod httpMethod, String method) {
		String serviceMethod = this.getClass().getName() + "|" + method;
		Set<Binding> bindings = uriBinding.get(serviceMethod);
		if (bindings == null) {
			bindings = new HashSet<>();
			uriBinding.put(serviceMethod, bindings);
		}
		bindings.add(new Binding(httpMethod, Pattern.compile(input), serviceMethod, actionType(serviceMethod), findOverride(serviceMethod)));
	}

	private String findOverride(String serviceMethod) {
		if(!securedActions.containsKey(serviceMethod)) {
			return null;
		}
		return securedActions.get(serviceMethod).getRight();
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

	public void setAccessLogger(AccessLogger accessLogger) {
		this.accessLogger = accessLogger;
	}

}
