package fr.wseduc.webutils.eventbus;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.eventbus.ReplyException;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class EventBusWithLogger implements EventBus {

	private final EventBus eb;
	private static final Logger logger = LoggerFactory.getLogger(EventBusWithLogger.class);
	private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm.ss.SSS");

	public EventBusWithLogger(EventBus eb) {
		this.eb = eb;
	}

	private static String formatDate(Date date) {
		return df.format(date);
	}

	private void sendLog(String address, String message) {
		logger.info(formatDate(new Date()) + " send : " + address + " - " + message);
	}

	private void publishLog(String address, String message) {
		logger.info(formatDate(new Date()) + " publish : " + address + " - " + message);
	}

	private String sendLogwithResponse(String address, String message) {
		String logMessageId = UUID.randomUUID().toString();
		logger.info(formatDate(new Date()) + " send : " + logMessageId + " - " + address + " - " + message);
		return logMessageId;
	}

	private <T> void responseLog(String logMessageId, T response) {
		String r;
		if (response instanceof JsonObject) {
			r = ((JsonObject) response).encode();
		} else if (response instanceof JsonArray) {
			r = ((JsonArray) response).encode();
		} else if (response instanceof Buffer) {
			r = "Buffer not displayed";
		} else {
			r = response.toString();
		}
		logger.info(formatDate(new Date()) + " response : " + logMessageId + " - " + r);
	}

	@Override
	public void close(Handler<AsyncResult<Void>> doneHandler) {
		eb.close(doneHandler);
	}

	@Override
	public EventBus send(String address, Object message) {
		sendLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public EventBus send(String address, Object message,
			final Handler<Message> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message.toString());
		if (replyHandler != null) {
			return eb.send(address, message, new Handler<Message>() {

				@Override
				public void handle(Message event) {
					responseLog(logMessageId, event.body());
					replyHandler.handle(event);
				}
			});
		} else {
			return eb.send(address, message, null);
		}
	}

	@Override
	public <T> EventBus sendWithTimeout(String address, Object message, long timeout,
			final Handler<AsyncResult<Message<T>>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message.toString());
		return eb.sendWithTimeout(address, message, timeout,
					timoutReplyHandler(replyHandler, logMessageId));
	}

	@Override
	public <T> EventBus send(String address, JsonObject message,
			final Handler<Message<T>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message.encode());
		return eb.send(address, message, replyHandler(replyHandler, logMessageId));
	}

	@Override
	public <T> EventBus sendWithTimeout(String address, JsonObject message, long timeout,
			Handler<AsyncResult<Message<T>>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message.encode());
		return eb.sendWithTimeout(address, message, timeout,
				timoutReplyHandler(replyHandler, logMessageId));
	}

	private <T> Handler<Message<T>> replyHandler(
			final Handler<Message<T>> replyHandler, final String logMessageId) {
		if (replyHandler == null) {
			return null;
		}
		return new Handler<Message<T>>() {

			@Override
			public void handle(Message<T> event) {
				responseLog(logMessageId, event.body());
				replyHandler.handle(event);
			}
		};
	}

	private <T> Handler<AsyncResult<Message<T>>> timoutReplyHandler(
			final Handler<AsyncResult<Message<T>>> replyHandler, final String logMessageId) {
		if (replyHandler == null) {
			return null;
		}
		return new Handler<AsyncResult<Message<T>>>() {
			@Override
			public void handle(AsyncResult<Message<T>> event) {
				if (event.succeeded()) {
					responseLog(logMessageId, event.result().body());
				} else {
					ReplyException ex = (ReplyException)event.cause();
					logger.error("MessageId : " + logMessageId);
					logger.error("Failure type: " + ex.failureType());
					logger.error("Failure code: " + ex.failureCode());
					logger.error("Failure message: " + ex.getMessage());
				}
				replyHandler.handle(event);
			}
		};
	}

	@Override
	public EventBus send(String address, JsonObject message) {
		sendLog(address, message.encode());
		return eb.send(address, message);
	}

	@Override
	public <T> EventBus send(String address, JsonArray message,
			final Handler<Message<T>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message.encode());
		return eb.send(address, message, replyHandler(replyHandler, logMessageId));
	}

	@Override
	public <T> EventBus sendWithTimeout(String address, JsonArray message, long timeout,
			Handler<AsyncResult<Message<T>>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message.encode());
		return eb.sendWithTimeout(address, message, timeout,
				timoutReplyHandler(replyHandler, logMessageId));
	}

	@Override
	public EventBus send(String address, JsonArray message) {
		sendLog(address, message.encode());
		return eb.send(address, message);
	}

	@Override
	public <T> EventBus send(String address, Buffer message,
			final Handler<Message<T>> replyHandler) {
//		final String logMessageId = sendLogwithResponse(address, message.toString());
//		return eb.send(address, message, replyHandler(replyHandler, logMessageId));
		return eb.send(address, message, replyHandler);
	}

	@Override
	public <T> EventBus sendWithTimeout(String address, Buffer message, long timeout,
			Handler<AsyncResult<Message<T>>> replyHandler) {
		return eb.sendWithTimeout(address, message, timeout, replyHandler);
	}

	@Override
	public EventBus send(String address, Buffer message) {
		//sendLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public <T> EventBus send(String address, byte[] message,
			final Handler<Message<T>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, new String(message));
		return eb.send(address, message, replyHandler(replyHandler, logMessageId));
	}

	@Override
	public <T> EventBus sendWithTimeout(String address, byte[] message, long timeout,
			Handler<AsyncResult<Message<T>>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, new String(message));
		return eb.sendWithTimeout(address, message, timeout,
				timoutReplyHandler(replyHandler, logMessageId));
	}

	@Override
	public EventBus send(String address, byte[] message) {
		sendLog(address, new String(message));
		return eb.send(address, message);
	}

	@Override
	public <T> EventBus send(String address, String message,
			final Handler<Message<T>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message);
		return eb.send(address, message, replyHandler(replyHandler, logMessageId));
	}

	@Override
	public <T> EventBus sendWithTimeout(String address, String message, long timeout,
			Handler<AsyncResult<Message<T>>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message);
		return eb.sendWithTimeout(address, message, timeout,
				timoutReplyHandler(replyHandler, logMessageId));
	}

	@Override
	public EventBus send(String address, String message) {
		sendLog(address, message);
		return eb.send(address, message);
	}

	@Override
	public <T> EventBus send(String address, Integer message,
			final Handler<Message<T>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message.toString());
		return eb.send(address, message, replyHandler(replyHandler, logMessageId));
	}

	@Override
	public <T> EventBus sendWithTimeout(String address, Integer message, long timeout,
			Handler<AsyncResult<Message<T>>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message.toString());
		return eb.sendWithTimeout(address, message, timeout,
				timoutReplyHandler(replyHandler, logMessageId));
	}

	@Override
	public EventBus send(String address, Integer message) {
		sendLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public <T> EventBus send(String address, Long message,
			final Handler<Message<T>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message.toString());
		return eb.send(address, message, replyHandler(replyHandler, logMessageId));
	}

	@Override
	public <T> EventBus sendWithTimeout(String address, Long message, long timeout,
			Handler<AsyncResult<Message<T>>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message.toString());
		return eb.sendWithTimeout(address, message, timeout,
				timoutReplyHandler(replyHandler, logMessageId));
	}

	@Override
	public EventBus send(String address, Long message) {
		sendLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public <T> EventBus send(String address, Float message,
			final Handler<Message<T>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message.toString());
		return eb.send(address, message, replyHandler(replyHandler, logMessageId));
	}

	@Override
	public <T> EventBus sendWithTimeout(String address, Float message, long timeout,
			Handler<AsyncResult<Message<T>>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message.toString());
		return eb.sendWithTimeout(address, message, timeout,
				timoutReplyHandler(replyHandler, logMessageId));
	}

	@Override
	public EventBus send(String address, Float message) {
		sendLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public <T> EventBus send(String address, Double message,
			final Handler<Message<T>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message.toString());
		return eb.send(address, message, replyHandler(replyHandler, logMessageId));
	}

	@Override
	public <T> EventBus sendWithTimeout(String address, Double message, long timeout,
			Handler<AsyncResult<Message<T>>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message.toString());
		return eb.sendWithTimeout(address, message, timeout,
				timoutReplyHandler(replyHandler, logMessageId));
	}

	@Override
	public EventBus send(String address, Double message) {
		sendLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public <T> EventBus send(String address, Boolean message,
			final Handler<Message<T>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message.toString());
		return eb.send(address, message, replyHandler(replyHandler, logMessageId));
	}

	@Override
	public <T> EventBus sendWithTimeout(String address, Boolean message, long timeout,
			Handler<AsyncResult<Message<T>>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message.toString());
		return eb.sendWithTimeout(address, message, timeout,
				timoutReplyHandler(replyHandler, logMessageId));
	}

	@Override
	public EventBus send(String address, Boolean message) {
		sendLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public <T> EventBus send(String address, Short message,
			final Handler<Message<T>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message.toString());
		return eb.send(address, message, replyHandler(replyHandler, logMessageId));
	}

	@Override
	public <T> EventBus sendWithTimeout(String address, Short message, long timeout,
			Handler<AsyncResult<Message<T>>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message.toString());
		return eb.sendWithTimeout(address, message, timeout,
				timoutReplyHandler(replyHandler, logMessageId));
	}

	@Override
	public EventBus send(String address, Short message) {
		sendLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public <T> EventBus send(String address, Character message,
			final Handler<Message<T>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message.toString());
		return eb.send(address, message, replyHandler(replyHandler, logMessageId));
	}

	@Override
	public <T> EventBus sendWithTimeout(String address, Character message, long timeout,
			Handler<AsyncResult<Message<T>>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message.toString());
		return eb.sendWithTimeout(address, message, timeout,
				timoutReplyHandler(replyHandler, logMessageId));
	}

	@Override
	public EventBus send(String address, Character message) {
		sendLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public <T> EventBus send(String address, Byte message,
			final Handler<Message<T>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message.toString());
		return eb.send(address, message, replyHandler(replyHandler, logMessageId));
	}

	@Override
	public <T> EventBus sendWithTimeout(String address, Byte message, long timeout,
			Handler<AsyncResult<Message<T>>> replyHandler) {
		final String logMessageId = sendLogwithResponse(address, message.toString());
		return eb.sendWithTimeout(address, message, timeout,
				timoutReplyHandler(replyHandler, logMessageId));
	}

	@Override
	public EventBus send(String address, Byte message) {
		sendLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public EventBus publish(String address, Object message) {
		publishLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public EventBus publish(String address, JsonObject message) {
		publishLog(address, message.encode());
		return eb.send(address, message);
	}

	@Override
	public EventBus publish(String address, JsonArray message) {
		publishLog(address, message.encode());
		return eb.send(address, message);
	}

	@Override
	public EventBus publish(String address, Buffer message) {
		//publishLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public EventBus publish(String address, byte[] message) {
		publishLog(address, new String(message));
		return eb.send(address, message);
	}

	@Override
	public EventBus publish(String address, String message) {
		publishLog(address, message);
		return eb.send(address, message);
	}

	@Override
	public EventBus publish(String address, Integer message) {
		publishLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public EventBus publish(String address, Long message) {
		publishLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public EventBus publish(String address, Float message) {
		publishLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public EventBus publish(String address, Double message) {
		publishLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public EventBus publish(String address, Boolean message) {
		publishLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public EventBus publish(String address, Short message) {
		publishLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public EventBus publish(String address, Character message) {
		publishLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public EventBus publish(String address, Byte message) {
		publishLog(address, message.toString());
		return eb.send(address, message);
	}

	@Override
	public EventBus unregisterHandler(String address,
			Handler<? extends Message> handler,
			Handler<AsyncResult<Void>> resultHandler) {
		return eb.unregisterHandler(address, handler, resultHandler);
	}

	@Override
	public EventBus unregisterHandler(String address,
			Handler<? extends Message> handler) {
		return eb.unregisterHandler(address, handler);
	}

	@Override
	public EventBus registerHandler(String address,
			Handler<? extends Message> handler,
			Handler<AsyncResult<Void>> resultHandler) {
		return eb.registerHandler(address, handler, resultHandler);
	}

	@Override
	public EventBus registerHandler(String address,
			Handler<? extends Message> handler) {
		return eb.registerHandler(address, handler);
	}

	@Override
	public EventBus registerLocalHandler(String address,
			Handler<? extends Message> handler) {
		return eb.registerLocalHandler(address, handler);
	}

	@Override
	public EventBus setDefaultReplyTimeout(long timeoutMs) {
		return eb.setDefaultReplyTimeout(timeoutMs);
	}

	@Override
	public long getDefaultReplyTimeout() {
		return eb.getDefaultReplyTimeout();
	}

}
