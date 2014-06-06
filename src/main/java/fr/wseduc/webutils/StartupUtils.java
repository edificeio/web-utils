package fr.wseduc.webutils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import fr.wseduc.webutils.security.SecuredAction;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

public class StartupUtils {

	private static final Logger log = LoggerFactory.getLogger(StartupUtils.class);

	public static void sendStartup(JsonObject app, JsonArray actions, Vertx vertx, Integer appRegistryPort) throws IOException {
		if (actions == null || actions.size() == 0) {
			actions = loadSecuredActions(vertx);
		}
		final String s = new JsonObject().putObject("application", app).putArray("actions", actions).encode();
		final HttpClient httpClient = vertx.createHttpClient().setHost("localhost")
				.setPort(appRegistryPort).setKeepAlive(false);
		httpClient.put("/appregistry/application", new Handler<HttpClientResponse>() {
			@Override
			public void handle(HttpClientResponse event) {
				if (event.statusCode() != 200) {
					log.error("Error recording application : " + s);
				}
				httpClient.close();
			}
		})
		.putHeader("Content-Type", "application/json")
		.end(s);
	}

	public static void sendStartup(JsonObject app, JsonArray actions, EventBus eb, String address,Vertx vertx,
			final Handler<Message<JsonObject>> handler) throws IOException {
		if (actions == null || actions.size() == 0) {
			actions = loadSecuredActions(vertx);
		}
		JsonObject jo = new JsonObject();
		jo.putObject("application", app)
		.putArray("actions", actions);
		eb.send(address, jo, handler);
	}

	public static void sendStartup(JsonObject app, JsonArray actions, EventBus eb, String address, Vertx vertx) throws IOException {
		sendStartup(app, actions, eb, address, vertx, null);
	}

	public static void sendStartup(JsonObject app, EventBus eb, String address, Vertx vertx,
			final Handler<Message<JsonObject>> handler) throws IOException {
		sendStartup(app, null, eb, address, vertx, handler);
	}

	public static void sendStartup(JsonObject app, EventBus eb, String address) throws IOException {
		sendStartup(app, null, eb, address, null);
	}

	public static JsonArray loadSecuredActions(Vertx vertx) throws IOException {
		String [] list = vertx.fileSystem().readDirSync(".", "^SecuredAction-.*json$");
		JsonArray securedActions = new JsonArray();
		for (String f : list) {
			BufferedReader in = null;
			try {
				in = new BufferedReader(new FileReader(f));
				String line;
				while((line = in.readLine()) != null) {
					securedActions.add(new JsonObject(line));
				}
			} finally {
				if (in != null) {
					in.close();
				}
			}
		}
		return securedActions;
	}

	public static Map<String, SecuredAction> securedActionsToMap(JsonArray securedActions) {
		if (securedActions == null || securedActions.size() == 0) {
			return Collections.emptyMap();
		}
		Map<String, SecuredAction> actions = new HashMap<>();
		for (Object a: securedActions) {
			JsonObject action = (JsonObject) a;
			String name = action.getString("name");
			String displayName = action.getString("displayName");
			String type = action.getString("type");
			if (name != null && type != null && displayName != null
					&& !name.trim().isEmpty() && !type.trim().isEmpty()) {
				actions.put(name, new SecuredAction(name, displayName, type));
			}
		}
		return actions;
	}

}
