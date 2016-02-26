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

package fr.wseduc.webutils.email;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.wseduc.webutils.eventbus.ResultMessage;
import fr.wseduc.webutils.exception.InvalidConfigurationException;
import fr.wseduc.webutils.Either;
import static fr.wseduc.webutils.Utils.isNotEmpty;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;
import org.vertx.java.platform.Container;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class SendInBlueSender extends NotificationHelper implements EmailSender {

	private static final Logger log = LoggerFactory.getLogger(SendInBlueSender.class);
	private final HttpClient httpClient;
	private final String apiKey;
	private final String dedicatedIp;
	private final ObjectMapper mapper;
	private static final String DATE_FORMAT = "yyyy-MM-dd";

	public SendInBlueSender(Vertx vertx, Container container, JsonObject config)
			throws InvalidConfigurationException, URISyntaxException {
		super(vertx, container);
		if (config != null && isNotEmpty(config.getString("uri")) && isNotEmpty(config.getString("api-key"))) {
			URI uri = new URI(config.getString("uri"));
			httpClient = vertx.createHttpClient()
					.setHost(uri.getHost())
					.setPort(uri.getPort())
					.setMaxPoolSize(16)
					.setSSL("https".equals(uri.getScheme()))
					.setKeepAlive(false);
			apiKey = config.getString("api-key");
			dedicatedIp = config.getString("ip");
			mapper = new ObjectMapper();
			mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
		} else {
			throw new InvalidConfigurationException("missing.parameters");
		}
	}

	@Override
	public void hardBounces(Date date, Handler<Either<String, List<Bounce>>> handler) {
		hardBounces(date, null, handler);
	}

	@Override
	public void hardBounces(Date startDate, Date endDate, final Handler<Either<String, List<Bounce>>> handler) {
		if (startDate == null) {
			handler.handle(new Either.Left<String, List<Bounce>>("invalid.date"));
			return;
		}
		final DateFormat df = new SimpleDateFormat(DATE_FORMAT);
		final JsonObject payload = new JsonObject().putString("event", "hard_bounce");
		final String start = df.format(startDate);
		if (endDate != null) {
			payload.putString("start_date", start).putString("end_date", df.format(endDate));
		} else {
			payload.putString("date", start);
		}
		HttpClientRequest req = httpClient.post("/v2.0/report", new Handler<HttpClientResponse>() {
			@Override
			public void handle(final HttpClientResponse resp) {
				resp.bodyHandler(new Handler<Buffer>() {
					@Override
					public void handle(Buffer buffer) {
						try {
							JsonObject res = new JsonObject(buffer.toString());
							if ("success".equals(res.getString("code"))) {
								JsonArray l = res.getArray("data");
								if (l == null || l.size() == 0) {
									handler.handle(new Either.Right<String, List<Bounce>>(
											Collections.<Bounce>emptyList()));
									return;
								}
								List<Bounce> bounces = mapper.readValue(l.encode(),
										new TypeReference<List<Bounce>>(){});
								handler.handle(new Either.Right<String, List<Bounce>>(bounces));
							} else {
								handler.handle(new Either.Left<String, List<Bounce>>(
										res.getValue("message").toString()));
							}
						} catch (RuntimeException | IOException e) {
							handler.handle(new Either.Left<String, List<Bounce>>(e.getMessage()));
							log.error(e.getMessage(), e);
						}
					}
				});
			}
		});
		req.putHeader("api-key", apiKey);
		req.end(payload.encode());
	}

	@Override
	protected void sendEmail(JsonObject json, final Handler<Message<JsonObject>> handler) {
		if (json == null || json.getArray("to") == null || json.getString("from") == null ||
				json.getString("subject") == null || json.getString("body") == null) {
			handler.handle(new ResultMessage().error("invalid.parameters"));
			return;
		}
		HttpClientRequest req = httpClient.post("/v2.0/email", new Handler<HttpClientResponse>() {
			@Override
			public void handle(HttpClientResponse resp) {
				if (resp.statusCode() == 200) {
					handler.handle(new ResultMessage());
				} else {
					resp.bodyHandler(new Handler<Buffer>() {
						@Override
						public void handle(Buffer buffer) {
							JsonObject err = new JsonObject(buffer.toString());
							handler.handle(new ResultMessage().error(err.getString("message")));
						}
					});
				}
			}
		});
		req.putHeader("api-key", apiKey);
		JsonObject to = new JsonObject();
		for (Object o: json.getArray("to")) {
			to.putString(o.toString(), "");
		}
		JsonObject payload = new JsonObject()
				.putObject("to", to)
				.putArray("from", new JsonArray().add(json.getString("from")))
				.putString("subject", json.getString("subject"))
				.putString("html", json.getString("body"));
		JsonObject headers = new JsonObject();
		if (isNotEmpty(dedicatedIp)) {
			headers.putString("X-Mailin-IP", dedicatedIp);
		}
		if (json.getArray("headers") != null) {
			for (Object o: json.getArray("headers")) {
				if (!(o instanceof JsonObject)) continue;
				JsonObject h = (JsonObject) o;
				headers.putString(h.getString("name"), h.getString("value"));
			}
		}
		if (headers.size() > 0) {
			payload.putObject("headers", headers);
		}
		if (json.getArray("cc") != null && json.getArray("cc").size() > 0) {
			JsonObject cc = new JsonObject();
			for (Object o: json.getArray("cc")) {
				cc.putString(o.toString(), "");
			}
			payload.putObject("cc", cc);
		}
		if (json.getArray("bcc") != null && json.getArray("bcc").size() > 0) {
			JsonObject bcc = new JsonObject();
			for (Object o: json.getArray("bcc")) {
				bcc.putString(o.toString(), "");
			}
			payload.putObject("bcc", bcc);
		}
		req.end(payload.encode());
	}

}
