package fr.wseduc.webutils.email;

import fr.wseduc.webutils.DefaultAsyncResult;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.eventbus.ResultMessage;
import fr.wseduc.webutils.exception.AsyncResultException;
import fr.wseduc.webutils.exception.InvalidConfigurationException;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mail.*;

import java.util.*;

public class SMTPSender extends NotificationHelper implements EmailSender {
    private static final Logger log = LoggerFactory.getLogger(SMTPSender.class);
    private final MailClient client;
    private final boolean splitRecipients;

    public SMTPSender(Vertx vertx, JsonObject config) throws InvalidConfigurationException {
        super(vertx, config);
        if (Objects.isNull(config) || !config.containsKey("hostname") || !config.containsKey("port")) {
            throw new InvalidConfigurationException("missing.parameters");
        }

        MailConfig smtpConfig = new MailConfig()
                .setHostname(config.getString("hostname"))
                .setPort(config.getInteger("port", 25));

        if (config.containsKey("username") && config.containsKey("password")) {
            smtpConfig.setUsername(config.getString("username"))
                    .setPassword(config.getString("password"));
        }

        if (config.containsKey("tls") && Boolean.TRUE.equals(config.getBoolean("tls"))) {
            smtpConfig.setStarttls(StartTLSOptions.REQUIRED);
        }

        client = MailClient.create(vertx, smtpConfig);
        splitRecipients = config.getBoolean("split-recipients", false);
    }

    @Override
    public void hardBounces(Date date, Handler<Either<String, List<Bounce>>> handler) {
        handler.handle(new Either.Right<>(new ArrayList<>()));
    }

    @Override
    public void hardBounces(Date startDate, Date endDate, Handler<Either<String, List<Bounce>>> handler) {
        handler.handle(new Either.Right<>(new ArrayList<>()));
    }

    @Override
    protected void sendEmail(JsonObject json, Handler<AsyncResult<Message<JsonObject>>> handler) {
        if (Objects.isNull(json) || Objects.isNull(json.getJsonArray("to"))
                || Objects.isNull(json.getString("subject")) || Objects.isNull(json.getString("body"))) {
            handler.handle(new DefaultAsyncResult<>(new AsyncResultException("invalid.parameters")));
            return;
        }

        if (splitRecipients && json.getJsonArray("to").size() > 1) {
            List<MailMessage> messages = new ArrayList<>();
            for (String recipient : (List<String>) json.getJsonArray("to").getList()) {
                messages.add(getMessage(json.copy().put("to", new JsonArray().add(recipient))));
            }

            send(messages, handler);
        } else {
            send(Collections.singletonList(getMessage(json)), handler);
        }

    }

    private MailMessage getMessage(JsonObject json) {
        MailMessage message = new MailMessage()
                .setFrom(json.getString("from"))
                .setTo((List<String>) json.getJsonArray("to").getList())
                .setSubject(json.getString("subject"))
                .setHtml(json.getString("body"));

        if (json.containsKey("cc") && !json.getJsonArray("cc").isEmpty()) {
            message.setCc((List<String>) json.getJsonArray("cc").getList());
        }

        if (json.containsKey("bcc") && !json.getJsonArray("bdd").isEmpty()) {
            message.setBcc((List<String>) json.getJsonArray("bcc").getList());
        }

        if (json.containsKey("attachments") && Objects.nonNull(json.getJsonArray("attachments"))
                && !json.getJsonArray("attachments").isEmpty()) {
            List<MailAttachment> attachments = new ArrayList<>();
            for (JsonObject att : (List<JsonObject>) json.getJsonArray("attachments").getList()) {
                MailAttachment attachment = new MailAttachment()
                        .setName(att.getString("name"))
                        .setData(Buffer.buffer(att.getString("content")));

                attachments.add(attachment);
            }

            message.setAttachment(attachments);
        }

        return message;
    }

    private void send(List<MailMessage> messages, Handler<AsyncResult<Message<JsonObject>>> handler) {
        List<Future> futures = new ArrayList<>();
        for (MailMessage message : messages) {
            Promise<Void> promise = Promise.promise();
            futures.add(promise.future());
            client.sendMail(message, ar -> {
                if (ar.failed()) {
                    log.error("Failed to send mail from SMTP", ar.cause());
                    promise.fail(ar.cause());
                } else {
                    promise.complete();
                }
            });
        }

        CompositeFuture.any(futures)
                .onFailure(throwable -> DefaultAsyncResult.handleAsyncError(throwable.getMessage(), handler))
                .onSuccess(unused -> DefaultAsyncResult.handleAsyncResult(new ResultMessage(), handler));
    }
}
