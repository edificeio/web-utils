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
import fr.wseduc.webutils.DefaultAsyncResult;
import fr.wseduc.webutils.eventbus.ResultMessage;
import fr.wseduc.webutils.exception.AsyncResultException;
import fr.wseduc.webutils.exception.InvalidConfigurationException;
import fr.wseduc.webutils.Either;

import static fr.wseduc.webutils.DefaultAsyncResult.handleAsyncError;
import static fr.wseduc.webutils.DefaultAsyncResult.handleAsyncResult;
import static fr.wseduc.webutils.Utils.isNotEmpty;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.*;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SendInBlueSender extends NotificationHelper implements EmailSender {

	private static final Logger log = LoggerFactory.getLogger(SendInBlueSender.class);
	private final HttpClient httpClient;
	private final String apiKey;
	private final String dedicatedIp;
	private final boolean splitRecipients;
	private final ObjectMapper mapper;
	private final int maxSize;
	private static final String DATE_FORMAT = "yyyy-MM-dd";

	public SendInBlueSender(Vertx vertx, JsonObject config)
			throws InvalidConfigurationException, URISyntaxException {
		super(vertx, config);
		if (config != null && isNotEmpty(config.getString("uri")) && isNotEmpty(config.getString("api-key"))) {
			URI uri = new URI(config.getString("uri"));
			HttpClientOptions options = new HttpClientOptions()
					.setDefaultHost(uri.getHost())
					.setDefaultPort(uri.getPort())
					.setSsl("https".equals(uri.getScheme()))
					.setMaxPoolSize(16)
					.setKeepAlive(false);
			httpClient = vertx.createHttpClient(options);
//					.setPort()
//					.setMaxPoolSize(16)
//					.setSSL("https".equals(uri.getScheme()))
//					.setKeepAlive(false);
			apiKey = config.getString("api-key");
			dedicatedIp = config.getString("ip");
			splitRecipients = config.getBoolean("split-recipients", false);
			maxSize = config.getInteger("max-size", 0);
			mapper = new ObjectMapper();
			final String datePattern = config.getString("date-pattern", "yyyy-MM-dd'T'HH:mm:ss.SSS");
			mapper.setDateFormat(new SimpleDateFormat(datePattern));
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
			handler.handle(new Either.Left<>("invalid.date"));
			return;
		}
		final DateFormat df = new SimpleDateFormat(DATE_FORMAT);
		final JsonObject payload = new JsonObject().put("event", "hard_bounce");
		final String start = df.format(startDate);
		if (endDate != null) {
			payload.put("start_date", start).put("end_date", df.format(endDate));
		} else {
			payload.put("date", start);
		}
		httpClient.request(new RequestOptions()
				.setMethod(HttpMethod.POST)
				.setURI("/v2.0/report")
				.setHeaders(new HeadersMultiMap().add("api-key", apiKey)))
				.flatMap(request -> request.send(payload.encode()))
				.flatMap(HttpClientResponse::body)
				.map(buffer -> new JsonObject(buffer.toString()))
				.onSuccess(res -> {
					try {
						if ("success".equals(res.getString("code"))) {
							JsonArray l = res.getJsonArray("data");
							if (l == null || l.size() == 0) {
								handler.handle(new Either.Right<>(
										Collections.<Bounce>emptyList()));
								return;
							}
							List<Bounce> bounces = mapper.readValue(l.encode(),
									new TypeReference<List<Bounce>>(){});
							handler.handle(new Either.Right<>(bounces));
						} else {
							handler.handle(new Either.Left<>(
									res.getValue("message").toString()));
						}
					} catch (RuntimeException | IOException e) {
						handler.handle(new Either.Left<>(e.getMessage()));
						log.error(e.getMessage(), e);
					}
				})
				.onFailure(except -> log.error("Error when query hardbounce to sendinblue.", except));
	}

	@Override
	protected void sendEmail(JsonObject json, final Handler<AsyncResult<Message<JsonObject>>> handler) {
		if (json == null || json.getJsonArray("to") == null || json.getString("from") == null ||
				json.getString("subject") == null || json.getString("body") == null) {
			handler.handle(new DefaultAsyncResult<>(new AsyncResultException("invalid.parameters")));
			return;
		}
		if (splitRecipients && json.getJsonArray("to").size() > 1) {
			final AtomicInteger count = new AtomicInteger(json.getJsonArray("to").size());
			final AtomicBoolean success = new AtomicBoolean(true);
			final JsonArray errors = new JsonArray();
			final Handler<AsyncResult<Message<JsonObject>>> h = ar -> {
				if (ar.failed()) {
					success.set(false);
					errors.add(ar.cause().getMessage());
				}
				if (count.decrementAndGet() == 0) {
					if (success.get()) {
						handleAsyncResult(new ResultMessage(), handler);
					} else {
						handleAsyncError(errors.encode(), handler);
					}
				}
			};
			for (Object to: json.getJsonArray("to")) {
				send(json.copy().put("to", new JsonArray().add(to.toString())), h);
			}
		} else {
			send(json, handler);
		}
	}

	private void send(JsonObject json, final Handler<AsyncResult<Message<JsonObject>>> handler) {

		JsonArray to = new JsonArray();

		for(Object o : json.getJsonArray("to")) {
			String email = o.toString();
			to.add(new JsonObject().put("email", email));
		}

		final JsonObject sender = new JsonObject().put("email", json.getString("from"));

		JsonObject payload = new JsonObject()
				.put("to", to)
				.put("sender", sender)
				.put("from", new JsonArray().add(json.getString("from")))
				.put("subject", json.getString("subject"))
				.put("htmlContent", json.getString("body"));

		JsonObject headers = new JsonObject();
		if (isNotEmpty(dedicatedIp)) {
			headers.put("X-Mailin-IP", dedicatedIp);
		}

		if (json.getJsonArray("headers") != null) {
			for (Object o: json.getJsonArray("headers")) {
				if (!(o instanceof JsonObject)) continue;
				JsonObject h = (JsonObject) o;
				headers.put(h.getString("name"), h.getString("value"));
			}
		}

		if (headers.size() > 0) {
			payload.put("headers", headers);
		}

		if (json.getJsonArray("cc") != null && json.getJsonArray("cc").size() > 0) {
			JsonObject cc = new JsonObject();
			for (Object o: json.getJsonArray("cc")) {
				cc.put(o.toString(), "");
			}
			payload.put("cc", cc);
		}

		if (json.getJsonArray("bcc") != null && json.getJsonArray("bcc").size() > 0) {
			JsonObject bcc = new JsonObject();
			for (Object o: json.getJsonArray("bcc")) {
				bcc.put(o.toString(), "");
			}
			payload.put("bcc", bcc);
		}

		int mailSize = json.getString("body").getBytes().length;

		if (json.getJsonArray("attachments") != null && !json.getJsonArray("attachments").isEmpty()) {
			JsonArray attachments = new JsonArray();
			for (Object o : json.getJsonArray("attachments")) {
				JsonObject att = (JsonObject) o;

				// Calculate the attachment size only once and store it in a variable
				int attachmentSize = att.getString("content").getBytes().length;

				// Check if adding this attachment would exceed the max size limit
				if (maxSize > 0 && (mailSize + attachmentSize) > maxSize) {
					log.warn("Mail too big, can't attach " + att.getString("name"));
				} else {
					mailSize += attachmentSize;

					String attachmentName = att.getString("name");
					String contentType = att.getString("contentType");

					// Check if the attachment name contains the file type
					if (!attachmentName.contains(".")) {
						// Get the file extension from the content type
						String fileExtension = "";
						if (contentType != null && contentType.contains("/")) {
							String[] parts = contentType.split("/");
							if (parts.length > 1) {
								fileExtension = parts[1];
							}
						}
						if (fileExtension != null) {
							attachmentName += "." + fileExtension;
						}
					}
					
					attachments.add(new JsonObject()
							.put("name", attachmentName)
							.put("content", att.getString("content")));
				}
			}
			payload.put("attachment", attachments);
		}

		httpClient.request(new RequestOptions()
						.setMethod(HttpMethod.POST)
						.setURI("/v3/smtp/email")
						.setHeaders(new HeadersMultiMap().add("api-key", apiKey)))
				.flatMap(request -> request.send(payload.encode()))
				.onSuccess(resp -> {
					if (resp.statusCode() == 201) {
						handleAsyncResult(new ResultMessage(), handler);
					} else {
						resp.bodyHandler(buffer -> {
							try {
								final JsonObject err = new JsonObject(buffer.toString());
								final String code = err.getString("code");
								log.error("Could not send mail with SendInBlue:" + code + " -> " + err.getString("message"));
								handleAsyncError(err.getString("message"), handler);
							} catch (DecodeException e) {
								log.error("Could not decode response: " + buffer.toString());
								handleAsyncError(buffer.toString(), handler);
							}
						});
					}
				})
				.onFailure(except -> log.error("Error sending to sendinblue.", except));
	}

	public ObjectMapper getMapper() {
		return mapper;
	}
}
