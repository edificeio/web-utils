package fr.wseduc.webutils;

import java.io.UnsupportedEncodingException;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

import fr.wseduc.webutils.http.Renders;

public class NotificationHelper {

	private static final String EMAIL_ADDRESS = "wse.email";
	private final EventBus eb;
	private final Renders render;
	private final Logger log;
	private final String senderEmail;
	private final String host;

	public NotificationHelper(Vertx vertx, EventBus eb, Container container) {
		this.eb = eb;
		this.log = container.logger();
		this.render = new Renders(vertx, container);
		this.senderEmail = container.config().getString("email", "noreply@one1d.fr");
		this.host = container.config().getString("host", "http://localhost:8009");
	}

	public void sendEmail(HttpServerRequest request, String to, String cc, String bcc,
			String subject, String templateBody, JsonObject templateParams,
			boolean translateSubject, final Handler<Message<JsonObject>> handler) {
		sendEmail(request, to, senderEmail, cc, bcc, subject, templateBody,
				templateParams, translateSubject, handler);
	}

	public void sendEmail(HttpServerRequest request, String to, String from, String cc, String bcc,
			String subject, String templateBody, JsonObject templateParams,
			boolean translateSubject, final Handler<Message<JsonObject>> handler) {
		final JsonObject json = new JsonObject()
		.putString("to", to)
		.putString("from", from)
		.putString("cc", cc)
		.putString("bcc", bcc);
		if (translateSubject) {
			json.putString("subject", I18n.getInstance().translate(
					subject, request.headers().get("Accept-Language")));
		} else {
			json.putString("subject", subject);
		}
		render.processTemplate(request, templateBody, templateParams, new Handler<String>() {
			@Override
			public void handle(String body) {
				if (body != null) {
					try {
						json.putString("body", new String(body.getBytes("UTF-8"), "ISO-8859-1"));
						eb.send(EMAIL_ADDRESS, json, handler);
					} catch (UnsupportedEncodingException e) {
						log.error(e.getMessage(), e);
						Message<JsonObject> m = new ErrorMessage();
						m.body().putString("error", e.getMessage());
						handler.handle(m);
					}
				} else {
					log.error("Message is null.");
					Message<JsonObject> m = new ErrorMessage();
					m.body().putString("error", "Message is null.");
					handler.handle(m);
				}
			}
		});
	}

	public String getSenderEmail() {
		return senderEmail;
	}

	public String getHost() {
		return host;
	}

	class ErrorMessage implements Message<JsonObject> {

		private final JsonObject body = new JsonObject();

		@Override
		public String address() {
			return null;
		}

		@Override
		public JsonObject body() {
			return body;
		}

		@Override
		public String replyAddress() {
			return null;
		}

		@Override
		public void reply() {

		}

		@Override
		public void reply(Object message) {

		}

		@Override
		public void reply(JsonObject message) {

		}

		@Override
		public void reply(JsonArray message) {

		}

		@Override
		public void reply(String message) {

		}

		@Override
		public void reply(Buffer message) {

		}

		@Override
		public void reply(byte[] message) {

		}

		@Override
		public void reply(Integer message) {

		}

		@Override
		public void reply(Long message) {

		}

		@Override
		public void reply(Short message) {

		}

		@Override
		public void reply(Character message) {

		}

		@Override
		public void reply(Boolean message) {

		}

		@Override
		public void reply(Float message) {

		}

		@Override
		public void reply(Double message) {

		}

		@Override
		public <T1> void reply(Handler<Message<T1>> replyHandler) {

		}

		@Override
		public <T> void replyWithTimeout(long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

		}

		@Override
		public <T1> void reply(Object message, Handler<Message<T1>> replyHandler) {

		}

		@Override
		public <T> void replyWithTimeout(Object message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

		}

		@Override
		public <T1> void reply(JsonObject message, Handler<Message<T1>> replyHandler) {

		}

		@Override
		public <T> void replyWithTimeout(JsonObject message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

		}

		@Override
		public <T1> void reply(JsonArray message, Handler<Message<T1>> replyHandler) {

		}

		@Override
		public <T> void replyWithTimeout(JsonArray message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

		}

		@Override
		public <T1> void reply(String message, Handler<Message<T1>> replyHandler) {

		}

		@Override
		public <T> void replyWithTimeout(String message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

		}

		@Override
		public <T1> void reply(Buffer message, Handler<Message<T1>> replyHandler) {

		}

		@Override
		public <T> void replyWithTimeout(Buffer message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

		}

		@Override
		public <T1> void reply(byte[] message, Handler<Message<T1>> replyHandler) {

		}

		@Override
		public <T> void replyWithTimeout(byte[] message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

		}

		@Override
		public <T1> void reply(Integer message, Handler<Message<T1>> replyHandler) {

		}

		@Override
		public <T> void replyWithTimeout(Integer message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

		}

		@Override
		public <T1> void reply(Long message, Handler<Message<T1>> replyHandler) {

		}

		@Override
		public <T> void replyWithTimeout(Long message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

		}

		@Override
		public <T1> void reply(Short message, Handler<Message<T1>> replyHandler) {

		}

		@Override
		public <T> void replyWithTimeout(Short message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

		}

		@Override
		public <T1> void reply(Character message, Handler<Message<T1>> replyHandler) {

		}

		@Override
		public <T> void replyWithTimeout(Character message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

		}

		@Override
		public <T1> void reply(Boolean message, Handler<Message<T1>> replyHandler) {

		}

		@Override
		public <T> void replyWithTimeout(Boolean message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

		}

		@Override
		public <T1> void reply(Float message, Handler<Message<T1>> replyHandler) {

		}

		@Override
		public <T> void replyWithTimeout(Float message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

		}

		@Override
		public <T1> void reply(Double message, Handler<Message<T1>> replyHandler) {

		}

		@Override
		public <T> void replyWithTimeout(Double message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

		}

		@Override
		public void fail(int failureCode, String message) {

		}
	}
}
